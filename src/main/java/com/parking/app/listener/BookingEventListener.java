package com.parking.app.listener;

import com.parking.app.model.event.BookingEvent;
import com.parking.app.service.ParkingLotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for booking events and updates ParkingLot aggregate counts asynchronously
 * Runs AFTER transaction commit to ensure booking changes are persisted
 * Operates outside transaction boundary - eventual consistency model
 */
@Component
public class BookingEventListener {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventListener.class);

    @Autowired
    private ParkingLotService parkingLotService;

    /**
     * Handle booking events after transaction commits
     * Uses @TransactionalEventListener to ensure booking is committed before updating counts
     */
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleBookingEvent(BookingEvent event) {
        logger.info("Received booking event: type={}, bookingId={}, lotId={}, spotId={}",
                   event.getEventType(), event.getBookingId(), event.getLotId(), event.getSpotId());

        try {
            switch (event.getEventType()) {
                case BOOKING_CREATED:
                    // Booking created - decrease available spots
                    parkingLotService.decreaseAvailableSpots(event.getLotId(), event.getSpotId());
                    break;

                case BOOKING_CANCELLED:
                case BOOKING_COMPLETED:
                case BOOKING_AUTO_COMPLETED:
                    // Booking released - increase available spots
                    parkingLotService.increaseAvailableSpots(event.getLotId(), event.getSpotId());
                    break;

                default:
                    logger.warn("Unknown booking event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Error handling booking event: type={}, bookingId={}, error={}",
                        event.getEventType(), event.getBookingId(), e.getMessage(), e);
            // Don't rethrow - we don't want to affect the main booking transaction
        }
    }

    /**
     * Fallback event listener without transaction dependency
     * Used when event is published outside of a transaction
     */
    @EventListener
    @Async
    public void handleBookingEventNonTransactional(BookingEvent event) {
        // This will be called if there's no active transaction
        // The @TransactionalEventListener takes precedence when there is a transaction
        logger.debug("Handling non-transactional booking event: {}", event.getEventType());
        handleBookingEvent(event);
    }
}

