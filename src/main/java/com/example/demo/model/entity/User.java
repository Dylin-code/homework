package com.example.demo.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("users")
public class User {

    @TableId
    private String userId;

    private BigDecimal balance;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
