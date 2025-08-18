package com.example.demo.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserReq {
    @NotBlank
    private String userId;

    @Min(0)
    private long initialBalance;
}
