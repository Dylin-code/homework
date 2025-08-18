package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.model.dto.TransferCancelRes;
import com.example.demo.model.dto.TransferHistoryItem;
import com.example.demo.model.dto.TransferReq;
import com.example.demo.model.dto.TransferRes;
import com.example.demo.model.entity.Transfer;
import com.example.demo.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferRes> transfer(@Valid @RequestBody TransferReq req) {
        Transfer tx = transferService.createTransfer(req.getFromUserId(), req.getToUserId(), req.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TransferRes(tx.getTransferId(), tx.getFromUserId(), tx.getToUserId(), tx.getAmount(), tx.getStatus(), tx.getCreatedAt(), tx.getUpdatedAt()));
    }

    @GetMapping
    public Page<TransferHistoryItem> history(@RequestParam String userId,
                                             @RequestParam(defaultValue = "1") int current,
                                             @RequestParam(defaultValue = "20") int size) {
        var entities = transferService.getHistory(userId, current, size);
        var records = entities.getRecords().stream()
                .map(t -> new TransferHistoryItem(t.getTransferId(), t.getFromUserId(), t.getToUserId(), t.getAmount(), t.getStatus(), t.getCreatedAt()))
                .toList();
        Page<TransferHistoryItem> page = new Page<>();
        page.setCurrent(entities.getCurrent());
        page.setSize(entities.getSize());
        page.setTotal(entities.getTotal());
        page.setRecords(records);
        return page;
    }

    @PostMapping("/{transferId}/cancel")
    public TransferCancelRes cancel(@PathVariable String transferId) {
        var tx = transferService.cancel(transferId);
        return new TransferCancelRes(tx.getTransferId(), tx.getStatus(), tx.getCanceledAt());
    }
}
