package com.parking.app.service;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.*;
import com.parking.app.repository.BookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BookingService {

    private static final int NO_SHOW_MINUTES = 30;

    private final BookingRepository bookingRepository;
    private final MongoOperations mongoOperations;
    private final UserService userService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final ParkingSpotService parkingSpotService;

    public BookingService(
            BookingRepository bookingRepository,
            MongoOperations mongoOperations,
            UserService userService,
            WalletService walletService,
            TransactionService transactionService,
            ParkingSpotService parkingSpotService
    ) {
        this.bookingRepository = bookingRepository;
        this.mongoOperations = mongoOperations;
        this.userService = userService;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.parkingSpotService = parkingSpotService;
    }

    private Bookings findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private boolean hasSufficientBalance(Wallet wallet, double amount) {
        return wallet.getBalance() >= Math.ceil(amount);
    }

    private double calculatePenaltyWithGrace(ZonedDateTime scheduled, ZonedDateTime actual, double ratePerMinute) {
        long minutesLate = java.time.Duration.between(scheduled, actual).toMinutes();
        if (minutesLate <= 10) {
            return 0;
        }
        return (minutesLate - 10) * ratePerMinute;
    }

    private double calculateCharge(ZonedDateTime from, ZonedDateTime to, double bookingRate) {
        long hours = java.time.temporal.ChronoUnit.HOURS.between(from, to);
        if (from.plusHours(hours).isBefore(to)) {
            hours++;
        }
        return hours * bookingRate;
    }

    private void validateTimes(ZonedDateTime checkIn, ZonedDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out times are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    private void validateBookingWindow(ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
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

    public Bookings startBooking(String spotId, String userId, String lotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime, String vehicleNumber) {
        validateTimes(checkInTime, checkOutTime);
        validateBookingWindow(checkInTime, checkOutTime);

        ParkingSpot spot = parkingSpotService.getParkingSpotById(spotId);
        if (spot == null) throw new NotFoundException("Parking spot not found");

        double amount = calculateCharge(checkInTime, checkOutTime, spot.getBookingRate());

        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Wallet wallet = walletService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (!hasSufficientBalance(wallet, amount)) {
            throw new InsufficientFundsException();
        }

        ensureNoBookingOverlap(spotId, checkInTime, checkOutTime);
        parkingSpotService.decrementSpotAvailability(spotId);
        deductWalletBalance(wallet,amount);
        recordWalletTransaction(userId, -amount, "Booking charge");
        return createAndSaveBooking(spotId, userId, lotId, checkInTime, checkOutTime, vehicleNumber, amount);
    }

    private void ensureNoBookingOverlap(String spotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
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

    private void deductWalletBalance(Wallet wallet, double amount) {
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setLastUpdated(new Date());
        walletService.save(wallet);
    }

    private void recordWalletTransaction(String userId, double amount, String description) {
        transactionService.save(new Transactions(userId, amount, description, new Date()));
    }

    private Bookings createAndSaveBooking(String spotId, String userId, String lotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime, String vehicleNumber, double amount) {
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

        Query active = new Query(Criteria.where("userId").is(booking.getUserId()).and("status").is("active"));
        if (mongoOperations.exists(active, Bookings.class)) {
            throw new ConflictException("User already has an active booking");
        }

        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        double lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());

        booking.setStatus("active");
        booking.setCheckInTime(Date.from(now.toInstant()));
        booking.setActualCheckInTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);

        // No penalty is charged at check-in, only calculated and stored if needed
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
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
        ZonedDateTime actualCheckIn = booking.getActualCheckInTime() != null
                ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                : scheduledCheckIn;

        double lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, actualCheckIn, spot.getCheckInPenaltyRate());
        double lateCheckOutPenalty = calculatePenaltyWithGrace(scheduledEnd, now, spot.getCheckOutPenaltyRate());
        double totalPenalty = lateCheckInPenalty + lateCheckOutPenalty;

        if (totalPenalty > 0) {
            applyPenaltyToWalletAndTransactions(booking.getUserId(), totalPenalty, lateCheckInPenalty, lateCheckOutPenalty);
        }

        updateBookingForCheckout(booking, now);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        applyBreakupAndRefund(booking);
        return bookingRepository.save(booking);
    }

    private void applyPenaltyToWalletAndTransactions(String userId, double totalPenalty, double lateCheckInPenalty, double lateCheckOutPenalty) {
        Wallet wallet = walletService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (!hasSufficientBalance(wallet, totalPenalty)) {
            throw new InsufficientFundsException();
        }
        wallet.setBalance(wallet.getBalance() - totalPenalty);
        wallet.setLastUpdated(new Date());
        walletService.save(wallet);
        if (lateCheckInPenalty > 0) {
            transactionService.save(new Transactions(userId, -lateCheckInPenalty, "Late check-in penalty", new Date()));
        }
        if (lateCheckOutPenalty > 0) {
            transactionService.save(new Transactions(userId, -lateCheckOutPenalty, "Late check-out penalty", new Date()));
        }
    }

    private void updateBookingForCheckout(Bookings booking, ZonedDateTime now) {
        booking.setStatus("completed");
        booking.setCheckOutTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);
        booking.setActualCheckInTime(booking.getActualCheckInTime());
        booking.setAutoCompleted(false);
    }

    private void applyBreakupAndRefund(Bookings booking) {
        generateAndApplyBookingBreakup(booking);
    }

    public void autoCompleteLateBookings() {
        ZonedDateTime now = ZonedDateTime.now();
        Query query = new Query(Criteria.where("status").is("active")
                .and("checkOutTime").lte(Date.from(now.toInstant()))
                .and("qrCodeScanned").ne(true));
        List<Bookings> lateBookings = mongoOperations.find(query, Bookings.class);
        for (Bookings booking : lateBookings) {
            autoCompleteSingleLateBooking(booking, now);
        }
        // Auto-cancel no-shows (no refund)
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
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
        ZonedDateTime actualCheckIn = booking.getActualCheckInTime() != null
                ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                : scheduledCheckIn;
        double lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, actualCheckIn, spot.getCheckInPenaltyRate());
        double lateCheckOutPenalty = calculatePenaltyWithGrace(scheduledEnd, now, spot.getCheckOutPenaltyRate());
        double totalPenalty = lateCheckInPenalty + lateCheckOutPenalty;
        if (totalPenalty > 0) {
            applyPenaltyToWalletAndTransactions(booking.getUserId(), totalPenalty, lateCheckInPenalty, lateCheckOutPenalty);
        }
        booking.setStatus("completed");
        booking.setQrCodeScanned(false);
        booking.setAutoCompleted(true);
        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        applyBreakupAndRefund(booking);
    }

    private void autoCancelNoShowBooking(Bookings booking) {
        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        applyBreakupAndRefund(booking);
    }

    public List<Bookings> getAllBookingsFiltered(
            String status,
            String lotId,
            ZonedDateTime fromDate,
            ZonedDateTime toDate,
            int page,
            int size
    ) {
        Query query = new Query();
        if (status != null && !status.isEmpty()) query.addCriteria(Criteria.where("status").is(status));
        if (lotId != null && !lotId.isEmpty()) query.addCriteria(Criteria.where("lotId").is(lotId));
        if (fromDate != null) query.addCriteria(Criteria.where("checkInTime").gte(Date.from(fromDate.toInstant())));
        if (toDate != null) query.addCriteria(Criteria.where("checkOutTime").lte(Date.from(toDate.toInstant())));
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<Bookings> getBookingHistoryByUserId(String userId) {
        Date now = Date.from(ZonedDateTime.now().toInstant());
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("status").is("completed")
                        .and("checkOutTime").lt(now)
        );
        query.with(Sort.by(Sort.Direction.DESC, "checkOutTime"));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<String> getVehicleNumbersByUserId(String userId) {
        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getVehicleNumbers();
    }

    public List<Bookings> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Bookings getBookingById(String id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public Bookings extendBooking(String bookingId, ZonedDateTime newCheckOutTime) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Only active bookings can be extended");
        }
        ZonedDateTime currentCheckOut = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), newCheckOutTime.getZone());
        if (!newCheckOutTime.isAfter(currentCheckOut)) {
            throw new IllegalArgumentException("New check-out time must be after current check-out time");
        }
        Query overlapQuery = new Query(
                Criteria.where("spotId").is(booking.getSpotId())
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
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

        double newAmount = calculateCharge(
                ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), newCheckOutTime.getZone()),
                newCheckOutTime,
                spot.getBookingRate()
        );
        double diff = newAmount - booking.getAmount();
        if (diff > 0) {
            Wallet wallet = walletService.findByUserId(booking.getUserId())
                    .orElseThrow(() -> new NotFoundException("Wallet not found"));
            if (!hasSufficientBalance(wallet, diff)) {
                throw new InsufficientFundsException();
            }
            wallet.setBalance(wallet.getBalance() - diff);
            wallet.setLastUpdated(new Date());
            walletService.save(wallet);
            transactionService.save(new Transactions(booking.getUserId(), -diff, "Booking extension charge", new Date()));
        }
        booking.setCheckOutTime(Date.from(newCheckOutTime.toInstant()));
        booking.setAmount(newAmount);
        return bookingRepository.save(booking);
    }

    public Bookings updateBookingStatus(String id, String status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        Bookings booking = findBookingOrThrow(id);
        String currentStatus = booking.getStatus();
        if ("completed".equalsIgnoreCase(currentStatus) || "cancelled".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot update status of completed/cancelled booking");
        }
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public void deleteBooking(String id) {
        bookingRepository.deleteById(id);
    }

    public List<Bookings> getBookingsByUserId(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    public QrValidationResult validateQrCodeForCheckIn(String bookingId, String qrCode) {
        Bookings booking = getBookingById(bookingId);
        if (booking == null) return new QrValidationResult(false, 0, "Booking not found");
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            return new QrValidationResult(false, 0, "Booking is not pending");
        }
        if (!bookingId.equals(qrCode)) {
            return new QrValidationResult(false, 0, "Invalid QR code for check-in");
        }
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) return new QrValidationResult(false, 0, "Parking spot not found");
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        double penalty = calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());

        if (penalty > 0) {
            return new QrValidationResult(true, penalty, "Penalty applies for late check-in");
        }
        return new QrValidationResult(true, 0, "QR code valid for check-in");
    }

    public QrValidationResult validateQrCodeForCheckOut(String bookingId, String qrCode) {
        Bookings booking = getBookingById(bookingId);
        if (booking == null) return new QrValidationResult(false, 0, "Booking not found");
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            return new QrValidationResult(false, 0, "Booking is not active");
        }
        if (!bookingId.equals(qrCode)) {
            return new QrValidationResult(false, 0, "Invalid QR code for checkout");
        }
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) return new QrValidationResult(false, 0, "Parking spot not found");
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
        double penalty = calculatePenaltyWithGrace(scheduledEnd, now, spot.getCheckOutPenaltyRate());

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

    public boolean isQrCodeValid(String bookingId) {
        Bookings booking = getBookingById(bookingId);
        if (booking == null) return false;
        Date now = new Date();
        Date checkIn = booking.getCheckInTime();
        Date checkOut = booking.getCheckOutTime();
        return checkIn != null && checkOut != null && !now.before(checkIn) && !now.after(checkOut);
    }

    // Helper: Generate breakup and apply subtotal/refund to wallet
    private void generateAndApplyBookingBreakup(Bookings booking) {
        if (!"completed".equalsIgnoreCase(booking.getStatus()) && !"cancelled".equalsIgnoreCase(booking.getStatus())) {
            return;
        }
        Map<String, Object> breakup = getBookingBreakupInternal(booking);
        double subtotal = (double) breakup.get("subtotal");
        double refund = (double) breakup.get("refundAmount");
        double totalDeducted = (double) breakup.get("totalDeducted");

        Wallet wallet = walletService.findByUserId(booking.getUserId()).orElse(null);
        if (wallet != null) {
            // Apply refund if any
            if (refund > 0) {
                wallet.setBalance(wallet.getBalance() + refund);
                wallet.setLastUpdated(new Date());
                walletService.save(wallet);
                transactionService.save(new Transactions(booking.getUserId(), refund, "Booking refund", new Date()));
            }
            // If subtotal is positive and not already deducted, deduct it (should not happen if already deducted at booking/penalty time)
            // If subtotal is negative (should not happen), add to wallet
        }
    }

    // Returns a breakup of charges, penalties, subtotal, and refund for a booking
    public Map<String, Object> getBookingBreakup(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"completed".equalsIgnoreCase(booking.getStatus()) && !"cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Breakup only available for checked-out or cancelled bookings");
        }
        return getBookingBreakupInternal(booking);
    }

    private Map<String, Object> getBookingBreakupInternal(Bookings booking) {
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) throw new NotFoundException("Parking spot not found");

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

        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            // Check if auto-cancelled (no-show)
            boolean isAutoCancelled = actualCheckIn == null && now.isAfter(scheduledCheckIn);
            if (isAutoCancelled) {
                // No refund, apply check-in penalty
                lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());
                refund = 0.0;
            } else if (now.isAfter(scheduledCheckIn)) {
                // User cancelled after check-in time, refund booking charge, apply check-in penalty
                lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, now, spot.getCheckInPenaltyRate());
                refund = booking.getAmount();
            } else {
                // User cancelled before check-in time, full refund
                refund = booking.getAmount();
            }
        } else if ("completed".equalsIgnoreCase(booking.getStatus())) {
            ZonedDateTime actualCheckOut = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
            lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, actualCheckIn, spot.getCheckInPenaltyRate());
            lateCheckOutPenalty = calculatePenaltyWithGrace(scheduledEnd, actualCheckOut, spot.getCheckOutPenaltyRate());
            // Refund logic: refund booking charge unless auto-completed
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

    public List<Bookings> findByLotIdAndTimeWindow(String lotId, ZonedDateTime startTime, ZonedDateTime endTime) {
        return bookingRepository.findByLotIdAndTimeWindow(lotId, startTime, endTime);
    }

    public boolean cancelBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            return false;
        }
        if ("pending".equalsIgnoreCase(booking.getStatus()) || "active".equalsIgnoreCase(booking.getStatus())) {
            walletService.refundWalletAndRecordTransaction(booking);
            transactionService.save(new Transactions(booking.getUserId(), booking.getAmount(), "Booking refund", new Date()));
        }
        setBookingCancelled(booking);
        parkingSpotService.incrementSpotAvailability(booking.getSpotId());
        applyBreakupAndRefund(booking);
        return true;
    }

    private void setBookingCancelled(Bookings booking) {
        booking.setStatus("cancelled");
        bookingRepository.save(booking);
    }
}
