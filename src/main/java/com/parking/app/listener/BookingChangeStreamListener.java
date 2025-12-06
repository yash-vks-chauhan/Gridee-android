package com.parking.app.listener;

import com.mongodb.client.model.changestream.OperationType;
import com.parking.app.model.BookingAudit;
import com.parking.app.model.Bookings;
import com.parking.app.repository.BookingAuditRepository;
import org.bson.BsonDocument;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple MongoDB Change Stream Listener for Bookings Collection
 * Captures all changes and stores them as audit history entries
 */
@Component
public class BookingChangeStreamListener implements MessageListener<com.mongodb.client.model.changestream.ChangeStreamDocument<Document>, Bookings> {

    private static final Logger logger = LoggerFactory.getLogger(BookingChangeStreamListener.class);

    private final BookingAuditRepository auditRepository;

    public BookingChangeStreamListener(BookingAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void onMessage(Message<com.mongodb.client.model.changestream.ChangeStreamDocument<Document>, Bookings> message) {
        try {
            com.mongodb.client.model.changestream.ChangeStreamDocument<Document> changeStream = message.getRaw();
            OperationType operationType = changeStream.getOperationType();

            logger.debug("📝 Change detected: {} - BookingId: {}",
                operationType, changeStream.getDocumentKey());

            BookingAudit audit = createSimpleAuditEntry(changeStream, operationType);

            if (audit != null) {
                auditRepository.save(audit);
                logger.info("✅ Audit history saved - Action: {}, BookingId: {}",
                    audit.getAction(), audit.getBookingId());
            }

        } catch (Exception e) {
            logger.error("❌ Error saving audit history: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a simple audit entry - just store the current state
     */
    private BookingAudit createSimpleAuditEntry(
            com.mongodb.client.model.changestream.ChangeStreamDocument<Document> changeStream,
            OperationType operationType) {

        String bookingId = extractBookingId(changeStream.getDocumentKey());
        Document fullDocument = changeStream.getFullDocument();

        // For DELETE - just log it
        if (operationType == OperationType.DELETE) {
            return BookingAudit.builder()
                .bookingId(bookingId)
                .action(BookingAudit.AuditAction.DELETED)
                .performedBy("SYSTEM")
                .timestamp(ZonedDateTime.now())
                .notes("Booking deleted")
                .build();
        }

        // For INSERT/UPDATE/REPLACE - capture the current state
        if (fullDocument == null) {
            logger.warn("⚠️ No document available for {} operation", operationType);
            return null;
        }

        // Simple action determination
        BookingAudit.AuditAction action = getSimpleAction(operationType, fullDocument);

        // Just store the complete snapshot
        Map<String, Object> snapshot = new HashMap<>();
        fullDocument.forEach((key, value) -> {
            if (!key.equals("_id") && !key.equals("_class")) {
                snapshot.put(key, value);
            }
        });

        return BookingAudit.builder()
            .bookingId(bookingId)
            .action(action)
            .performedBy(fullDocument.getString("userId"))
            .userId(fullDocument.getString("userId"))
            .spotId(fullDocument.getString("spotId"))
            .lotName(fullDocument.getString("lotName"))
            .vehicleNumber(fullDocument.getString("vehicleNumber"))
            .newStatus(fullDocument.getString("status"))
            .newValues(snapshot)
            .amount(fullDocument.getDouble("amount"))
            .checkInTime(fullDocument.getDate("checkInTime"))
            .checkOutTime(fullDocument.getDate("checkOutTime"))
            .timestamp(ZonedDateTime.now())
            .automated(fullDocument.getBoolean("autoCompleted", false))
            .notes(operationType.getValue() + " - Snapshot captured")
            .build();
    }

    /**
     * Simple action mapping based on operation type
     */
    private BookingAudit.AuditAction getSimpleAction(OperationType operationType, Document document) {
        if (operationType == OperationType.INSERT) {
            return BookingAudit.AuditAction.CREATED;
        }

        // For updates, use status if available, otherwise just mark as UPDATED
        String status = document.getString("status");
        if (status != null) {
            switch (status) {
                case "CANCELLED":
                    return BookingAudit.AuditAction.CANCELLED;
                case "COMPLETED":
                    return BookingAudit.AuditAction.CHECKED_OUT;
                case "ACTIVE":
                    return BookingAudit.AuditAction.CHECKED_IN;
            }
        }

        return BookingAudit.AuditAction.UPDATED;
    }

    /**
     * Extract booking ID from document key
     */
    private String extractBookingId(BsonDocument documentKey) {
        if (documentKey != null && documentKey.containsKey("_id")) {
            return documentKey.get("_id").asObjectId().getValue().toString();
        }
        return "UNKNOWN";
    }
}
