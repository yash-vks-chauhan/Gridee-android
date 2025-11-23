package com.parking.app.config;

import com.mongodb.client.model.changestream.FullDocument;
import com.parking.app.listener.BookingChangeStreamListener;
import com.parking.app.model.Bookings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.*;

/**
 * Configuration for MongoDB Change Streams
 * Enables automatic audit trail for all Bookings collection changes
 *
 * Requirements:
 * - MongoDB must be running as a replica set (even for single node)
 * - Change streams are only available in MongoDB 3.6+
 * - Enable with: mongodb.audit.enabled=true
 *
 * How it works:
 * - Listens to all INSERT, UPDATE, DELETE operations on 'bookings' collection
 * - Automatically saves audit records to 'booking_audit' collection
 * - Captures old and new values for all changes
 * - No code changes needed in services - fully automatic!
 */
@Configuration
@ConditionalOnProperty(name = "mongodb.audit.enabled", havingValue = "true", matchIfMissing = false)
public class MongoChangeStreamConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoChangeStreamConfig.class);

    @Bean
    public MessageListenerContainer messageListenerContainer(MongoTemplate mongoTemplate,
                                                             BookingChangeStreamListener bookingListener) {

        logger.info("🔧 Initializing MongoDB Change Stream for Bookings audit...");

        try {
            // Create the message listener container
            MessageListenerContainer container = new DefaultMessageListenerContainer(mongoTemplate) {
                @Override
                public boolean isAutoStartup() {
                    return true;
                }
            };

            // Create change stream request for Bookings collection
            // FullDocument.UPDATE_LOOKUP ensures we get the complete document after updates
            ChangeStreamRequest<Bookings> request = ChangeStreamRequest.builder(bookingListener)
                .collection("bookings")
                .filter() // No filter - capture all changes (INSERT, UPDATE, DELETE)
                .fullDocumentLookup(FullDocument.UPDATE_LOOKUP)
                .build();

            // Register the listener
            container.register(request, Bookings.class);

            logger.info("✅ MongoDB Change Stream ENABLED for Bookings collection");
            logger.info("   📝 Automatic audit trail active");
            logger.info("   📊 Capturing: INSERT, UPDATE, DELETE operations");
            logger.info("   💾 Saving to: booking_audit collection");
            logger.info("");
            logger.info("   ⚠️  IMPORTANT: MongoDB must be running as a replica set");
            logger.info("   ℹ️  For local development, initialize replica set with:");
            logger.info("      rs.initiate()");

            return container;

        } catch (Exception e) {
            logger.error("❌ Failed to initialize MongoDB Change Stream", e);
            logger.error("   Possible causes:");
            logger.error("   1. MongoDB is not running as a replica set");
            logger.error("   2. MongoDB version is older than 3.6");
            logger.error("   3. Insufficient permissions");
            logger.error("");
            logger.error("   To disable audit, set: mongodb.audit.enabled=false");
            throw new RuntimeException("Failed to initialize MongoDB Change Stream. " +
                "Ensure MongoDB is running as a replica set (run 'rs.initiate()' in mongo shell)", e);
        }
    }
}
