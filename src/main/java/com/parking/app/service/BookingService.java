package com.parking.app.service;

import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.*;
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
@Transactional
public class BookingService {

    private final BookingLifecycleService lifecycleService;
    private final BookingQueryService queryService;
    private final BookingBreakupService breakupService;
    private final BookingAutoCompletionService autoCompletionService;
    private final QRCodeValidationService qrCodeValidationService;
    private final BookingValidationService validationService;
    private final UserService userService;
    private final ParkingSpotService parkingSpotService;

    public BookingService(
            BookingLifecycleService lifecycleService,
            BookingQueryService queryService,
            BookingBreakupService breakupService,
            BookingAutoCompletionService autoCompletionService,
            QRCodeValidationService qrCodeValidationService,
            BookingValidationService validationService,
            UserService userService,
            ParkingSpotService parkingSpotService
    ) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
        this.breakupService = breakupService;
        this.autoCompletionService = autoCompletionService;
        this.qrCodeValidationService = qrCodeValidationService;
        this.validationService = validationService;
        this.userService = userService;
        this.parkingSpotService = parkingSpotService;
    }

    // ===== Booking Lifecycle Operations =====
    public Bookings startBooking(String spotId, String userId, String lotId,
                                ZonedDateTime checkInTime, ZonedDateTime checkOutTime,
                                String vehicleNumber) {
        return lifecycleService.startBooking(spotId, userId, lotId, checkInTime, checkOutTime, vehicleNumber);
    }

    public Bookings confirmBooking(String bookingId) {
        return lifecycleService.confirmBooking(bookingId);
    }

    public Bookings checkIn(String bookingId, String qrCode) {
        return lifecycleService.checkIn(bookingId, qrCode);
    }

    public Bookings checkOut(String bookingId, String qrCode) {
        return lifecycleService.checkOut(bookingId, qrCode);
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

    public List<String> getVehicleNumbersByUserId(String userId) {
        Users user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return queryService.getVehicleNumbersByUserId(userId, user);
    }

    public List<Bookings> getAllBookings() {
        return queryService.getAllBookings();
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

    // ===== QR Code Validation =====
    public QRCodeValidationService.QrValidationResult validateQrCodeForCheckIn(String bookingId, String qrCode) {
        Bookings booking = queryService.getBookingById(bookingId);
        ParkingSpot spot = booking != null ? parkingSpotService.findById(booking.getSpotId()) : null;
        return qrCodeValidationService.validateQrCodeForCheckIn(booking, qrCode, spot);
    }

    public QRCodeValidationService.QrValidationResult validateQrCodeForCheckOut(String bookingId, String qrCode) {
        Bookings booking = queryService.getBookingById(bookingId);
        ParkingSpot spot = booking != null ? parkingSpotService.findById(booking.getSpotId()) : null;
        return qrCodeValidationService.validateQrCodeForCheckOut(booking, qrCode, bookingId, spot);
    }

    public boolean isQrCodeValid(String bookingId) {
        Bookings booking = queryService.getBookingById(bookingId);
        return validationService.isQrCodeValid(booking);
    }

    // ===== Booking Breakup Operations =====
    public Map<String, Object> getBookingBreakup(String bookingId) {
        Bookings booking = queryService.getBookingById(bookingId);
        if (booking == null) {
            throw new NotFoundException("Booking not found");
        }
        if (!"completed".equalsIgnoreCase(booking.getStatus()) &&
            !"cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Breakup only available for checked-out or cancelled bookings");
        }
        ParkingSpot spot = parkingSpotService.findById(booking.getSpotId());
        if (spot == null) {
            throw new NotFoundException("Parking spot not found");
        }
        return breakupService.calculateBookingBreakup(booking, spot);
    }

    // ===== Legacy Support - Inner Class for QrValidationResult =====
    public static class QrValidationResult extends QRCodeValidationService.QrValidationResult {
        public QrValidationResult(boolean valid, double penalty, String message) {
            super(valid, penalty, message);
        }
    }
}

