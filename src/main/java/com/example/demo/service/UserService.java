package com.example.demo.service;

import com.example.demo.model.entity.User;
import com.example.demo.repository.UserRepo;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;

    @Transactional
    public User createUser(@NotBlank String userId, BigDecimal initialBalance) {
        if (userRepo.lambdaQuery().eq(User::getUserId, userId).exists()) {
            throw new IllegalArgumentException("userId already exists");
        }
        if (initialBalance == null || initialBalance.signum() < 0) {
            throw new IllegalArgumentException("initialBalance must be >= 0");
        }
        User ua = User.builder()
                .userId(userId)
                .balance(initialBalance)
                .build();
        userRepo.save(ua);
        return ua;
    }

    @Cacheable(cacheNames = "balances", key = "#userId")
    public BigDecimal getBalance(String userId) {
        return userRepo.lambdaQuery()
                .eq(User::getUserId, userId)
                .oneOpt()
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getBalance();
    }
}