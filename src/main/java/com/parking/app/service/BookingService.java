package com.parking.app.service;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.model.Users;
import com.parking.app.repository.BookingRepository;
import com.parking.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class BookingService {

    private static final double HOURLY_RATE = 2.5;
    private static final double PENALTY_RATE = 5.0;
    private static final int DEFAULT_HOLD_HOURS = 2;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final MongoOperations mongoOperations;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            MongoOperations mongoOperations
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.mongoOperations = mongoOperations;
    }

    private Bookings findBookingOrThrow(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    public Bookings startBooking(String spotId, String userId, String lotId, ZonedDateTime checkInTime, ZonedDateTime checkOutTime, String vehicleNumber) {
        validateTimes(checkInTime, checkOutTime);

        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update decUpdate = new Update().inc("available", -1);

        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery, decUpdate, org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);

        if (updatedSpot == null) {
            throw new ConflictException("No spots available");
        }

        double amount = calculateCharge(checkInTime, checkOutTime);

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

    private void validateTimes(ZonedDateTime checkIn, ZonedDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and Check-out times are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out time must be after Check-in time");
        }
    }

    public List<String> getVehicleNumbersByUserId(String userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return user.getVehicleNumbers();
    }

    private double calculateCharge(ZonedDateTime from, ZonedDateTime to) {
        long hours = ChronoUnit.HOURS.between(from, to);
        if (from.plusHours(hours).isBefore(to)) {
            hours++; // round up if not exact
        }
        return hours * HOURLY_RATE;
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

    public Bookings checkIn(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only active bookings can check in");
        }
        booking.setCheckInTime(Date.from(ZonedDateTime.now().toInstant()));
        return bookingRepository.save(booking);
    }

    public Bookings checkOut(String bookingId) {
        Bookings booking = findBookingOrThrow(bookingId);
        if (!"active".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Only active bookings can check out");
        }

        ZonedDateTime now = ZonedDateTime.now();
        booking.setStatus("completed");
        booking.setCheckOutTime(Date.from(now.toInstant()));

        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(booking.getSpotId())), incUpdate, ParkingSpot.class);

        ZonedDateTime checkIn = ZonedDateTime.ofInstant(booking.getCheckInTime().toInstant(), now.getZone());
        ZonedDateTime scheduledEnd = ZonedDateTime.ofInstant(booking.getCheckOutTime().toInstant(), now.getZone());

        double baseCharge = calculateCharge(checkIn, now);
        double penalty = calculatePenalty(scheduledEnd, now);
        double totalCost = baseCharge + penalty;
        booking.setAmount(totalCost);

        Users user = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        int neededCoins = (int) Math.ceil(totalCost);
        if (user.getWalletCoins() < neededCoins) {
            throw new InsufficientFundsException();
        }
        user.setWalletCoins(user.getWalletCoins() - neededCoins);
        userRepository.save(user);

        return bookingRepository.save(booking);
    }

    private double calculatePenalty(ZonedDateTime scheduledEnd, ZonedDateTime actualEnd) {
        if (actualEnd.isBefore(scheduledEnd)) {
            return 0;
        }
        long lateHours = ChronoUnit.HOURS.between(scheduledEnd, actualEnd);
        if (scheduledEnd.plusHours(lateHours).isBefore(actualEnd)) {
            lateHours++; // round up
        }
        return lateHours * PENALTY_RATE;
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
}
