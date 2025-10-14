package com.parking.app.service.booking;

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
                Criteria.where("spotId").is(spotId)
                        .andOperator(
                                Criteria.where("status").in("pending", "active"),
                                Criteria.where("checkInTime").lt(Date.from(checkOutTime.toInstant())),
                                Criteria.where("checkOutTime").gt(Date.from(checkInTime.toInstant()))
                        )
        );
        if (mongoOperations.exists(overlapQuery, Bookings.class)) {
            throw new ConflictException("Booking time overlaps with an existing booking for this spot");
        }
    }

    public void ensureNoBookingOverlapForExtension(String spotId, String bookingId, ZonedDateTime currentCheckOut, ZonedDateTime newCheckOutTime) {
        Query overlapQuery = new Query(
                Criteria.where("spotId").is(spotId)
                        .andOperator(
                                Criteria.where("status").in("pending", "active"),
                                Criteria.where("checkInTime").lt(Date.from(newCheckOutTime.toInstant())),
                                Criteria.where("checkOutTime").gt(Date.from(currentCheckOut.toInstant())),
                                Criteria.where("_id").ne(bookingId)
                        )
        );
        if (mongoOperations.exists(overlapQuery, Bookings.class)) {
            throw new ConflictException("Cannot extend: spot is booked for the requested time");
        }
    }

    public void validateNoActiveBookingForUser(String userId) {
        Query active = new Query(Criteria.where("userId").is(userId).and("status").is("active"));
        if (mongoOperations.exists(active, Bookings.class)) {
            throw new ConflictException("User already has an active booking");
        }
    }

    public boolean isQrCodeValid(Bookings booking) {
        if (booking == null) return false;
        Date now = new Date();
        Date checkIn = booking.getCheckInTime();
        Date checkOut = booking.getCheckOutTime();
        return checkIn != null && checkOut != null && !now.before(checkIn) && !now.after(checkOut);
    }
}
