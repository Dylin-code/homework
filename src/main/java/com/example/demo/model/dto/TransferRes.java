package com.example.demo.model.dto;

import com.example.demo.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class TransferRes {
    private String transferId;
    private String fromUserId;
    private String toUserId;
    private BigDecimal amount;
    private TransferStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
