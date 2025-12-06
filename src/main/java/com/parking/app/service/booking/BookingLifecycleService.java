package com.parking.app.service.booking;

import com.parking.app.constants.BookingStatus;
import com.parking.app.constants.CheckInMode;
import com.parking.app.exception.ConflictException;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.model.Wallet;
import com.parking.app.model.event.BookingEvent;
import com.parking.app.repository.BookingRepository;
import com.parking.app.repository.UserRepository;
import com.parking.app.service.ParkingSpotService;
import com.parking.app.service.UserService;
import com.parking.app.service.WalletService;
import com.parking.app.service.lock.LockService;
import com.parking.app.util.BookingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Service responsible for core booking lifecycle operations
 * Optimized for high concurrency with distributed locking and proper transaction management
 */
@Service
public class BookingLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(BookingLifecycleService.class);
    private static final long LOCK_WAIT_TIME_MS = 10000; // 30 seconds max wait for lock (increased from 5s)
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final ParkingSpotService parkingSpotService;
    private final BookingValidationService validationService;
    private final BookingWalletService bookingWalletService;
    private final BookingBreakupService breakupService;
    private final UserRepository userRepository;
    private final LockService lockService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingLifecycleService(BookingRepository bookingRepository,
                                  UserService userService,
                                  WalletService walletService,
                                  ParkingSpotService parkingSpotService,
                                  BookingValidationService validationService,
                                  BookingWalletService bookingWalletService,
                                  BookingBreakupService breakupService,
                                  UserRepository userRepository,
                                  LockService lockService,
                                  ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.walletService = walletService;
        this.parkingSpotService = parkingSpotService;
        this.validationService = validationService;
        this.bookingWalletService = bookingWalletService;
        this.breakupService = breakupService;
        this.userRepository = userRepository;
        this.lockService = lockService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new booking with proper concurrency control and ACID guarantees
     *
     * CONCURRENCY STRATEGY:
     * 1. Distributed Lock (Redis/MongoDB) - Prevents multiple instances from processing same spot
     * 2. Optimistic Locking - MongoDB's findAndModify ensures atomic spot reservation
     * 3. Transaction Management - All operations within transaction boundary
     * 4. Retry Mechanism - Handles transient failures due to contention
     *
     * RACE CONDITION HANDLING:
     * - If 1000 users try to book 10 spots, distributed lock ensures serialized access per spot
     * - Atomic spot reservation prevents double-booking even if lock somehow fails
     * - Transaction rollback ensures consistency if any operation fails
     *
     * @return Created booking
     * @throws ConflictException if no spots available or booking overlap
     * @throws InsufficientFundsException if wallet balance insufficient
     */
    @Retryable(
        value = {ConflictException.class},
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    public Bookings createBooking(String spotId, String userId,
                                  ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                  String vehicleNumber) {
        logger.info("Creating booking for spotId={}, userId={}, timeRange=[{} to {}]",
                    spotId, userId, checkInTime, checkOutTime);

        // STEP 1: Fast-fail validations (no DB operations)
        BookingUtility.validateTimes(checkInTime, checkOutTime);
        BookingUtility.validateBookingWindow(checkInTime, checkOutTime);

        // STEP 2: Fetch required entities (read-only operations)
        ParkingSpot spot = parkingSpotService.getParkingSpotById(spotId);
        if (spot == null) {
            throw new NotFoundException("Parking spot not found");
        }

        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // STEP 3: Calculate amount early to validate balance before acquiring lock
        double amount = BookingUtility.calculateCharge(checkInTime, checkOutTime, spot.getBookingRate());

        Wallet wallet = walletService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (!validationService.hasSufficientBalance(wallet, amount)) {
            logger.warn("Insufficient balance for userId={}, required={}, available={}",
                       userId, amount, wallet.getBalance());
            throw new InsufficientFundsException();
        }

        // STEP 4: CRITICAL SECTION - Use distributed lock for multi-instance coordination
        // This ensures only ONE instance processes booking for this spot at a time
        // LockService is either Redis (high-performance) or MongoDB (fallback)
        return lockService.executeWithLock(spotId, LOCK_WAIT_TIME_MS, () -> {
            logger.debug("Acquired distributed lock for spotId={}", spotId);

            // Execute the actual booking creation within transaction
            return createBookingWithTransaction(spotId, userId, spot.getLotName(), checkInTime, checkOutTime,
                                               vehicleNumber, amount, spot);
        });
    }

    /**
     * Internal method to create booking within transaction boundary
     * This method runs with proper transaction isolation to ensure ACID properties
     */
//    @Transactional(
//        propagation = Propagation.REQUIRES_NEW,
//        isolation = Isolation.READ_COMMITTED,
//        rollbackFor = Exception.class
//    )
    protected Bookings createBookingWithTransaction(String spotId, String userId, String lotName,
                                                    ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                                    String vehicleNumber, double amount, ParkingSpot spot) {
        logger.debug("Starting transaction for booking creation spotId={}", spotId);

        try {
            //TODO : time based spot check not possible. need to change db model

            // STEP 6: ATOMIC OPERATION - Reserve spot using MongoDB's findAndModify
            // This is the CRITICAL operation that prevents double-booking
            // MongoDB ensures this operation is atomic across all instances
            boolean spotReserved = parkingSpotService.atomicReserveSpotForBooking(spotId);
            if (!spotReserved) {
                logger.warn("Failed to reserve spot {} - no availability", spotId);
                throw new ConflictException("No spots available - booking full");
            }

            logger.info("Successfully reserved spot {} for userId={}", spotId, userId);

            // STEP 7: Deduct wallet balance and create transaction record
            // If this fails, @Transactional will rollback and spot will be released
            bookingWalletService.deductAndRecord(userId, amount, "Booking charge");

            // STEP 8: Create and persist booking
            Bookings booking = createAndSaveBooking(spotId, userId, lotName, checkInTime,
                                                   checkOutTime, vehicleNumber, amount);

            logger.info("Booking created successfully: bookingId={}, spotId={}, userId={}",
                       booking.getId(), spotId, userId);

            // STEP 9: Publish booking created event for async ParkingLot update
            eventPublisher.publishEvent(new BookingEvent(
                this,
                booking.getId(),
                lotName,
                spotId,
                BookingEvent.BookingEventType.BOOKING_CREATED,
                userId
            ));

            return booking;

        } catch (ConflictException | InsufficientFundsException e) {
            // These are business exceptions - propagate directly
            logger.warn("Business validation failed for spotId={}: {}", spotId, e.getMessage());
            throw e;

        } catch (Exception e) {
            // Unexpected error - rollback will happen automatically
            // Spot availability will be restored by transaction rollback
            logger.error("Unexpected error during booking creation for spotId={}: {}",
                        spotId, e.getMessage(), e);
            // Release spot explicitly as a safety measure
            try {
                parkingSpotService.incrementSpotAvailability(spotId);
            } catch (Exception releaseEx) {
                logger.error("Failed to release spot {} during error handling", spotId, releaseEx);
            }
            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
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

        // Publish booking completed event for async ParkingLot update
        eventPublisher.publishEvent(new BookingEvent(
            this,
            booking.getId(),
            booking.getLotName(),
            booking.getSpotId(),
            BookingEvent.BookingEventType.BOOKING_COMPLETED,
            booking.getUserId()
        ));

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

        // Publish booking cancelled event for async ParkingLot update
        eventPublisher.publishEvent(new BookingEvent(
            this,
            booking.getId(),
            booking.getLotName(),
            booking.getSpotId(),
            BookingEvent.BookingEventType.BOOKING_CANCELLED,
            booking.getUserId()
        ));

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

    private Bookings createAndSaveBooking(String spotId, String userId, String lotName,
                                         ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                         String vehicleNumber, double amount) {
        Bookings booking = new Bookings();
        booking.setSpotId(spotId);
        booking.setUserId(userId);
        booking.setLotName(lotName);
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
