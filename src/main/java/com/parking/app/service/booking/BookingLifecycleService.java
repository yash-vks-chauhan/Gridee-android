package com.parking.app.service.booking;

import com.parking.app.constants.BookingStatus;
import com.parking.app.constants.CheckInMode;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.model.Wallet;
import com.parking.app.repository.BookingRepository;
import com.parking.app.repository.UserRepository;
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
    private final UserRepository userRepository;

    public BookingLifecycleService(BookingRepository bookingRepository,
                                  UserService userService,
                                  WalletService walletService,
                                  ParkingSpotService parkingSpotService,
                                  BookingValidationService validationService,
                                  BookingWalletService bookingWalletService,
                                  BookingBreakupService breakupService,
                                  UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.walletService = walletService;
        this.parkingSpotService = parkingSpotService;
        this.validationService = validationService;
        this.bookingWalletService = bookingWalletService;
        this.breakupService = breakupService;
        this.userRepository = userRepository;
    }

    public Bookings createBooking(String spotId, String userId, String lotId,
                                  ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                  String vehicleNumber) {
        //TODO : handle transaction in the logic
        ParkingSpot spot = parkingSpotService.getParkingSpotById(spotId);
        if (spot == null) throw new NotFoundException("Parking spot not found");

        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Wallet wallet = walletService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        BookingUtility.validateTimes(checkInTime, checkOutTime);
        BookingUtility.validateBookingWindow(checkInTime, checkOutTime);

        double amount = BookingUtility.calculateCharge(checkInTime, checkOutTime, spot.getBookingRate());

        if (!validationService.hasSufficientBalance(wallet, amount)) {
            throw new InsufficientFundsException();
        }
        //TODO : check if spot update,decrement and booking creation should be atomic
        validationService.ensureNoBookingOverlap(spotId, checkInTime, checkOutTime);
        parkingSpotService.decrementSpotAvailability(spotId);
        bookingWalletService.deductAndRecord(userId, amount, "Booking charge");

        return createAndSaveBooking(spotId, userId, lotId, checkInTime, checkOutTime, vehicleNumber, amount);
    }


    private Bookings findBookingByAuthMode(String bookingId, CheckInMode mode, String vehicleNumber,
                                           String pin, String requiredStatus) {
        // If bookingId is provided (QR_CODE mode), use direct lookup - fastest path
        if (bookingId != null && !bookingId.trim().isEmpty()) {
            return findBookingOrThrow(bookingId);
        }

        // For VEHICLE_NUMBER and PIN modes, we need to find the booking
        if (mode == null) {
            throw new IllegalArgumentException("mode is required when bookingId is not provided");
        }

        switch (mode) {
            case VEHICLE_NUMBER:
                if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
                    throw new IllegalArgumentException("Vehicle number is required for VEHICLE_NUMBER mode");
                }
                // Optimized query: finds booking by vehicle number using index
                return bookingRepository.findByVehicleNumberAndStatus(vehicleNumber.trim(), requiredStatus)
                        .orElseThrow(() -> new NotFoundException("No " + requiredStatus.toLowerCase() +
                                " booking found for vehicle number: " + vehicleNumber));

            case PIN:
                if (pin == null || pin.trim().isEmpty()) {
                    throw new IllegalArgumentException("PIN is required for PIN mode");
                }
                // Optimized: direct PIN lookup using index, then find booking by userId
                Users user = userRepository.findByCheckInPin(pin.trim())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid PIN"));

                return bookingRepository.findByUserIdAndStatus(user.getId(), requiredStatus)
                        .orElseThrow(() -> new NotFoundException("No " + requiredStatus.toLowerCase() +
                                " booking found for user"));

            case QR_CODE:
                throw new IllegalArgumentException("BookingId must be provided for QR_CODE mode");
            default:
                throw new IllegalArgumentException("Invalid check-in mode");
        }
    }

    /**
     * Check-in with multiple authentication methods: QR code, vehicle number, or PIN
     * @param checkInOperatorId - ID of the operator performing the check-in (null for user self check-in)
     */
    public Bookings checkIn(String bookingId, CheckInMode mode, String qrCode, String vehicleNumber, String pin, String checkInOperatorId) {
        // Efficiently find booking based on mode - uses indexed queries for speed
        // The findBookingByAuthMode method already validates the authentication credentials
        Bookings booking = findBookingByAuthMode(bookingId, mode, vehicleNumber, pin, BookingStatus.PENDING.name());

        if (!BookingStatus.PENDING.name().equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only pending bookings can check in");
        }

        // Single query to validate no active bookings
        validationService.validateNoActiveBookingForUser(booking.getUserId());

        // Get spot details
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        // Update booking status to ACTIVE
        ZonedDateTime now = ZonedDateTime.now();
        booking.setStatus(BookingStatus.ACTIVE.name());
        booking.setCheckInTime(Date.from(now.toInstant()));
        booking.setActualCheckInTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);
        booking.setCheckInOperatorId(checkInOperatorId);  // Store operator ID who performed check-in

        return bookingRepository.save(booking);
    }

    /**
     * Check-out with multiple authentication methods: QR code, vehicle number, or PIN
     * @param checkOutOperatorId - ID of the operator performing the check-out (null for user self check-out)
     */
    public Bookings checkOut(String bookingId, CheckInMode mode, String qrCode, String vehicleNumber, String pin, String checkOutOperatorId) {
        // Efficiently find booking based on mode
        // The findBookingByAuthMode method already validates the authentication credentials
        Bookings booking = findBookingByAuthMode(bookingId, mode, vehicleNumber, pin, BookingStatus.ACTIVE.name());

        if (!BookingStatus.ACTIVE.name().equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is not active");
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

        booking.setStatus(BookingStatus.COMPLETED.name());
        booking.setCheckOutTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);
        booking.setAutoCompleted(false);
        booking.setCheckOutOperatorId(checkOutOperatorId);  // Store operator ID who performed check-out

        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        breakupService.applyBreakupAndRefund(booking, spot, bookingWalletService);

        return booking;
    }

    public Bookings extendBooking(String bookingId, ZonedDateTime newCheckOutTime) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!BookingStatus.ACTIVE.name().equalsIgnoreCase(booking.getStatus())) {
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
        if (BookingStatus.CANCELLED.name().equalsIgnoreCase(booking.getStatus())) {
            return false;
        }

        if (BookingStatus.PENDING.name().equalsIgnoreCase(booking.getStatus()) ||
            BookingStatus.ACTIVE.name().equalsIgnoreCase(booking.getStatus())) {
            bookingWalletService.refundToWallet(booking.getUserId(),
                booking.getAmount(), "Booking refund");
        }

        booking.setStatus(BookingStatus.CANCELLED.name());
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
        if (BookingStatus.COMPLETED.name().equalsIgnoreCase(currentStatus) ||
            BookingStatus.CANCELLED.name().equalsIgnoreCase(currentStatus)) {
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
        booking.setStatus(BookingStatus.PENDING.name());
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
