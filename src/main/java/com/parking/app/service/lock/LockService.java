package com.parking.app.service.lock;

/**
 * Abstraction for distributed locking mechanism
 * Allows switching between Redis and MongoDB implementations
 */
public interface LockService {

    /**
     * Acquires a distributed lock for a parking spot
     * @param spotId The parking spot ID
     * @param waitTimeMs Maximum time to wait for lock acquisition
     * @return Lock token if successful, null otherwise
     */
    String acquireLock(String spotId, long waitTimeMs);

    /**
     * Releases a distributed lock
     * @param spotId The parking spot ID
     * @param lockValue The lock token received during acquisition
     * @return true if released successfully
     */
    boolean releaseLock(String spotId, String lockValue);

    /**
     * Execute operation with distributed lock
     * @param spotId The parking spot ID
     * @param waitTimeMs Maximum time to wait for lock
     * @param operation The operation to execute
     * @return Result of the operation
     * @throws RuntimeException if lock cannot be acquired
     */
    <T> T executeWithLock(String spotId, long waitTimeMs, LockOperation<T> operation);

    @FunctionalInterface
    interface LockOperation<T> {
        T execute();
    }
}

