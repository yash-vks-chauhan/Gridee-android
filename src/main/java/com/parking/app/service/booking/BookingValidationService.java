package com.parking.app.service.booking;

import com.parking.app.constants.BookingStatus;
import com.parking.app.exception.ConflictException;
import com.parking.app.model.Bookings;
import com.parking.app.model.Wallet;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Service responsible for booking validation logic
 */
@Service
public class BookingValidationService {

    private final MongoOperations mongoOperations;

    public BookingValidationService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public boolean hasSufficientBalance(Wallet wallet, double amount) {
        return wallet.getBalance() >= Math.ceil(amount);
    }

    public void ensureNoBookingOverlap(String spotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
        Query overlapQuery = new Query(
                Criteria.where(Bookings.FIELD_SPOT_ID).is(spotId)
                        .andOperator(
                                Criteria.where(Bookings.FIELD_STATUS).in(BookingStatus.PENDING.name(), BookingStatus.ACTIVE.name()),
                                Criteria.where(Bookings.FIELD_CHECK_IN_TIME).lt(Date.from(checkOutTime.toInstant())),
                                Criteria.where(Bookings.FIELD_CHECK_OUT_TIME).gt(Date.from(checkInTime.toInstant()))
                        )
        );
        if (mongoOperations.exists(overlapQuery, Bookings.class)) {
            throw new ConflictException("Booking time overlaps with an existing booking for this spot");
        }
    }

    public void ensureNoBookingOverlapForExtension(String spotId, String bookingId, ZonedDateTime currentCheckOut, ZonedDateTime newCheckOutTime) {
        Query overlapQuery = new Query(
                Criteria.where(Bookings.FIELD_SPOT_ID).is(spotId)
                        .andOperator(
                                Criteria.where(Bookings.FIELD_STATUS).in(BookingStatus.PENDING.name(), BookingStatus.ACTIVE.name()),
                                Criteria.where(Bookings.FIELD_CHECK_IN_TIME).lt(Date.from(newCheckOutTime.toInstant())),
                                Criteria.where(Bookings.FIELD_CHECK_OUT_TIME).gt(Date.from(currentCheckOut.toInstant())),
                                Criteria.where(Bookings.FIELD_ID).ne(bookingId)
                        )
        );
        if (mongoOperations.exists(overlapQuery, Bookings.class)) {
            throw new ConflictException("Cannot extend: spot is booked for the requested time");
        }
    }

    public void validateNoActiveBookingForUser(String userId) {
        Query active = new Query(Criteria.where(Bookings.FIELD_USER_ID).is(userId).and(Bookings.FIELD_STATUS).is(BookingStatus.ACTIVE.name()));
        if (mongoOperations.exists(active, Bookings.class)) {
            throw new ConflictException("User already has an active booking");
        }
    }
}
