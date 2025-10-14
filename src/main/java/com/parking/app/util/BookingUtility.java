package com.parking.app.util;

import com.parking.app.exception.ConflictException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Utility class for booking-related calculations and validations
 */
public class BookingUtility {

    public static void validateTimes(ZonedDateTime checkIn, ZonedDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out times are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    public static void validateBookingWindow(ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
        LocalDate today = ZonedDateTime.now().toLocalDate();
        LocalTime nowTime = ZonedDateTime.now().toLocalTime();
        LocalTime cutoff = LocalTime.of(20, 0);

        if (nowTime.isBefore(cutoff)) {
            if (!checkInTime.toLocalDate().isEqual(today) || checkOutTime.toLocalTime().isAfter(cutoff)) {
                throw new ConflictException("Bookings before 8pm must be for today and end by 8pm");
            }
        } else {
            if (!checkInTime.toLocalDate().isEqual(today.plusDays(1))) {
                throw new ConflictException("Bookings after 8pm must be for tomorrow");
            }
        }
    }

    public static double calculatePenaltyWithGrace(ZonedDateTime scheduled, ZonedDateTime actual, double ratePerMinute) {
        long minutesLate = java.time.Duration.between(scheduled, actual).toMinutes();
        if (minutesLate <= 10) {
            return 0;
        }
        return (minutesLate - 10) * ratePerMinute;
    }

    public static double calculateCharge(ZonedDateTime from, ZonedDateTime to, double bookingRate) {
        long hours = java.time.temporal.ChronoUnit.HOURS.between(from, to);
        if (from.plusHours(hours).isBefore(to)) {
            hours++;
        }
        return hours * bookingRate;
    }
}
