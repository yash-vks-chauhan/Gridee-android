package com.parking.app.service.lock;

import com.mongodb.DuplicateKeyException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * MongoDB-based Distributed Lock Service (Fallback)
 * ACTIVE when: app.locking.provider=mongodb
 *
 * Purpose: No-Redis fallback for low-traffic environments
 * Performance: 5-10ms lock operations, uses main database
 *
 * Implementation: Uses MongoDB's unique index on lock collection
 * - acquireLock() -> tries to insert document with spotId as _id
 * - If insert succeeds -> lock acquired
 * - If DuplicateKeyException -> lock held by another process
 * - TTL index auto-expires locks to prevent deadlocks
 */
@Service
@ConditionalOnProperty(name = "app.locking.provider", havingValue = "mongodb")
public class MongoLockService implements LockService {

    private static final Logger logger = LoggerFactory.getLogger(MongoLockService.class);
    private static final String LOCK_COLLECTION = "booking_locks";
    private static final long DEFAULT_LOCK_TIMEOUT_MS = 30000; // 30 seconds

    private final MongoTemplate mongoTemplate;

    public MongoLockService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        createLockCollectionIndexes();
        logger.info("⚠️ MongoDB Lock Service ENABLED - Fallback mode for low-traffic environments");
        logger.info("   For production with high load, switch to Redis: app.locking.provider=redis");
    }

    /**
     * Create TTL index to auto-expire locks and prevent deadlocks
     */
    private void createLockCollectionIndexes() {
        try {
            mongoTemplate.getCollection(LOCK_COLLECTION)
                    .createIndex(new Document("expiresAt", 1),
                            new com.mongodb.client.model.IndexOptions()
                                    .expireAfter(0L, java.util.concurrent.TimeUnit.SECONDS));
            logger.debug("Created TTL index on booking_locks collection");
        } catch (Exception e) {
            logger.warn("Failed to create TTL index (may already exist): {}", e.getMessage());
        }
    }

    @Override
    public String acquireLock(String spotId, long waitTimeMs) {
        String lockValue = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        int attemptCount = 0;
        long backoffMs = 50; // Start with 50ms backoff
        long maxBackoffMs = 500; // Cap at 500ms

        logger.debug("🔒 Thread {} attempting to acquire lock for spot {}",
            Thread.currentThread().getName(), spotId);

        while (System.currentTimeMillis() - startTime < waitTimeMs) {
            attemptCount++;
            long elapsedMs = System.currentTimeMillis() - startTime;

            try {
                Document lock = new Document()
                        .append("_id", spotId)
                        .append("lockValue", lockValue)
                        .append("acquiredAt", new Date())
                        .append("threadName", Thread.currentThread().getName())
                        .append("expiresAt", new Date(System.currentTimeMillis() + DEFAULT_LOCK_TIMEOUT_MS));

                mongoTemplate.insert(lock, LOCK_COLLECTION);

                logger.info("✅ Thread {} ACQUIRED lock for spot {} (attempt #{}, elapsed: {}ms)",
                    Thread.currentThread().getName(), spotId, attemptCount, elapsedMs);
                return lockValue;

            } catch (DuplicateKeyException | org.springframework.dao.DuplicateKeyException e) {
                // Lock already held by another thread - wait and retry with exponential backoff
                // This catches both com.mongodb.DuplicateKeyException and org.springframework.dao.DuplicateKeyException
                long remainingTime = waitTimeMs - elapsedMs;

                if (remainingTime <= 0) {
                    logger.warn("⏱️ Thread {} TIMEOUT waiting for lock on spot {} after {}ms ({} attempts)",
                        Thread.currentThread().getName(), spotId, elapsedMs, attemptCount);
                    break;
                }

                // Log every 5 attempts to avoid spam
                if (attemptCount % 5 == 0) {
                    logger.debug("⏳ Thread {} waiting for lock on spot {} (attempt #{}, elapsed: {}ms, remaining: {}ms)",
                        Thread.currentThread().getName(), spotId, attemptCount, elapsedMs, remainingTime);
                }

                try {
                    // Exponential backoff: 50ms -> 100ms -> 200ms -> 400ms -> 500ms (capped)
                    long sleepTime = Math.min(backoffMs, Math.min(maxBackoffMs, remainingTime));
                    Thread.sleep(sleepTime);
                    backoffMs = Math.min(backoffMs * 2, maxBackoffMs);

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.warn("⚠️ Thread {} interrupted while waiting for lock on spot {}",
                        Thread.currentThread().getName(), spotId);
                    return null;
                }
            } catch (Exception e) {
                // Truly unexpected errors (not DuplicateKey)
                logger.error("❌ Unexpected error acquiring lock for spot {}: {}",
                    spotId, e.getClass().getName() + ": " + e.getMessage());
                return null;
            }
        }

        logger.warn("❌ Thread {} FAILED to acquire lock for spot {} after {}ms ({} attempts)",
            Thread.currentThread().getName(), spotId,
            System.currentTimeMillis() - startTime, attemptCount);
        return null;
    }

    @Override
    public boolean releaseLock(String spotId, String lockValue) {
        try {
            // Only delete if we own the lock (verify lockValue matches)
            Query query = Query.query(
                    Criteria.where("_id").is(spotId)
                            .and("lockValue").is(lockValue)
            );

            Document result = mongoTemplate.findAndRemove(query, Document.class, LOCK_COLLECTION);

            if (result != null) {
                logger.info("🔓 Thread {} RELEASED lock for spot {} with token {}",
                    Thread.currentThread().getName(), spotId, lockValue);
                return true;
            } else {
                logger.warn("⚠️ Thread {} failed to release lock for spot {} - token mismatch or already expired",
                    Thread.currentThread().getName(), spotId);
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ Error releasing lock for spot {}: {}", spotId, e.getMessage());
            return false;
        }
    }

    @Override
    public <T> T executeWithLock(String spotId, long waitTimeMs, LockOperation<T> operation) {
        String lockToken = acquireLock(spotId, waitTimeMs);
        if (lockToken == null) {
            throw new RuntimeException("Unable to acquire MongoDB lock for spot: " + spotId);
        }

        try {
            return operation.execute();
        } finally {
            releaseLock(spotId, lockToken);
        }
    }
}
