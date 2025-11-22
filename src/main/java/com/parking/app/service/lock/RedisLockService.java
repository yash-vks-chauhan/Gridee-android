package com.parking.app.service.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based Distributed Lock Service
 * ACTIVE when: app.locking.provider=redis (default)
 *
 * Purpose: High-performance distributed locking for multi-instance deployments
 * Performance: <1ms lock operations, automatic TTL, no database overhead
 */
@Service
@ConditionalOnProperty(name = "app.locking.provider", havingValue = "redis", matchIfMissing = true)
public class RedisLockService implements LockService {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockService.class);
    private static final String LOCK_PREFIX = "booking:lock:";
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 10000; // 10 seconds

    private final RedisTemplate<String, String> redisTemplate;

    public RedisLockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        logger.info("✅ Redis Lock Service ENABLED - High-performance distributed locking active");
    }

    @Override
    public String acquireLock(String spotId, long waitTimeMs) {
        String lockKey = LOCK_PREFIX + spotId;
        String lockValue = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < waitTimeMs) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, DEFAULT_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                logger.debug("Acquired Redis lock for spot {} with token {}", spotId, lockValue);
                return lockValue;
            }

            // Small backoff to reduce contention
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        logger.warn("Failed to acquire Redis lock for spot {} after {}ms", spotId, waitTimeMs);
        return null;
    }

    @Override
    public boolean releaseLock(String spotId, String lockValue) {
        String lockKey = LOCK_PREFIX + spotId;
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        // Only release if we own the lock
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
            logger.debug("Released Redis lock for spot {} with token {}", spotId, lockValue);
            return true;
        }

        logger.warn("Failed to release Redis lock for spot {} - token mismatch", spotId);
        return false;
    }

    @Override
    public <T> T executeWithLock(String spotId, long waitTimeMs, LockOperation<T> operation) {
        String lockToken = acquireLock(spotId, waitTimeMs);
        if (lockToken == null) {
            throw new RuntimeException("Unable to acquire Redis lock for spot: " + spotId);
        }

        try {
            return operation.execute();
        } finally {
            releaseLock(spotId, lockToken);
        }
    }
}

