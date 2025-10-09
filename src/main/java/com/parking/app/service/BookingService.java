//package com.parking.app.service;
//
//import com.parking.app.exception.ConflictException;
//import com.parking.app.exception.InsufficientFundsException;
//import com.parking.app.exception.NotFoundException;
//import com.parking.app.exception.IllegalStateException;
//import com.parking.app.model.Bookings;
//import com.parking.app.model.ParkingSpot;
//import com.parking.app.model.Users;
//import com.parking.app.model.Wallet;
//import com.parking.app.model.Transactions;
//import com.parking.app.repository.BookingRepository;
//import com.parking.app.repository.UserRepository;
//import com.parking.app.repository.WalletRepository;
//import com.parking.app.repository.TransactionsRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.mongodb.core.MongoOperations;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.ZonedDateTime;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.Date;
//import java.util.List;
//
//@Service
//@Transactional
//public class BookingService {
//
//    private static final double HOURLY_RATE = 5.0;
//    private static final double PENALTY_RATE = 10.0;
//
//    private final BookingRepository bookingRepository;
//    private final UserRepository userRepository;
//    private final WalletRepository walletRepository;
//    private final TransactionsRepository transactionsRepository;
//    private final MongoOperations mongoOperations;
//
//    @Autowired
//    public BookingService(
//            BookingRepository bookingRepository,
//            UserRepository userRepository,
//            WalletRepository walletRepository,
//            TransactionsRepository transactionsRepository,
//            MongoOperations mongoOperations
//    ) {
//        this.bookingRepository = bookingRepository;
//        this.userRepository = userRepository;
//        this.walletRepository = walletRepository;
//        this.transactionsRepository = transactionsRepository;
//        this.mongoOperations = mongoOperations;
//    }
//
//    private Bookings findBookingOrThrow(String bookingId) {
//        return bookingRepository.findById(bookingId)
//                .orElseThrow(() -> new NotFoundException("Booking not found"));
//    }
//
//    private boolean hasSufficientBalance(Wallet wallet, double amount) {
//        return wallet.getBalance() >= Math.ceil(amount);
//    }
//
//    private double calculatePenaltyWithGrace(ZonedDateTime scheduled, ZonedDateTime actual) {
//        long minutesLate = java.time.Duration.between(scheduled, actual).toMinutes();
//        if (minutesLate <= 10) {
//            return 0;
//        }
//        long penaltyHours = ((minutesLate - 11) / 60) + 1;
//        return penaltyHours * PENALTY_RATE;
//    }
//
//    private double calculateCharge(ZonedDateTime from, ZonedDateTime to) {
//        long hours = java.time.temporal.ChronoUnit.HOURS.between(from, to);
//        if (from.plusHours(hours).isBefore(to)) {
//            hours++;
//        }
//        return hours * HOURLY_RATE;
//    }
//
//    private void validateTimes(ZonedDateTime checkIn, ZonedDateTime checkOut) {
//        if (checkIn == null || checkOut == null) {
//            throw new IllegalArgumentException("Check-in and check-out times are required");
//        }
//        if (!checkOut.isAfter(checkIn)) {
//            throw new IllegalArgumentException("Check-out must be after check-in");
//        }
//    }
//
//    private void validateBookingWindow(ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
//        LocalDate today = ZonedDateTime.now().toLocalDate();
//        LocalTime nowTime = ZonedDateTime.now().toLocalTime();
//        LocalTime cutoff = LocalTime.of(20, 0);
//
//        if (nowTime.isBefore(cutoff)) {
//            if (!checkInTime.toLocalDate().isEqual(today) || checkOutTime.toLocalTime().isAfter(cutoff)) {
//                throw new ConflictException("Bookings before 8pm must be for today and end by 8pm");
//            }
//        } else {
//            if (!checkInTime.toLocalDate().isEqual(today.plusDays(1))) {
//                throw new ConflictException("Bookings after 8pm must be for tomorrow");
//            }
//        }
//    }
//
//    // --- CHANGED: Removed active booking check here ---
//    public Bookings startBooking(String spotId, String userId, String lotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime, String vehicleNumber) {
//        validateTimes(checkInTime, checkOutTime);
//        validateBookingWindow(checkInTime, checkOutTime);
//
//        double amount = calculateCharge(checkInTime, checkOutTime);
//
//        Users user = userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException("User not found"));
//
//        Wallet wallet = walletRepository.findByUserId(userId)
//                .orElseThrow(() -> new NotFoundException("Wallet not found"));
//
//        if (!hasSufficientBalance(wallet, amount)) {
//            throw new InsufficientFundsException();
//        }
//
//        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
//        Update decUpdate = new Update().inc("available", -1);
//
//        ParkingSpot updatedSpot = mongoOperations.findAndModify(
//                spotQuery, decUpdate, ParkingSpot.class);
//
//        if (updatedSpot == null) {
//            throw new ConflictException("No spots available");
//        }
//
//        wallet.setBalance(wallet.getBalance() - Math.ceil(amount));
//        wallet.setLastUpdated(new Date());
//        walletRepository.save(wallet);
//
//        transactionsRepository.save(new Transactions(userId, -amount, "Booking charge", new Date()));
//
//        Bookings booking = new Bookings();
//        booking.setSpotId(spotId);
//        booking.setUserId(userId);
//        booking.setLotId(lotId);
//        booking.setStatus("pending");
//        booking.setAmount(amount);
//        booking.setCreatedAt(Date.from(ZonedDateTime.now().toInstant()));
//        booking.setCheckInTime(Date.from(checkInTime.toInstant()));
//        booking.setCheckOutTime(Date.from(checkOutTime.toInstant()));
//        booking.setVehicleNumber(vehicleNumber);
//        booking.setQrCodeScanned(false);
//        return bookingRepository.save(booking);
//    }
//    private void ensureAvailableNotExceedCapacity(String spotId) {
//        ParkingSpot spot = mongoOperations.findById(spotId, ParkingSpot.class);
//        if (spot != null && spot.getAvailable() > spot.getCapacity()) {
//            spot.setAvailable(spot.getCapacity());
//            mongoOperations.save(spot);
//        }
//    }
//
//
//    public Bookings confirmBooking(String bookingId) {
//        Bookings booking = findBookingOrThrow(bookingId);
//        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
//            throw new IllegalStateException("Only pending bookings can be confirmed");
//        }
//        booking.setStatus("pending");
//        ZonedDateTime now = ZonedDateTime.now();
//        booking.setCheckInTime(Date.from(now.toInstant()));
//        return bookingRepository.save(booking);
//    }
//
//    // --- CHANGED: Active booking check moved here ---
//    public Bookings checkIn(String bookingId, String qrCode) {
//        Bookings booking = findBookingOrThrow(bookingId);
//        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
//            throw new IllegalStateException("Only pending bookings can check in");
//        }
//        if (!bookingId.equals(qrCode)) {
//            throw new IllegalStateException("Invalid QR code for check-in");
//        }
//
//        // Only one active booking per user at a time
//        Query active = new Query(Criteria.where("userId").is(booking.getUserId()).and("status").is("active"));
//        if (mongoOperations.exists(active, Bookings.class)) {
//            throw new ConflictException("User already has an active booking");
//        }
//
//        ZonedDateTime now = ZonedDateTime.now();
//        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
//        double penalty = calculatePenaltyWithGrace(scheduledCheckIn, now);
//
//        if (penalty > 0) {
//            Wallet wallet = walletRepository.findByUserId(booking.getUserId())
//                    .orElseThrow(() -> new NotFoundException("Wallet not found"));
//            if (!hasSufficientBalance(wallet, penalty)) {
//                throw new InsufficientFundsException();
//            }
//            wallet.setBalance(wallet.getBalance() - penalty);
//            wallet.setLastUpdated(new Date());
//            walletRepository.save(wallet);
//            transactionsRepository.save(new Transactions(booking.getUserId(), -penalty, "Late check-in penalty", new Date()));
//        }
//
//        booking.setStatus("active");
//        booking.setCheckInTime(Date.from(now.toInstant()));
//        booking.setQrCodeScanned(true);
//        return bookingRepository.save(booking);
//    }
//
//    public Bookings checkOut(String bookingId, String qrCode) {
//        Bookings booking = findBookingOrThrow(bookingId);
//        if (!"active".equalsIgnoreCase(booking.getStatus())) {
//            throw new IllegalStateException("Booking is not active");
//        }
//        if (!bookingId.equals(qrCode)) {
//            throw new IllegalStateException("Invalid QR code for checkout");
//        }
//
//        ZonedDateTime now = ZonedDateTime.now();
//        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
//        double penalty = calculatePenaltyWithGrace(scheduledEnd, now);
//
//        Wallet wallet = walletRepository.findByUserId(booking.getUserId())
//                .orElseThrow(() -> new NotFoundException("Wallet not found"));
//
//        if (penalty > 0) {
//            if (!hasSufficientBalance(wallet, penalty)) {
//                throw new InsufficientFundsException();
//            }
//            wallet.setBalance(wallet.getBalance() - penalty);
//            wallet.setLastUpdated(new Date());
//            walletRepository.save(wallet);
//            transactionsRepository.save(new Transactions(booking.getUserId(), -penalty, "Check-out penalty", new Date()));
//        }
//
//        double refundAmount = Math.ceil(booking.getAmount());
//        wallet.setBalance(wallet.getBalance() + refundAmount);
//        wallet.setLastUpdated(new Date());
//        walletRepository.save(wallet);
//        transactionsRepository.save(new Transactions(booking.getUserId(), refundAmount, "Booking charge refund", new Date()));
//
//        booking.setStatus("completed");
//        booking.setCheckOutTime(Date.from(now.toInstant()));
//        booking.setQrCodeScanned(true);
//
//        Update incUpdate = new Update().inc("available", 1);
//        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
//        ensureAvailableNotExceedCapacity(booking.getSpotId());
//        return bookingRepository.save(booking);
//    }
//
//    @Scheduled(cron = "0 * * * * *")
//    public void autoCompleteLateBookings() {
//        ZonedDateTime now = ZonedDateTime.now();
//        Query query = new Query(Criteria.where("status").is("active")
//                .and("checkOutTime").lte(Date.from(now.toInstant()))
//                .and("qrCodeScanned").ne(true));
//        List<Bookings> lateBookings = mongoOperations.find(query, Bookings.class);
//        for (Bookings booking : lateBookings) {
//            ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
//            double penalty = calculatePenaltyWithGrace(scheduledEnd, now);
//
//            if (penalty > 0) {
//                Wallet wallet = walletRepository.findByUserId(booking.getUserId()).orElse(null);
//                if (wallet != null && hasSufficientBalance(wallet, penalty)) {
//                    wallet.setBalance(wallet.getBalance() - penalty);
//                    wallet.setLastUpdated(new Date());
//                    walletRepository.save(wallet);
//                    transactionsRepository.save(new Transactions(booking.getUserId(), -penalty, "Auto-complete penalty", new Date()));
//                }
//            }
//            booking.setStatus("completed");
//            booking.setQrCodeScanned(false);
//            bookingRepository.save(booking);
//
//            Update incUpdate = new Update().inc("available", 1);
//            mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
//            ensureAvailableNotExceedCapacity(booking.getSpotId());
//        }
//    }
//
//    public boolean cancelBooking(String bookingId) {
//        Bookings booking = findBookingOrThrow(bookingId);
//        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
//            return false;
//        }
//
//        Update incUpdate = new Update().inc("available", 1);
//        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
//        ensureAvailableNotExceedCapacity(booking.getSpotId());
//        booking.setStatus("cancelled");
//        bookingRepository.save(booking);
//        return true;
//    }
//
//    @Scheduled(cron = "0 0 20 * * *")
//    public void resetParkingSpotsAvailability() {
//        List<ParkingSpot> spots = mongoOperations.findAll(ParkingSpot.class);
//        for (ParkingSpot spot : spots) {
//            spot.setAvailable(spot.getCapacity());
//            mongoOperations.save(spot);
//        }
//    }
//
//    public List<Bookings> getAllBookingsFiltered(
//            String status,
//            String lotId,
//            ZonedDateTime fromDate,
//            ZonedDateTime toDate,
//            int page,
//            int size
//    ) {
//        Query query = new Query();
//        if (status != null && !status.isEmpty()) query.addCriteria(Criteria.where("status").is(status));
//        if (lotId != null && !lotId.isEmpty()) query.addCriteria(Criteria.where("lotId").is(lotId));
//        if (fromDate != null) query.addCriteria(Criteria.where("checkInTime").gte(Date.from(fromDate.toInstant())));
//        if (toDate != null) query.addCriteria(Criteria.where("checkOutTime").lte(Date.from(toDate.toInstant())));
//        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
//        return mongoOperations.find(query, Bookings.class);
//    }
//
//    public List<Bookings> getBookingHistoryByUserId(String userId) {
//        Date now = Date.from(ZonedDateTime.now().toInstant());
//        Query query = new Query(
//                Criteria.where("userId").is(userId)
//                        .and("status").is("completed")
//                        .and("checkOutTime").lt(now)
//        );
//        query.with(Sort.by(Sort.Direction.DESC, "checkOutTime"));
//        return mongoOperations.find(query, Bookings.class);
//    }
//
//    public List<String> getVehicleNumbersByUserId(String userId) {
//        Users user = userRepository.findById(userId)
//                .orElseThrow(() -> new NotFoundException("User not found"));
//        return user.getVehicleNumbers();
//    }
//
//    public List<Bookings> getAllBookings() {
//        return bookingRepository.findAll();
//    }
//
//    public Bookings getBookingById(String id) {
//        return bookingRepository.findById(id).orElse(null);
//    }
//
//    public Bookings updateBookingStatus(String id, String status) {
//        if (status == null) {
//            throw new IllegalArgumentException("Status cannot be null");
//        }
//        Bookings booking = findBookingOrThrow(id);
//        String currentStatus = booking.getStatus();
//        if ("completed".equalsIgnoreCase(currentStatus) || "cancelled".equalsIgnoreCase(currentStatus)) {
//            throw new IllegalStateException("Cannot update status of completed/cancelled booking");
//        }
//        booking.setStatus(status);
//        return bookingRepository.save(booking);
//    }
//
//    public void deleteBooking(String id) {
//        bookingRepository.deleteById(id);
//    }
//
//    public List<Bookings> getBookingsByUserId(String userId) {
//        return bookingRepository.findByUserId(userId);
//    }
//
//    public QrValidationResult validateQrCodeForCheckIn(String bookingId, String qrCode) {
//        Bookings booking = getBookingById(bookingId);
//        if (booking == null) return new QrValidationResult(false, 0, "Booking not found");
//        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
//            return new QrValidationResult(false, 0, "Booking is not pending");
//        }
//        if (!bookingId.equals(qrCode)) {
//            return new QrValidationResult(false, 0, "Invalid QR code for check-in");
//        }
//        ZonedDateTime now = ZonedDateTime.now();
//        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
//        double penalty = calculatePenaltyWithGrace(scheduledCheckIn, now);
//
//        if (penalty > 0) {
//            Wallet wallet = walletRepository.findByUserId(booking.getUserId()).orElse(null);
//            if (wallet != null && hasSufficientBalance(wallet, penalty)) {
//                wallet.setBalance(wallet.getBalance() - penalty);
//                wallet.setLastUpdated(new Date());
//                walletRepository.save(wallet);
//                transactionsRepository.save(new Transactions(booking.getUserId(), -penalty, "Late check-in penalty", new Date()));
//            }
//            return new QrValidationResult(true, penalty, "Penalty applies for late check-in");
//        }
//        return new QrValidationResult(true, 0, "QR code valid for check-in");
//    }
//
//    public QrValidationResult validateQrCodeForCheckOut(String bookingId, String qrCode) {
//        Bookings booking = getBookingById(bookingId);
//        if (booking == null) return new QrValidationResult(false, 0, "Booking not found");
//        if (!"active".equalsIgnoreCase(booking.getStatus())) {
//            return new QrValidationResult(false, 0, "Booking is not active");
//        }
//        if (!bookingId.equals(qrCode)) {
//            return new QrValidationResult(false, 0, "Invalid QR code for checkout");
//        }
//        ZonedDateTime now = ZonedDateTime.now();
//        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
//        double penalty = calculatePenaltyWithGrace(scheduledEnd, now);
//
//        if (penalty > 0) {
//            return new QrValidationResult(true, penalty, "Penalty applies for late check-out");
//        }
//        return new QrValidationResult(true, 0, "QR code valid for checkout");
//    }
//
//
//    public static class QrValidationResult {
//        public boolean valid;
//        public double penalty;
//        public String message;
//        public QrValidationResult(boolean valid, double penalty, String message) {
//            this.valid = valid;
//            this.penalty = penalty;
//            this.message = message;
//        }
//    }
//
//    public boolean isQrCodeValid(String bookingId) {
//        Bookings booking = getBookingById(bookingId);
//        if (booking == null) return false;
//        Date now = new Date();
//        Date checkIn = booking.getCheckInTime();
//        Date checkOut = booking.getCheckOutTime();
//        return checkIn != null && checkOut != null && !now.before(checkIn) && !now.after(checkOut);
//    }
//}


