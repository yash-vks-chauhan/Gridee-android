package com.parking.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to handle JWT token blacklisting for logout and security
 */
@Service
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    // Store blacklisted tokens with expiration time
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // Clean up expired tokens every 1 hour
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);
    private long lastCleanup = System.currentTimeMillis();

    /**
     * Add token to blacklist
     */
    public void blacklistToken(String token, long expirationTimeMs) {
        blacklistedTokens.put(token, expirationTimeMs);
        logger.info("Token blacklisted: {}", token.substring(0, Math.min(20, token.length())) + "...");
        cleanupExpiredTokensIfNeeded();
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isBlacklisted(String token) {
        Long expirationTime = blacklistedTokens.get(token);
        if (expirationTime == null) {
            return false;
        }

        // Remove if expired
        if (System.currentTimeMillis() > expirationTime) {
            blacklistedTokens.remove(token);
            return false;
        }

        return true;
    }

    /**
     * Cleanup expired tokens from memory
     */
    private void cleanupExpiredTokensIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            AtomicInteger removedCount = new AtomicInteger(0);
            blacklistedTokens.entrySet().removeIf(entry -> {
                boolean expired = now > entry.getValue();
                if (expired) {
                    removedCount.incrementAndGet();
                }
                return expired;
            });
            lastCleanup = now;
            if (removedCount.get() > 0) {
                logger.info("Cleaned up {} expired blacklisted tokens", removedCount.get());
            }
        }
    }

    /**
     * Clear all blacklisted tokens (for testing/admin purposes)
     */
    public void clearAll() {
        int size = blacklistedTokens.size();
        blacklistedTokens.clear();
        logger.warn("Cleared all {} blacklisted tokens", size);
    }

    /**
     * Get count of blacklisted tokens
     */
    public int getBlacklistedCount() {
        return blacklistedTokens.size();
    }
}
