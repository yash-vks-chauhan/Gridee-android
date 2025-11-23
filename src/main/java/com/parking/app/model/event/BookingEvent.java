package com.parking.app.model.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a booking state changes
 * Used to asynchronously update ParkingLot aggregate counts
 */
@Getter
public class BookingEvent extends ApplicationEvent {

    private final String bookingId;
    private final String lotName;
    private final String spotId;
    private final BookingEventType eventType;
    private final String userId;

    public BookingEvent(Object source, String bookingId, String lotName, String spotId,
                        BookingEventType eventType, String userId) {
        super(source);
        this.bookingId = bookingId;
        this.lotName = lotName;
        this.spotId = spotId;
        this.eventType = eventType;
        this.userId = userId;
    }

    public enum BookingEventType {
        BOOKING_CREATED,        // Spot reserved, decrease available count
        BOOKING_CANCELLED,      // Spot released, increase available count
        BOOKING_COMPLETED,      // Checkout done, increase available count
        BOOKING_AUTO_COMPLETED  // Auto-completed, increase available count
    }
}

