package com.parking.app.config;

import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * MongoDB Index Configuration for Optimal Performance
 *
 * CRITICAL FOR CONCURRENCY:
 * These indexes ensure fast lookups during high-load scenarios
 * Without proper indexes, overlap checks and spot queries will be slow
 * causing timeout and poor user experience under heavy load
 *
 * ALL INDEX CREATION IS CENTRALIZED HERE - No @CompoundIndex in models
 */
@Configuration
public class MongoIndexConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

    @Bean
    public CommandLineRunner createIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            logger.info("Creating MongoDB indexes for optimal booking performance...");

            // ===== BOOKINGS COLLECTION INDEXES =====
            IndexOperations bookingIndexOps = mongoTemplate.indexOps(Bookings.class);

            // Drop all existing indexes to prevent conflicts (except _id)
            try {
                bookingIndexOps.getIndexInfo().forEach(indexInfo -> {
                    String indexName = indexInfo.getName();
                    if (!indexName.equals("_id_")) {
                        try {
                            bookingIndexOps.dropIndex(indexName);
                            logger.debug("Dropped existing index: {}", indexName);
                        } catch (Exception e) {
                            logger.warn("Could not drop index {}: {}", indexName, e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                logger.warn("Error dropping indexes: {}", e.getMessage());
            }

            // Compound index for overlap detection (CRITICAL for preventing double-booking)
            // This index is used in ensureNoBookingOverlap() query
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_SPOT_ID, Sort.Direction.ASC)
                    .on(Bookings.FIELD_STATUS, Sort.Direction.ASC)
                    .on(Bookings.FIELD_CHECK_IN_TIME, Sort.Direction.ASC)
                    .on(Bookings.FIELD_CHECK_OUT_TIME, Sort.Direction.ASC)
                    .named("idx_spot_status_times")
            );

            // Index for user bookings lookup (fast user history queries)
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_USER_ID, Sort.Direction.ASC)
                    .on(Bookings.FIELD_STATUS, Sort.Direction.ASC)
                    .named("idx_user_status")
            );

            // Index for vehicle number lookup (check-in by vehicle number)
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_VEHICLE_NUMBER, Sort.Direction.ASC)
                    .on(Bookings.FIELD_STATUS, Sort.Direction.ASC)
                    .named("idx_vehicle_status")
            );

            // Index for lot-based queries (admin dashboard)
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_LOT_ID, Sort.Direction.ASC)
                    .on(Bookings.FIELD_CHECK_IN_TIME, Sort.Direction.DESC)
                    .named("idx_lot_checkin")
            );

            // Index for operator check-ins (audit trail)
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_CHECK_IN_OPERATOR_ID, Sort.Direction.ASC)
                    .named("idx_checkin_operator")
            );

            // Index for operator check-outs (audit trail)
            createIndexSafely(bookingIndexOps, new Index()
                    .on(Bookings.FIELD_CHECK_OUT_OPERATOR_ID, Sort.Direction.ASC)
                    .named("idx_checkout_operator")
            );

            // ===== PARKING SPOT COLLECTION INDEXES =====
            IndexOperations spotIndexOps = mongoTemplate.indexOps(ParkingSpot.class);

            // Drop all existing indexes to prevent conflicts (except _id)
            try {
                spotIndexOps.getIndexInfo().forEach(indexInfo -> {
                    String indexName = indexInfo.getName();
                    if (!indexName.equals("_id_")) {
                        try {
                            spotIndexOps.dropIndex(indexName);
                            logger.debug("Dropped existing spot index: {}", indexName);
                        } catch (Exception e) {
                            logger.warn("Could not drop spot index {}: {}", indexName, e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                logger.warn("Error dropping spot indexes: {}", e.getMessage());
            }

            // Index for lot-based spot queries
            createIndexSafely(spotIndexOps, new Index()
                    .on(ParkingSpot.FIELD_LOT_ID, Sort.Direction.ASC)
                    .on(ParkingSpot.FIELD_AVAILABLE, Sort.Direction.DESC)
                    .named("idx_lot_availability")
            );

            // Index for spot availability (fast availability checks)
            createIndexSafely(spotIndexOps, new Index()
                    .on(ParkingSpot.FIELD_AVAILABLE, Sort.Direction.DESC)
                    .named("idx_availability")
            );

            logger.info("✅ MongoDB indexes created successfully for high-concurrency support");
        };
    }

    /**
     * Safely creates an index, handling conflicts gracefully
     */
    private void createIndexSafely(IndexOperations indexOps, Index index) {
        try {
            String indexName = indexOps.createIndex(index);
            logger.debug("Created/verified index: {} -> {}", index.getIndexKeys(), indexName);
        } catch (Exception e) {
            logger.error("Failed to create index {}: {}", index.getIndexKeys(), e.getMessage());
            throw e;
        }
    }
}
