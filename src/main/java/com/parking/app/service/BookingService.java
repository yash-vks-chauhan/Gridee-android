package com.parking.app.service;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.model.Wallet;
import com.parking.app.repository.BookingRepository;
import com.parking.app.repository.UserRepository;
import com.parking.app.repository.WalletRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private static final double HOURLY_RATE = 5.0;
    private static final double PENALTY_RATE = 10.0;
    private static final int DEFAULT_HOLD_HOURS = 2;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final MongoOperations mongoOperations;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            WalletRepository walletRepository,
            MongoOperations mongoOperations
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.mongoOperations = mongoOperations;
    }

    private Bookings findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private boolean hasSufficientBalance(Wallet wallet, double amount) {
        return wallet.getBalance() >= Math.ceil(amount);
    }

    private double calculatePenalty(ZonedDateTime scheduledEnd, ZonedDateTime actualEnd) {
        if (actualEnd.isBefore(scheduledEnd)) {
            return 0;
        }
        long lateHours = java.time.temporal.ChronoUnit.HOURS.between(scheduledEnd, actualEnd);
        if (scheduledEnd.plusHours(lateHours).isBefore(actualEnd)) {
            lateHours++; // round up
        }
        return lateHours * PENALTY_RATE;
    }

    private double calculateCharge(ZonedDateTime from, ZonedDateTime to) {
        long hours = java.time.temporal.ChronoUnit.HOURS.between(from, to);
        if (from.plusHours(hours).isBefore(to)) {
            hours++; // round up if not exact
        }
        return hours * HOURLY_RATE;
    }

    private void validateTimes(ZonedDateTime checkIn, ZonedDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and Check-out times are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out time must be after Check-in time");
        }
    }

    // Booking time restriction logic added here
    private void validateBookingWindow(ZonedDateTime checkInTime, ZonedDateTime checkOutTime) {
        LocalDate today = ZonedDateTime.now().toLocalDate();
        LocalTime nowTime = ZonedDateTime.now().toLocalTime();
        LocalTime cutoff = LocalTime.of(20, 0); // 8 pm

        if (nowTime.isBefore(cutoff)) {
            // Before 8 pm: only allow bookings for today, ending before 8 pm
            if (!checkInTime.toLocalDate().isEqual(today) || checkOutTime.toLocalTime().isAfter(cutoff)) {
                throw new java.lang.IllegalStateException("Bookings are only allowed for today until 8 pm.");
            }
        } else {
            // After 8 pm: only allow bookings for tomorrow
            LocalDate tomorrow = today.plusDays(1);
            if (!checkInTime.toLocalDate().isEqual(tomorrow) || !checkOutTime.toLocalDate().isEqual(tomorrow)) {
                throw new java.lang.IllegalStateException("Bookings after 8 pm are only allowed for tomorrow.");
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

        System.out.println("Wallet balance: " + wallet.getBalance() + ", Booking charge: " + amount);

        if (!hasSufficientBalance(wallet, amount)) {
            throw new InsufficientFundsException();
        }

        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update decUpdate = new Update().inc("available", -1);

        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery, decUpdate, org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);

        if (updatedSpot == null) {
            throw new ConflictException("No spots available");
        }

        wallet.setBalance(wallet.getBalance() - Math.ceil(amount));
        wallet.setLastUpdated(new Date());
        walletRepository.save(wallet);

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
        return bookingRepository.save(booking);
    }

    public Bookings checkOut(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new java.lang.IllegalStateException("Only active bookings can check out");
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());

        booking.setStatus("completed");
        booking.setCheckOutTime(Date.from(now.toInstant()));

        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);

        Wallet wallet = walletRepository.findByUserId(booking.getUserId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (now.isAfter(scheduledEnd)) {
            double penalty = calculatePenalty(scheduledEnd, now);
            wallet.setBalance(wallet.getBalance() - Math.ceil(penalty)); // Allow negative
            wallet.setLastUpdated(new Date());
            booking.setAmount(penalty);
        } else {
            wallet.setBalance(wallet.getBalance() + Math.ceil(booking.getAmount()));
            wallet.setLastUpdated(new Date());
            booking.setAmount(booking.getAmount());
        }
        walletRepository.save(wallet);

        return bookingRepository.save(booking);
    }

    public Bookings confirmBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"pending".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }

        booking.setStatus("active");
        ZonedDateTime now = ZonedDateTime.now();
        booking.setCheckInTime(Date.from(now.toInstant()));

        long expiryTimestamp = booking.getCheckOutTime() != null
                ? booking.getCheckOutTime().getTime()
                : (booking.getCheckInTime() != null
                ? booking.getCheckInTime().getTime() + DEFAULT_HOLD_HOURS * 60 * 60 * 1000
                : Instant.now().toEpochMilli() + DEFAULT_HOLD_HOURS * 60 * 60 * 1000);

        String qrRaw = booking.getId() + "|" + expiryTimestamp;
        String qrCode = Base64.getEncoder().encodeToString(qrRaw.getBytes(StandardCharsets.UTF_8));
        booking.setQrCode(qrCode);

        return bookingRepository.save(booking);
    }

    public boolean cancelBooking(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if ("cancelled".equalsIgnoreCase(booking.getStatus())) {
            return false;
        }

        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);

        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        return true;
    }

    @Scheduled(cron = "0 0 20 * * *")
    public void resetParkingSpotsAvailability() {
        List<ParkingSpot> spots = mongoOperations.findAll(ParkingSpot.class);
        for (ParkingSpot spot : spots) {
            spot.setAvailable(spot.getCapacity());
            mongoOperations.save(spot);
        }
    }

    public Bookings checkIn(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only active bookings can check in");
        }
        booking.setCheckInTime(Date.from(ZonedDateTime.now().toInstant()));
        return bookingRepository.save(booking);
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
        if (status != null) query.addCriteria(Criteria.where("status").is(status));
        if (lotId != null) query.addCriteria(Criteria.where("lotId").is(lotId));
        if (fromDate != null) query.addCriteria(Criteria.where("checkInTime").gte(Date.from(fromDate.toInstant())));
        if (toDate != null) query.addCriteria(Criteria.where("checkOutTime").lte(Date.from(toDate.toInstant())));
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<Bookings> getBookingHistoryByUserId(String userId) {
        Date now = Date.from(ZonedDateTime.now().toInstant());
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .orOperator(
                                Criteria.where("status").in("completed", "cancelled"),
                                Criteria.where("checkOutTime").lt(now)
                        )
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
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public void deleteBooking(String id) {
        bookingRepository.deleteById(id);
    }

    public List<Bookings> getBookingsByUserId(String userId) {
        return bookingRepository.findByUserId(userId);
    }
    public boolean isQrCodeValid(String bookingId) {
        Bookings booking = getBookingById(bookingId);
        if (booking == null) return false;
        Date now = new Date();
        Date checkIn = booking.getCheckInTime();
        Date checkOut = booking.getCheckOutTime();
        // QR is valid only between check-in and check-out
        return checkIn != null && checkOut != null && !now.before(checkIn) && !now.after(checkOut);
    }

}
