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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock
    private UserRepo userRepo;

    @Mock
    private TransferRepo transferRepo;

    @Mock
    private TransferEventPublisher publisher;

    @Mock
    private TxCacheEvictor cacheEvictor;

    @InjectMocks
    private TransferService transferService;

    private User fromUser;
    private User toUser;
    private Transfer transfer;
    private final String fromUserId = "user1";
    private final String toUserId = "user2";
    private final BigDecimal amount = new BigDecimal("100");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transferService, "cancelWindowMinutes", 10L);

        fromUser = User.builder()
                .userId(fromUserId)
                .balance(new BigDecimal("1000"))
                .build();

        toUser = User.builder()
                .userId(toUserId)
                .balance(new BigDecimal("500"))
                .build();

        transfer = Transfer.builder()
                .transferId(UUID.randomUUID().toString())
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .status(TransferStatus.COMPLETED)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    @Test
    void createTransfer_Success() {
        // 按字典序决定加锁顺序
        when(userRepo.findByIdForUpdate(fromUserId)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByIdForUpdate(toUserId)).thenReturn(Optional.of(toUser));

        Transfer result = transferService.createTransfer(fromUserId, toUserId, amount);

        // 验证结果
        assertNotNull(result);
        assertEquals(TransferStatus.COMPLETED, result.getStatus());

        // 验证余额变化
        assertEquals(new BigDecimal("900"), fromUser.getBalance());
        assertEquals(new BigDecimal("600"), toUser.getBalance());

        // 验证方法调用
        verify(userRepo).updateById(fromUser);
        verify(userRepo).updateById(toUser);
        verify(transferRepo).save(any(Transfer.class));

        // 验证事件发布
        verify(publisher).publishCreated(any(TransferCreatedEvent.class));
        verify(publisher).publishCompleted(any(TransferCompletedEvent.class));

        // 验证缓存清除
        verify(cacheEvictor).evictBalancesAfterCommit(fromUserId, toUserId);
    }

    @Test
    void createTransfer_InsufficientBalance() {
        // 设置余额不足
        fromUser.setBalance(new BigDecimal("50"));

        when(userRepo.findByIdForUpdate(fromUserId)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByIdForUpdate(toUserId)).thenReturn(Optional.of(toUser));

        // 验证异常抛出
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.createTransfer(fromUserId, toUserId, amount));

        assertEquals("insufficient balance", exception.getMessage());

        // 验证没有更新和保存操作
        verify(userRepo, never()).updateById(any());
        verify(transferRepo, never()).save(any());
        verify(publisher, never()).publishCreated(any());
        verify(publisher, never()).publishCompleted(any());
        verify(cacheEvictor, never()).evictBalancesAfterCommit(anyString(), anyString());
    }

    @Test
    void createTransfer_SameUser() {
        // 验证自己转给自己的情况
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.createTransfer(fromUserId, fromUserId, amount));

        assertEquals("fromUserId and toUserId cannot be the same", exception.getMessage());
    }

    @Test
    void createTransfer_NegativeAmount() {
        // 验证负数金额的情况
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.createTransfer(fromUserId, toUserId, new BigDecimal("-10")));

        assertEquals("amount must be > 0", exception.getMessage());
    }

    @Test
    void cancel_Success() {
        // 设置创建时间为当前时间，在取消窗口内
        transfer.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(60)));

        when(transferRepo.findByIdForUpdate(transfer.getTransferId())).thenReturn(Optional.of(transfer));
        when(userRepo.findByIdForUpdate(fromUserId)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByIdForUpdate(toUserId)).thenReturn(Optional.of(toUser));

        Transfer result = transferService.cancel(transfer.getTransferId());

        // 验证状态变化
        assertEquals(TransferStatus.CANCELED, result.getStatus());
        assertNotNull(result.getCanceledAt());

        // 验证余额恢复
        assertEquals(new BigDecimal("1100"), fromUser.getBalance());
        assertEquals(new BigDecimal("400"), toUser.getBalance());

        // 验证方法调用
        verify(userRepo).updateById(fromUser);
        verify(userRepo).updateById(toUser);
        verify(transferRepo).updateById(transfer);
        verify(publisher).publishCanceled(any(TransferCanceledEvent.class));
        verify(cacheEvictor).evictBalancesAfterCommit(fromUserId, toUserId);
    }

    @Test
    void cancel_AlreadyCanceled() {
        transfer.setStatus(TransferStatus.CANCELED);

        when(transferRepo.findByIdForUpdate(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.cancel(transfer.getTransferId()));

        assertEquals("transfer already canceled", exception.getMessage());
    }

    @Test
    void cancel_WindowExceeded() {
        // 设置创建时间超过取消窗口
        transfer.setCreatedAt(Timestamp.from(Instant.now().minusSeconds(20 * 60L)));

        when(transferRepo.findByIdForUpdate(transfer.getTransferId())).thenReturn(Optional.of(transfer));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.cancel(transfer.getTransferId()));

        assertEquals("cancel window exceeded", exception.getMessage());
    }

    @Test
    void cancel_InsufficientCounterpartyBalance() {
        transfer.setCreatedAt(Timestamp.from(Instant.now()));
        toUser.setBalance(new BigDecimal("50")); // 设置接收方余额不足以退还

        when(transferRepo.findByIdForUpdate(transfer.getTransferId())).thenReturn(Optional.of(transfer));
        when(userRepo.findByIdForUpdate(fromUserId)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByIdForUpdate(toUserId)).thenReturn(Optional.of(toUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> transferService.cancel(transfer.getTransferId()));

        assertEquals("counterparty has insufficient balance to reverse", exception.getMessage());
    }

    // 模拟LambdaQueryChain类以支持测试
    private static class MockLambdaQueryChain<T> {
        public MockLambdaQueryChain<T> eq(Object column, Object val) {
            return this;
        }

        public MockLambdaQueryChain<T> or() {
            return this;
        }

        public MockLambdaQueryChain<T> orderByDesc(Object column) {
            return this;
        }

        public Page<T> page(Page<T> page) {
            return page;
        }
    }

}