package com.parking.app.service;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.model.Wallet;
import com.parking.app.model.Transactions;
import com.parking.app.repository.BookingRepository;
import com.parking.app.repository.UserRepository;
import com.parking.app.repository.WalletRepository;
import com.parking.app.repository.TransactionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private static final double HOURLY_RATE = 5.0;
    private static final double PENALTY_RATE = 10.0;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionsRepository transactionsRepository;
    private final MongoOperations mongoOperations;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionsRepository transactionsRepository,
            MongoOperations mongoOperations
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionsRepository = transactionsRepository;
        this.mongoOperations = mongoOperations;
    }

    private Bookings findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private boolean hasSufficientBalance(Wallet wallet, double amount) {
        return wallet.getBalance() >= Math.ceil(amount);
    }

    private double calculatePenaltyWithGrace(ZonedDateTime scheduled, ZonedDateTime actual) {
        long minutesLate = java.time.Duration.between(scheduled, actual).toMinutes();
        if (minutesLate <= 10) {
            return 0;
        }
        long penaltyHours = ((minutesLate - 11) / 60) + 1;
        return penaltyHours * PENALTY_RATE;
    }

    private double calculateCharge(ZonedDateTime from, ZonedDateTime to) {
        long hours = java.time.temporal.ChronoUnit.HOURS.between(from, to);
        if (from.plusHours(hours).isBefore(to)) {
            hours++;
        }
        return hours * HOURLY_RATE;
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

        double amount = calculateCharge(checkInTime, checkOutTime);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (!hasSufficientBalance(wallet, amount)) {
            throw new InsufficientFundsException();
        }
// Check for overlapping bookings for the same spot
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


        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update decUpdate = new Update().inc("available", -1);

        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery, decUpdate, ParkingSpot.class);

        if (updatedSpot == null) {
            throw new ConflictException("No spots available");
        }

        // No deduction here, only at checkout

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
        return bookingRepository.save(booking);
    }

    private void ensureAvailableNotExceedCapacity(String spotId) {
        ParkingSpot spot = mongoOperations.findById(spotId, ParkingSpot.class);
        if (spot != null && spot.getAvailable() > spot.getCapacity()) {
            spot.setAvailable(spot.getCapacity());
            mongoOperations.save(spot);
        }
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

    // In src/main/java/com/parking/app/service/BookingService.java

    public Bookings checkIn(String bookingId, String qrCode) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Only pending bookings can check in");
        }
        if (!bookingId.equals(qrCode)) {
            throw new java.lang.IllegalStateException("Invalid QR code for check-in");
        }

        // Only one active booking per user at a time
        Query active = new Query(Criteria.where("userId").is(booking.getUserId()).and("status").is("active"));
        if (mongoOperations.exists(active, Bookings.class)) {
            throw new ConflictException("User already has an active booking");
        }

        ZonedDateTime now = ZonedDateTime.now();
        booking.setStatus("active");
        booking.setCheckInTime(Date.from(now.toInstant()));
        booking.setActualCheckInTime(Date.from(now.toInstant())); // Set actual check-in time
        booking.setQrCodeScanned(true);
        return bookingRepository.save(booking);
    }


    public Bookings checkOut(String bookingId, String qrCode) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is not active");
        }
        if (!bookingId.equals(qrCode)) {
            throw new IllegalStateException("Invalid QR code for checkout");
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());

        double lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, booking.getActualCheckInTime() != null
                ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                : scheduledCheckIn);
        double lateCheckOutPenalty = calculatePenaltyWithGrace(scheduledEnd, now);
        double bookingCharge = Math.ceil(booking.getAmount());
        double totalPenalty = lateCheckInPenalty + lateCheckOutPenalty;
        double totalDeduction = bookingCharge + totalPenalty;

        Wallet wallet = walletRepository.findByUserId(booking.getUserId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (!hasSufficientBalance(wallet, totalDeduction)) {
            throw new InsufficientFundsException();
        }

        wallet.setBalance(wallet.getBalance() - totalDeduction);
        wallet.setLastUpdated(new Date());
        walletRepository.save(wallet);

        transactionsRepository.save(new Transactions(booking.getUserId(), -bookingCharge, "Booking charge", new Date()));
        if (lateCheckInPenalty > 0) {
            transactionsRepository.save(new Transactions(booking.getUserId(), -lateCheckInPenalty, "Late check-in penalty", new Date()));
        }
        if (lateCheckOutPenalty > 0) {
            transactionsRepository.save(new Transactions(booking.getUserId(), -lateCheckOutPenalty, "Late check-out penalty", new Date()));
        }

        // Refund logic if needed (e.g., unused time, etc.) can be added here

        booking.setStatus("completed");
        booking.setCheckOutTime(Date.from(now.toInstant()));
        booking.setQrCodeScanned(true);

        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
        ensureAvailableNotExceedCapacity(booking.getSpotId());
        return bookingRepository.save(booking);
    }

    @Scheduled(cron = "0 * * * * *")
    public void autoCompleteLateBookings() {
        ZonedDateTime now = ZonedDateTime.now();
        Query query = new Query(Criteria.where("status").is("active")
                .and("checkOutTime").lte(Date.from(now.toInstant()))
                .and("qrCodeScanned").ne(true));
        List<Bookings> lateBookings = mongoOperations.find(query, Bookings.class);
        for (Bookings booking : lateBookings) {
            ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
            ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());

            double lateCheckInPenalty = calculatePenaltyWithGrace(scheduledCheckIn, booking.getActualCheckInTime() != null
                    ? ZonedDateTime.ofInstant(booking.getActualCheckInTime().toInstant(), now.getZone())
                    : scheduledCheckIn);
            double lateCheckOutPenalty = calculatePenaltyWithGrace(scheduledEnd, now);
            double bookingCharge = Math.ceil(booking.getAmount());
            double totalPenalty = lateCheckInPenalty + lateCheckOutPenalty;
            double totalDeduction = bookingCharge + totalPenalty;

            Wallet wallet = walletRepository.findByUserId(booking.getUserId()).orElse(null);
            if (wallet != null && hasSufficientBalance(wallet, totalDeduction)) {
                wallet.setBalance(wallet.getBalance() - totalDeduction);
                wallet.setLastUpdated(new Date());
                walletRepository.save(wallet);

                transactionsRepository.save(new Transactions(booking.getUserId(), -bookingCharge, "Booking charge", new Date()));
                if (lateCheckInPenalty > 0) {
                    transactionsRepository.save(new Transactions(booking.getUserId(), -lateCheckInPenalty, "Late check-in penalty", new Date()));
                }
                if (lateCheckOutPenalty > 0) {
                    transactionsRepository.save(new Transactions(booking.getUserId(), -lateCheckOutPenalty, "Late check-out penalty", new Date()));
                }
            }
            booking.setStatus("completed");
            booking.setQrCodeScanned(false);
            bookingRepository.save(booking);

            Update incUpdate = new Update().inc("available", 1);
            mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
            ensureAvailableNotExceedCapacity(booking.getSpotId());
        }
