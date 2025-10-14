package com.parking.app.service.booking;

import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.util.BookingUtility;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

/**
 * Service responsible for QR code validation operations
 */
@Service
public class QRCodeValidationService {

    public QrValidationResult validateQrCodeForCheckIn(Bookings booking, String qrCode, ParkingSpot spot) {
        if (booking == null) {
            return new QrValidationResult(false, 0, "Booking not found");
        }
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            return new QrValidationResult(false, 0, "Booking is not pending");
        }
        if (!booking.getId().equals(qrCode)) {
            return new QrValidationResult(false, 0, "Invalid QR code for check-in");
        }
        if (spot == null) {
            return new QrValidationResult(false, 0, "Parking spot not found");
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(
                booking.getCheckInTime().toInstant(), now.getZone());
        double penalty = BookingUtility.calculatePenaltyWithGrace(
                scheduledCheckIn, now, spot.getCheckInPenaltyRate());

        if (penalty > 0) {
            return new QrValidationResult(true, penalty, "Penalty applies for late check-in");
        }
        return new QrValidationResult(true, 0, "QR code valid for check-in");
    }

    public QrValidationResult validateQrCodeForCheckOut(Bookings booking, String qrCode,
                                                        String bookingId, ParkingSpot spot) {
        if (booking == null) {
            return new QrValidationResult(false, 0, "Booking not found");
        }
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            return new QrValidationResult(false, 0, "Booking is not active");
        }
        if (!bookingId.equals(qrCode)) {
            return new QrValidationResult(false, 0, "Invalid QR code for checkout");
        }
        if (spot == null) {
            return new QrValidationResult(false, 0, "Parking spot not found");
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(
                booking.getCheckOutTime().toInstant(), now.getZone());
        double penalty = BookingUtility.calculatePenaltyWithGrace(
                scheduledEnd, now, spot.getCheckOutPenaltyRate());

        if (penalty > 0) {
            return new QrValidationResult(true, penalty, "Penalty applies for late check-out");
        }
        return new QrValidationResult(true, 0, "QR code valid for checkout");
    }

    public static class QrValidationResult {
        public boolean valid;
        public double penalty;
        public String message;

        public QrValidationResult(boolean valid, double penalty, String message) {
            this.valid = valid;
            this.penalty = penalty;
            this.message = message;
        }
    }
}
