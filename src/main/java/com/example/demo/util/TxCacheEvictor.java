package com.example.demo.util;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class TxCacheEvictor {
    private final CacheManager cacheManager;

    public void evictBalancesAfterCommit(String... userIds) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String userId : userIds) {
                    if (cacheManager.getCache("balances") != null) {
                        cacheManager.getCache("balances").evictIfPresent(userId);
                    }
                }
            }
        });
    }
}
