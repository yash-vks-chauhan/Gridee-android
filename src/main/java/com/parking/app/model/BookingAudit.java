package com.parking.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Audit trail for all booking changes
 * Tracks complete history of create, update, cancel, and delete operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "booking_audit")
public class BookingAudit {

    @Id
    private String id;

    /**
     * Reference to the booking being audited
     */
    @Indexed
    private String bookingId;

    /**
     * Type of operation performed
     */
    @Indexed
    private AuditAction action;

    /**
     * User who performed the action
     */
    @Indexed
    private String performedBy;

    /**
     * User ID associated with the booking
     */
    @Indexed
    private String userId;

    /**
     * Spot ID for the booking
     */
    @Indexed
    private String spotId;

    /**
     * Lot name/ID for the booking
     */
    @Indexed
    private String lotName;

    /**
     * Vehicle number
     */
    private String vehicleNumber;

    /**
     * Previous status before the change
     */
    private String oldStatus;

    /**
     * New status after the change
     */
    private String newStatus;

    /**
     * Previous values (JSON map of field names to old values)
     */
    private Map<String, Object> oldValues;

    /**
     * New values (JSON map of field names to new values)
     */
    private Map<String, Object> newValues;

    /**
     * Booking amount at the time of audit
     */
    private Double amount;

    /**
     * Check-in time at the time of audit
     */
    private Date checkInTime;

    /**
     * Check-out time at the time of audit
     */
    private Date checkOutTime;

    /**
     * Timestamp when the audit record was created
     */
    @Indexed
    private ZonedDateTime timestamp;

    /**
     * IP address of the user who performed the action
     */
    private String ipAddress;

    /**
     * User agent (browser/device info)
     */
    private String userAgent;

    /**
     * Additional notes or reason for the action
     */
    private String notes;

    /**
     * Whether this was an automatic action (e.g., auto-completion)
     */
    private Boolean automated;

    /**
     * Operator ID if action was performed by an operator
     */
    private String operatorId;

    /**
     * Enum for audit actions
     */
    public enum AuditAction {
        CREATED,           // Booking was created
        UPDATED,           // Booking details were updated
        STATUS_CHANGED,    // Status was changed
        CHECKED_IN,        // User checked in
        CHECKED_OUT,       // User checked out
        EXTENDED,          // Booking was extended
        CANCELLED,         // Booking was cancelled
        AUTO_COMPLETED,    // Booking was auto-completed
        DELETED,           // Booking was deleted (admin action)
        REFUNDED,          // Refund was processed
        PENALTY_APPLIED    // Penalty was applied
    }
}
