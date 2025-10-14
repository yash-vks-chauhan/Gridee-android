package com.parking.app.service.booking;

import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.repository.BookingRepository;
import com.parking.app.service.ParkingSpotService;
import com.parking.app.util.BookingUtility;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Service responsible for auto-completion of bookings and no-show handling
 */
@Service
public class BookingAutoCompletionService {

    private static final int NO_SHOW_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final MongoOperations mongoOperations;
    private final ParkingSpotService parkingSpotService;
    private final BookingWalletService walletService;
    private final BookingBreakupService breakupService;

    public BookingAutoCompletionService(BookingRepository bookingRepository,
                                        MongoOperations mongoOperations,
                                        ParkingSpotService parkingSpotService,
                                        BookingWalletService walletService,
                                        BookingBreakupService breakupService) {
        this.bookingRepository = bookingRepository;
        this.mongoOperations = mongoOperations;
        this.parkingSpotService = parkingSpotService;
        this.walletService = walletService;
        this.breakupService = breakupService;
    }

    public void autoCompleteLateBookings() {
        ZonedDateTime now = ZonedDateTime.now();

        // Auto-complete late active bookings
        Query lateQuery = new Query(Criteria.where("status").is("active")
                .and("checkOutTime").lte(Date.from(now.toInstant()))
                .and("qrCodeScanned").ne(true));
        List<Bookings> lateBookings = mongoOperations.find(lateQuery, Bookings.class);

        for (Bookings booking : lateBookings) {
            autoCompleteSingleLateBooking(booking, now);
        }

        // Auto-cancel no-shows
        Query noShowQuery = new Query(Criteria.where("status").is("pending")
                .and("checkInTime").lt(Date.from(now.minusMinutes(NO_SHOW_MINUTES).toInstant())));
        List<Bookings> noShowBookings = mongoOperations.find(noShowQuery, Bookings.class);

        for (Bookings booking : noShowBookings) {
            autoCancelNoShowBooking(booking);
        }
    }

    private void autoCompleteSingleLateBooking(Bookings booking, ZonedDateTime now) {
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) return;

        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(
                booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(
                booking.getCheckOutTime().toInstant(), now.getZone());
        ZonedDateTime actualCheckIn = booking.getActualCheckInTime() != null
                ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                : scheduledCheckIn;

        double lateCheckInPenalty = BookingUtility.calculatePenaltyWithGrace(
                scheduledCheckIn, actualCheckIn, spot.getCheckInPenaltyRate());
        double lateCheckOutPenalty = BookingUtility.calculatePenaltyWithGrace(
                scheduledEnd, now, spot.getCheckOutPenaltyRate());
        double totalPenalty = lateCheckInPenalty + lateCheckOutPenalty;

        if (totalPenalty > 0) {
            walletService.applyPenaltyToWallet(booking.getUserId(), totalPenalty,
                    lateCheckInPenalty, lateCheckOutPenalty);
        }

        booking.setStatus("completed");
        booking.setQrCodeScanned(false);
        booking.setAutoCompleted(true);
        bookingRepository.save(booking);

        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        breakupService.applyBreakupAndRefund(booking, spot, walletService);
    }

    private void autoCancelNoShowBooking(Bookings booking) {
        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot != null) {
            breakupService.applyBreakupAndRefund(booking, spot, walletService);
        }
    }
}