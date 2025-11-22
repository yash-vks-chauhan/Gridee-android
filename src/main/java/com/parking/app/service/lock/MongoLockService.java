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

        while (System.currentTimeMillis() - startTime < waitTimeMs) {
            try {
                Document lock = new Document()
                        .append("_id", spotId)
                        .append("lockValue", lockValue)
                        .append("acquiredAt", new Date())
                        .append("expiresAt", new Date(System.currentTimeMillis() + DEFAULT_LOCK_TIMEOUT_MS));

                mongoTemplate.insert(lock, LOCK_COLLECTION);
                logger.debug("Acquired MongoDB lock for spot {} with token {}", spotId, lockValue);
                return lockValue;

            } catch (DuplicateKeyException e) {
                // Lock already held by another process - wait and retry
                logger.debug("Lock already held for spot {}, waiting...", spotId);

                try {
                    Thread.sleep(100); // Backoff slightly longer than Redis
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        logger.warn("Failed to acquire MongoDB lock for spot {} after {}ms", spotId, waitTimeMs);
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
                logger.debug("Released MongoDB lock for spot {} with token {}", spotId, lockValue);
                return true;
            } else {
                logger.warn("Failed to release MongoDB lock for spot {} - token mismatch or already expired", spotId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error releasing MongoDB lock for spot {}: {}", spotId, e.getMessage());
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