// Add this inside autoCompleteLateBookings()

// Handle no-show (never checked in) bookings
        Query noShowQuery = new Query(Criteria.where("status").is("pending")
                .and("checkInTime").lt(Date.from(now.toInstant())));
        List<Bookings> noShowBookings = mongoOperations.find(noShowQuery, Bookings.class);
        for (Bookings booking : noShowBookings) {
            ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
            double penalty = calculatePenaltyWithGrace(scheduledCheckIn, now);
            double bookingCharge = Math.ceil(booking.getAmount());
            double totalDeduction = bookingCharge + penalty;

            Wallet wallet = walletRepository.findByUserId(booking.getUserId()).orElse(null);
            if (wallet != null && hasSufficientBalance(wallet, totalDeduction)) {
                wallet.setBalance(wallet.getBalance() - totalDeduction);
                wallet.setLastUpdated(new Date());
                walletRepository.save(wallet);

                transactionsRepository.save(new Transactions(booking.getUserId(), -bookingCharge, "Booking charge (no-show)", new Date()));
                if (penalty > 0) {
                    transactionsRepository.save(new Transactions(booking.getUserId(), -penalty, "No-show penalty", new Date()));
                }
            }
            booking.setStatus("no-show");
            bookingRepository.save(booking);

            Update incUpdate = new Update().inc("available", 1);
            mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
            ensureAvailableNotExceedCapacity(booking.getSpotId());
        }

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
        // Check for overlapping bookings for the same spot (excluding this booking)
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
        // Update check-out time and fare
        double newAmount = calculateCharge(
                ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), newCheckOutTime.getZone()),
                newCheckOutTime
        );
        booking.setCheckOutTime(Date.from(newCheckOutTime.toInstant()));
        booking.setAmount(newAmount);
        return bookingRepository.save(booking);
    }


    public boolean cancelBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            return false;
        }

        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);
        ensureAvailableNotExceedCapacity(booking.getSpotId());
        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        return true;
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Kolkata")
    public void resetParkingSpotsAvailability() {
        List<ParkingSpot> spots = mongoOperations.findAll(ParkingSpot.class);
        for (ParkingSpot spot : spots) {
            spot.setAvailable(spot.getCapacity());
            mongoOperations.save(spot);
        }
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
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getVehicleNumbers();
    }

    public List<Bookings> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Bookings getBookingById(String id) {
        return bookingRepository.findById(id).orElse(null);
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

    // Validation only, no deduction
    public QrValidationResult validateQrCodeForCheckIn(String bookingId, String qrCode) {
        Bookings booking = getBookingById(bookingId);
        if (booking == null) return new QrValidationResult(false, 0, "Booking not found");
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            return new QrValidationResult(false, 0, "Booking is not pending");
        }
        if (!bookingId.equals(qrCode)) {
            return new QrValidationResult(false, 0, "Invalid QR code for check-in");
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledCheckIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        double penalty = calculatePenaltyWithGrace(scheduledCheckIn, now);

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
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());
        double penalty = calculatePenaltyWithGrace(scheduledEnd, now);

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
}
