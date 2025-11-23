package com.parking.app.service;

import com.parking.app.constants.BookingStatus;
import com.parking.app.constants.CheckInMode;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.service.booking.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Main BookingService - acts as a facade to delegate to specialized booking services
 */
@Service
//@Transactional
public class BookingService {

    private final BookingLifecycleService lifecycleService;
    private final BookingQueryService queryService;
    private final BookingBreakupService breakupService;
    private final BookingAutoCompletionService autoCompletionService;
    private final BookingValidationService validationService;
    private final ParkingSpotService parkingSpotService;

    public BookingService(
            BookingLifecycleService lifecycleService,
            BookingQueryService queryService,
            BookingBreakupService breakupService,
            BookingAutoCompletionService autoCompletionService,
            BookingValidationService validationService,
            ParkingSpotService parkingSpotService
    ) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
        this.breakupService = breakupService;
        this.autoCompletionService = autoCompletionService;
        this.validationService = validationService;
        this.parkingSpotService = parkingSpotService;
    }

    // ===== Booking Lifecycle Operations =====
    public Bookings createBooking(String spotId, String userId,
                                  ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                  String vehicleNumber) {
        return lifecycleService.createBooking(spotId, userId, checkInTime, checkOutTime, vehicleNumber);
    }

    public Bookings checkIn(String bookingId, CheckInMode mode, String qrCode, String vehicleNumber, String pin, String checkInOperatorId) {
        return lifecycleService.checkIn(bookingId, mode, qrCode, vehicleNumber, pin, checkInOperatorId);
    }

    public Bookings checkOut(String bookingId, CheckInMode mode, String qrCode, String vehicleNumber, String pin, String checkOutOperatorId) {
        return lifecycleService.checkOut(bookingId, mode, qrCode, vehicleNumber, pin, checkOutOperatorId);
    }

    public Bookings extendBooking(String bookingId, ZonedDateTime newCheckOutTime) {
        return lifecycleService.extendBooking(bookingId, newCheckOutTime);
    }

    public boolean cancelBooking(String bookingId) {
        return lifecycleService.cancelBooking(bookingId);
    }

    public Bookings updateBookingStatus(String id, String status) {
        return lifecycleService.updateBookingStatus(id, status);
    }

    public void deleteBooking(String id) {
        lifecycleService.deleteBooking(id);
    }

    // ===== Query Operations =====
    public List<Bookings> getAllBookingsFiltered(String status, String lotId,
                                                  ZonedDateTime fromDate, ZonedDateTime toDate,
                                                  int page, int size) {
        return queryService.getAllBookingsFiltered(status, lotId, fromDate, toDate, page, size);
    }

    public List<Bookings> getBookingHistoryByUserId(String userId) {
        return queryService.getBookingHistoryByUserId(userId);
    }

    public Bookings getBookingById(String id) {
        return queryService.getBookingById(id);
    }

    public List<Bookings> getBookingsByUserId(String userId) {
        return queryService.getBookingsByUserId(userId);
    }

    public List<Bookings> findByLotIdAndTimeWindow(String lotId, ZonedDateTime startTime,
                                                    ZonedDateTime endTime) {
        return queryService.findByLotIdAndTimeWindow(lotId, startTime, endTime);
    }

    // ===== Auto-completion Operations =====
    public void autoCompleteLateBookings() {
        autoCompletionService.autoCompleteLateBookings();
    }

    // ===== Booking Breakup Operations =====
    public Map<String, Object> getBookingBreakup(String bookingId) {
        Bookings booking = queryService.getBookingById(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking not found");
        }
        if (!BookingStatus.COMPLETED.name().equalsIgnoreCase(booking.getStatus()) &&
            !BookingStatus.CANCELLED.name().equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Breakup only available for checked-out or cancelled bookings");
        }
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) {
            throw new NotFoundException("Parking spot not found");
        }
        return breakupService.calculateBookingBreakup(booking, spot);
    }
}
