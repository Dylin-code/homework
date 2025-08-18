package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.demo.enums.TransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@TableName("transfers")
public class Transfer {

    @TableId
    private String transferId;

    private String fromUserId;

    private String toUserId;

    private BigDecimal amount;

    private TransferStatus status;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp canceledAt;
}
