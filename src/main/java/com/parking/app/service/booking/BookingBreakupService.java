package com.parking.app.service.booking;

import com.parking.app.constants.BookingStatus;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.util.BookingUtility;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for calculating booking breakups, penalties, and refunds
 */
@Service
public class BookingBreakupService {

    public Map<String, Object> calculateBookingBreakup(Bookings booking, ParkingSpot spot) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
        ZonedDateTime actualCheckIn = booking.getActualCheckInTime() != null
                ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                : scheduledCheckIn;

        double lateCheckInPenalty = 0.0;
        double lateCheckOutPenalty = 0.0;
        double refund = 0.0;

        boolean isAutoCompleted = booking.getAutoCompleted() != null && booking.getAutoCompleted();

        if (BookingStatus.CANCELLED.name().equalsIgnoreCase(booking.getStatus())) {
            boolean isAutoCancelled = actualCheckIn == null && now.isAfter(scheduledCheckIn);
            if (isAutoCancelled) {
                lateCheckInPenalty = BookingUtility.calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());
                refund = 0.0;
            } else if (now.isAfter(scheduledCheckIn)) {
                lateCheckInPenalty = BookingUtility.calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());
                refund = booking.getAmount();
            } else {
                refund = booking.getAmount();
            }
        } else if (BookingStatus.COMPLETED.name().equalsIgnoreCase(booking.getStatus())) {
            ZonedDateTime actualCheckOut = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
            lateCheckInPenalty = BookingUtility.calculatePenaltyWithGrace(scheduledCheckIn, actualCheckIn, spot.getCheckInPenaltyRate());
            lateCheckOutPenalty = BookingUtility.calculatePenaltyWithGrace(scheduledEnd, actualCheckOut, spot.getCheckOutPenaltyRate());
            if (!isAutoCompleted) {
                refund = booking.getAmount();
            }
        }

        double subtotal = booking.getAmount() + lateCheckInPenalty + lateCheckOutPenalty;
        double totalDeducted = subtotal - refund;

        Map<String, Object> breakup = new HashMap<>();
        breakup.put("bookingCharge", booking.getAmount());
        breakup.put("lateCheckInPenalty", lateCheckInPenalty);
        breakup.put("lateCheckOutPenalty", lateCheckOutPenalty);
        breakup.put("subtotal", subtotal);
        breakup.put("refundAmount", refund);
        breakup.put("totalDeducted", totalDeducted);
        breakup.put("status", booking.getStatus());
        breakup.put("bookingRate", spot.getBookingRate());
        breakup.put("checkInPenaltyRate", spot.getCheckInPenaltyRate());
        breakup.put("checkOutPenaltyRate", spot.getCheckOutPenaltyRate());
        breakup.put("autoCompleted", isAutoCompleted);
        return breakup;
    }

    public void applyBreakupAndRefund(Bookings booking, ParkingSpot spot,
                                     BookingWalletService walletService) {
        if (!BookingStatus.COMPLETED.name().equalsIgnoreCase(booking.getStatus()) &&
            !BookingStatus.CANCELLED.name().equalsIgnoreCase(booking.getStatus())) {
            return;
        }

        Map<String, Object> breakup = calculateBookingBreakup(booking, spot);
        double refund = (double) breakup.get("refundAmount");

        if (refund > 0) {
            walletService.refundToWallet(booking.getUserId(), refund, "Booking refund");
        }
    }
}
