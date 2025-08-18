package com.example.demo.model.dto;

import com.example.demo.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class TransferCancelRes {
    private String transferId;
    private TransferStatus status;
    private Timestamp canceledAt;
}
