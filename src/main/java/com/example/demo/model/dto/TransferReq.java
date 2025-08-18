package com.example.demo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class TransferReq {
    @NotBlank
    private String fromUserId;
    @NotBlank
    private String toUserId;
    @Positive
    private BigDecimal amount;
}
