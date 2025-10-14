package com.parking.app.service.booking;

import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.model.Wallet;
import com.parking.app.repository.BookingRepository;
import com.parking.app.service.ParkingSpotService;
import com.parking.app.service.UserService;
import com.parking.app.service.WalletService;
import com.parking.app.util.BookingUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Service responsible for core booking lifecycle operations
 */
@Service
@Transactional
public class BookingLifecycleService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final ParkingSpotService parkingSpotService;
    private final BookingValidationService validationService;
    private final BookingWalletService bookingWalletService;
    private final BookingBreakupService breakupService;

    public BookingLifecycleService(BookingRepository bookingRepository,
                                  UserService userService,
                                  WalletService walletService,
                                  ParkingSpotService parkingSpotService,
                                  BookingValidationService validationService,
                                  BookingWalletService bookingWalletService,
                                  BookingBreakupService breakupService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.walletService = walletService;
        this.parkingSpotService = parkingSpotService;
        this.validationService = validationService;
        this.bookingWalletService = bookingWalletService;
        this.breakupService = breakupService;
    }

    public Bookings startBooking(String spotId, String userId, String lotId,
                                ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                String vehicleNumber) {
        BookingUtility.validateTimes(checkInTime, checkOutTime);
        BookingUtility.validateBookingWindow(checkInTime, checkOutTime);

        ParkingSpot spot = parkingSpotService.getParkingSpotById(spotId);
        if (spot == null) throw new NotFoundException("Parking spot not found");

        double amount = BookingUtility.calculateCharge(checkInTime, checkOutTime, spot.getBookingRate());

        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Wallet wallet = walletService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (!validationService.hasSufficientBalance(wallet, amount)) {
            throw new InsufficientFundsException();
        }

        validationService.ensureNoBookingOverlap(spotId, checkInTime, checkOutTime);
        parkingSpotService.decrementSpotAvailability(spotId);
        bookingWalletService.deductAndRecord(userId, amount, "Booking charge");

        return createAndSaveBooking(spotId, userId, lotId, checkInTime, checkOutTime, vehicleNumber, amount);
    }

    public Bookings confirmBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }
        booking.setStatus("pending");
        ZonedDateTime now = ZonedDateTime.now();
        booking.setCheckInTime(Date.from(now.toInstant()));
        return bookingRepository.save(booking);
    }

    public Bookings checkIn(String bookingId, String qrCode) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Only pending bookings can check in");
        }
        if (!bookingId.equals(qrCode)) {
            throw new java.lang.IllegalStateException("Invalid QR code for check-in");
        }

        validationService.validateNoActiveBookingForUser(booking.getUserId());

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        ZonedDateTime now = ZonedDateTime.now();
        booking.setStatus("active");
        booking.setCheckInTime(Date.from(now.toInstant()));
        booking.setActualCheckInTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);

        return bookingRepository.save(booking);
    }

    public Bookings checkOut(String bookingId, String qrCode) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Booking is not active");
        }
        if (!bookingId.equals(qrCode)) {
            throw new java.lang.IllegalStateException("Invalid QR code for checkout");
        }

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        ZonedDateTime now = ZonedDateTime.now();
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
            bookingWalletService.applyPenaltyToWallet(booking.getUserId(), totalPenalty,
                lateCheckInPenalty, lateCheckOutPenalty);
        }

        booking.setStatus("completed");
        booking.setCheckOutTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);
        booking.setAutoCompleted(false);

        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        breakupService.applyBreakupAndRefund(booking, spot, bookingWalletService);

        return booking;
    }

    public Bookings extendBooking(String bookingId, ZonedDateTime newCheckOutTime) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Only active bookings can be extended");
        }

        ZonedDateTime currentCheckOut = ZonedDateTime.ofInstant(
            booking.getCheckOutTime().toInstant(), newCheckOutTime.getZone());
        if (!newCheckOutTime.isAfter(currentCheckOut)) {
            throw new IllegalArgumentException("New check-out time must be after current check-out time");
        }

        validationService.ensureNoBookingOverlapForExtension(
            booking.getSpotId(), bookingId, currentCheckOut, newCheckOutTime);

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        double newAmount = BookingUtility.calculateCharge(
                ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), newCheckOutTime.getZone()),
                newCheckOutTime,
                spot.getBookingRate()
        );
        double diff = newAmount - booking.getAmount();

        if (diff > 0) {
            Wallet wallet = walletService.findByUserId(booking.getUserId())
                    .orElseThrow(() -> new NotFoundException("Wallet not found"));
            if (!validationService.hasSufficientBalance(wallet, diff)) {
                throw new InsufficientFundsException();
            }
            bookingWalletService.deductAndRecord(booking.getUserId(), diff, "Booking extension charge");
        }

        booking.setCheckOutTime(Date.from(newCheckOutTime.toInstant()));
        booking.setAmount(newAmount);
        return bookingRepository.save(booking);
    }

    public boolean cancelBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            return false;
        }

        if ("pending".equalsIgnoreCase(booking.getStatus()) ||
            "active".equalsIgnoreCase(booking.getStatus())) {
            bookingWalletService.refundToWallet(booking.getUserId(),
                booking.getAmount(), "Booking refund");
        }

        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot != null) {
            breakupService.applyBreakupAndRefund(booking, spot, bookingWalletService);
        }

        return true;
    }

    public Bookings updateBookingStatus(String id, String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        Bookings booking = findBookingOrThrow(id);
        String currentStatus = booking.getStatus();
        if ("completed".equalsIgnoreCase(currentStatus) ||
            "cancelled".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot update status of completed/cancelled booking");
        }
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public void deleteBooking(String id) {
        bookingRepository.deleteById(id);
    }

    private Bookings findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private Bookings createAndSaveBooking(String spotId, String userId, String lotId,
                                         ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                         String vehicleNumber, double amount) {
        Bookings booking = new Bookings();
        booking.setSpotId(spotId);
        booking.setUserId(userId);
        booking.setLotId(lotId);
        booking.setStatus("pending");
        booking.setAmount(amount);
        booking.setCreatedAt(Date.from(ZonedDateTime.now().toInstant()));
        booking.setCheckInTime(Date.from(checkInTime.toInstant()));
        booking.setCheckOutTime(Date.from(checkOutTime.toInstant()));
        booking.setVehicleNumber(vehicleNumber);
        booking.setQrCodeScanned(false);
        booking.setActualCheckInTime(null);
        booking.setAutoCompleted(false);
        return bookingRepository.save(booking);
    }
}

