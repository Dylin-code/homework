package com.example.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.enums.TransferStatus;
import com.example.demo.model.entity.Transfer;
import com.example.demo.model.entity.User;
import com.example.demo.mq.TransferEventPublisher;
import com.example.demo.mq.event.TransferCanceledEvent;
import com.example.demo.mq.event.TransferCompletedEvent;
import com.example.demo.mq.event.TransferCreatedEvent;
import com.example.demo.repository.TransferRepo;
import com.example.demo.repository.UserRepo;
import com.example.demo.util.TxCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final UserRepo userRepo;
    private final TransferRepo transferRepo;
    private final TransferEventPublisher publisher;
    private final TxCacheEvictor cacheEvictor;

    @Value("${app.transfer.cancel-window-minutes:10}")
    private long cancelWindowMinutes;

    /**
     * 轉帳 同步版本
     * @param fromUserId
     * @param toUserId
     * @param amount
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Transfer createTransfer(String fromUserId, String toUserId, BigDecimal amount) {
        if (Objects.equals(fromUserId, toUserId)) {
            throw new IllegalArgumentException("fromUserId and toUserId cannot be the same");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }

        // 以 userId 字典序決定加鎖順序，降低死鎖機率
        String first = fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId;
        String second = fromUserId.compareTo(toUserId) < 0 ? toUserId : fromUserId;

        User firstLocked = userRepo.findByIdForUpdate(first)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + first));
        User secondLocked = userRepo.findByIdForUpdate(second)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + second));

        User from = fromUserId.equals(first) ? firstLocked : secondLocked;
        User to = toUserId.equals(second) ? secondLocked : firstLocked;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        userRepo.updateById(from);
        userRepo.updateById(to);

        Transfer tx = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .status(TransferStatus.COMPLETED) // 此實作採同步結算
                .build();
        transferRepo.save(tx);

        // 發送事件（可供審計/通知）
        publisher.publishCreated(TransferCreatedEvent.builder()
                .transferId(tx.getTransferId())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount).build());
        publisher.publishCompleted(TransferCompletedEvent.builder()
                .transferId(tx.getTransferId())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount).build());

        // 交易提交後清除兩個帳戶的餘額快取
        cacheEvictor.evictBalancesAfterCommit(fromUserId, toUserId);
        return tx;
    }

    public Page<Transfer> getHistory(String userId, long current, long size) {
        Page<Transfer> page = new Page<>();
        page.setCurrent(current);
        page.setSize(size);
        return transferRepo.lambdaQuery()
                .eq(Transfer::getFromUserId, userId).or()
                .eq(Transfer::getToUserId, userId)
                .orderByDesc(Transfer::getCreatedAt)
                .page(page);
    }

    @Transactional
    public Transfer cancel(String transferId) {
        Transfer tx = transferRepo.findByIdForUpdate(transferId)
                .orElseThrow(() -> new IllegalArgumentException("transfer not found"));

        if (tx.getStatus() == TransferStatus.CANCELED) {
            throw new IllegalStateException("transfer already canceled");
        }

        Instant created = tx.getCreatedAt().toInstant();
        if (Instant.now().isAfter(created.plus(Duration.ofMinutes(cancelWindowMinutes)))) {
            throw new IllegalStateException("cancel window exceeded");
        }

        // 鎖定帳戶（與轉帳相同順序）
        String fromUserId = tx.getFromUserId();
        String toUserId = tx.getToUserId();
        String first = fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId;
        String second = fromUserId.compareTo(toUserId) < 0 ? toUserId : fromUserId;

        User firstLocked = userRepo.findByIdForUpdate(first)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + first));
        User secondLocked = userRepo.findByIdForUpdate(second)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + second));

        User from = fromUserId.equals(first) ? firstLocked : secondLocked;
        User to = toUserId.equals(second) ? secondLocked : firstLocked;

        BigDecimal amount = tx.getAmount();
        if (to.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("counterparty has insufficient balance to reverse");
        }

        // 反轉資金
        to.setBalance(to.getBalance().subtract(amount));
        from.setBalance(from.getBalance().add(amount));
        userRepo.updateById(from);
        userRepo.updateById(to);

        tx.setStatus(TransferStatus.CANCELED);
        tx.setCanceledAt(Timestamp.from(Instant.now()));
        transferRepo.updateById(tx);

        // 發送取消事件
        publisher.publishCanceled(TransferCanceledEvent.builder()
                .transferId(tx.getTransferId())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount).build());

        cacheEvictor.evictBalancesAfterCommit(fromUserId, toUserId);
        return tx;
    }
}
