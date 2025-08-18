package com.example.demo.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class BalanceCache {

    private final StringRedisTemplate redis;
    private final RedissonClient redisson;
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final int JITTER_SECONDS = 15;      // 隨機抖動，避免同秒失效
    private static final long LOCK_WAIT_MS = 80;       // 等拿鎖等待時間
    private static final long LOCK_LEASE_MS = 3000;    // 鎖租期，靠 watchdog 自動續租亦可
    private static final long SPIN_WAIT_MS = 200;      // 等待他人填充快取的輪詢時間
    private static final long SPIN_STEP_MS = 20;

    public BigDecimal get(String userId, Supplier<BigDecimal> dbLoader) {
        String key = key(userId);

        // 1) 先查快取
        String v = redis.opsForValue().get(key);
        if (v != null) return new BigDecimal(v);

        // 2) 嘗試搶鎖（單飛）
        RLock lock = redisson.getLock(lockKey(userId));
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_MS, LOCK_LEASE_MS, TimeUnit.MILLISECONDS);
            if (locked) {
                // double-check，避免驚群中第二次回源
                v = redis.opsForValue().get(key);
                if (v != null) return new BigDecimal(v);

                BigDecimal dbVal = dbLoader.get(); // 只有鎖主回源 DB
                if (dbVal != null) {
                    long ttl = TTL.getSeconds() + ThreadLocalRandom.current().nextInt(JITTER_SECONDS + 1);
                    redis.opsForValue().set(key, dbVal.toPlainString(), ttl, TimeUnit.SECONDS);
                } else {
                    // 不存在則做「負快取」：短 TTL 防穿透
                    redis.opsForValue().set(key, "NULL", 10, TimeUnit.SECONDS);
                    throw new IllegalArgumentException("user not found");
                }
                return dbVal;
            } else {
                // 3) 沒搶到鎖 -> 旋轉等待他人填充（避免直接打 DB）
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SPIN_WAIT_MS);
                do {
                    v = redis.opsForValue().get(key);
                    if (v != null) {
                        if ("NULL".equals(v)) throw new IllegalArgumentException("user not found");
                        return new BigDecimal(v);
                    }
                    Thread.sleep(SPIN_STEP_MS);
                } while (System.nanoTime() < deadline);

                // 超時保底：回源 DB，但「不寫回快取」，避免與鎖主競賽
                BigDecimal dbVal = dbLoader.get();
                if (dbVal == null) throw new IllegalArgumentException("user not found");
                return dbVal;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // 中斷則直接走 DB，仍不寫回快取
            BigDecimal dbVal = dbLoader.get();
            if (dbVal == null) throw new IllegalArgumentException("user not found");
            return dbVal;
        } finally {
            if (locked) {
                try { lock.unlock(); } catch (Exception ignore) {}
            }
        }
    }

    public void evict(String userId) {
        redis.delete(key(userId));
    }

    private String key(String userId) { return "balances:" + userId; }
    private String lockKey(String userId) { return "lock:balances:" + userId; }
}
