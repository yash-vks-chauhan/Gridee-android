// Java
package com.parking.app.controller;

import com.parking.app.constants.CheckInMode;
import com.parking.app.dto.CheckInRequestDto;
import com.parking.app.dto.CreateBookingRequestDto;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.Users;
import com.parking.app.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ==================== BOOKING LIFECYCLE OPERATIONS ====================

    @PostMapping("/{userId}/create")
    public ResponseEntity<?> createBooking(
            @PathVariable String userId,
            @RequestBody CreateBookingRequestDto request
    ) {
        ZonedDateTime checkIn = ZonedDateTime.parse(request.getCheckInTime());
        ZonedDateTime checkOut = ZonedDateTime.parse(request.getCheckOutTime());
        Bookings booking = bookingService.createBooking(
                request.getSpotId(),
                userId,
                request.getLotId(),
                checkIn,
                checkOut,
                request.getVehicleNumber()
        );
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{userId}/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String userId, @PathVariable String bookingId) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to cancel booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        boolean cancelled = bookingService.cancelBooking(bookingId);
        return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ==================== CHECK-IN/CHECK-OUT OPERATIONS ====================

    /**
     * Check-in endpoint for operators (no userId required)
     * Supports all authentication modes: QR_CODE, VEHICLE_NUMBER, PIN
     */
    @PostMapping("checkin")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequestDto request) {
        // Validate mode is provided
        if (request.getMode() == null) {
            throw new IllegalArgumentException("Check-in mode is required");
        }

        // Extract operator user ID from security context
        String checkInOperatorId = getAuthenticatedUserId();

        String bookingId = request.getMode() == CheckInMode.QR_CODE
                ? request.getQrCode()
                : null;

        // Support multiple authentication methods with explicit mode selection
        Bookings booking = bookingService.checkIn(
            bookingId,
            request.getMode(),
            request.getQrCode(),
            request.getVehicleNumber(),
            request.getPin(),
            checkInOperatorId  // Pass operator ID who performed check-in
        );
        return ResponseEntity.ok(booking);
    }

    /**
     * User-specific check-in endpoint with bookingId
     * Validates that the booking belongs to the user before check-in
     * Supports all authentication modes
     */
    @PostMapping("/{userId}/checkin/{bookingId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> checkInWithBookingId(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody CheckInRequestDto request
    ) {
        // Validate mode is provided
        if (request.getMode() == null) {
            throw new IllegalArgumentException("Check-in mode is required");
        }

        // Verify booking belongs to user
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            logger.warn("Booking {} not found", bookingId);
            throw new NotFoundException("Booking not found");
        }

        if (!booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to check in for booking {} not belonging to them", userId, bookingId);
            throw new com.parking.app.exception.IllegalStateException("Booking does not belong to this user");
        }

        // For user self check-in, pass null as checkInOperatorId
        booking = bookingService.checkIn(
            bookingId,
            request.getMode(),
            request.getQrCode(),
            request.getVehicleNumber(),
            request.getPin(),
            null  // null for user self check-in
        );

        return ResponseEntity.ok(booking);
    }

    /**
     * Check-out endpoint for operators (no userId required)
     * Supports all authentication modes: QR_CODE, VEHICLE_NUMBER, PIN
     */
    @PostMapping("checkout")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<?> checkOut(@RequestBody CheckInRequestDto request) {
        // Validate mode is provided
        if (request.getMode() == null) {
            throw new IllegalArgumentException("Check-out mode is required");
        }

        // Extract operator user ID from security context
        String checkOutOperatorId = getAuthenticatedUserId();

        String bookingId = request.getMode() == CheckInMode.QR_CODE
                ? request.getQrCode()
                : null;

        // Support multiple authentication methods with explicit mode selection
        Bookings booking = bookingService.checkOut(
            bookingId,
            request.getMode(),
            request.getQrCode(),
            request.getVehicleNumber(),
            request.getPin(),
            checkOutOperatorId  // Pass operator ID who performed check-out
        );
        return ResponseEntity.ok(booking);
    }

    /**
     * User-specific check-out endpoint with bookingId
     * Validates that the booking belongs to the user before check-out
     * Supports all authentication modes
     */
    @PostMapping("/{userId}/checkout/{bookingId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> checkOutWithBookingId(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody CheckInRequestDto request
    ) {
        // Validate mode is provided
        if (request.getMode() == null) {
            throw new IllegalArgumentException("Check-out mode is required");
        }

        // Verify booking belongs to user
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null) {
            logger.warn("Booking {} not found", bookingId);
            throw new NotFoundException("Booking not found");
        }

        if (!booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to check out for booking {} not belonging to them", userId, bookingId);
            throw new com.parking.app.exception.IllegalStateException("Booking does not belong to this user");
        }

        // For user self check-out, pass null as checkOutOperatorId
        booking = bookingService.checkOut(
            bookingId,
            request.getMode(),
            request.getQrCode(),
            request.getVehicleNumber(),
            request.getPin(),
            null  // null for user self check-out
        );

        return ResponseEntity.ok(booking);
    }

    // DEPRECATED: Old checkout endpoint - kept for backward compatibility
    @Deprecated
    @PostMapping("/{userId}/{bookingId}/checkout")
    public ResponseEntity<?> checkOutOld(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to check out for booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        String qrCode = body.get("qrCode");
        booking = bookingService.checkOut(bookingId, CheckInMode.QR_CODE, qrCode, null, null, null);
        return ResponseEntity.ok(booking);
    }


    // ==================== BOOKING QUERIES ====================

    @GetMapping("/{userId}/{bookingId}")
    public ResponseEntity<Bookings> getBookingById(@PathVariable String userId, @PathVariable String bookingId) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to access booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/{userId}/all")
    public ResponseEntity<List<Bookings>> getAllBookingsForUser(@PathVariable String userId) {
        List<Bookings> bookings = bookingService.getBookingsByUserId(userId);
        return bookings.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(bookings);
    }

    @GetMapping("/{userId}/all/history")
    public ResponseEntity<List<Bookings>> getBookingHistory(@PathVariable String userId) {
        List<Bookings> history = bookingService.getBookingHistoryByUserId(userId);
        return history.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(history);
    }

    @GetMapping("/{userId}/{bookingId}/priceBreakup")
    public ResponseEntity<?> getBookingBreakup(
            @PathVariable String userId,
            @PathVariable String bookingId
    ) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to access breakup for booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> breakup = bookingService.getBookingBreakup(bookingId);
        return ResponseEntity.ok(breakup);
    }

    // ==================== BOOKING MODIFICATIONS ====================

    @PutMapping("/{userId}/{bookingId}")
    public ResponseEntity<?> updateBookingStatus(@PathVariable String userId,
                                                 @PathVariable String bookingId,
                                                 @RequestBody Bookings bookingDetails) {
        if (bookingDetails.getStatus() == null) {
            throw new IllegalArgumentException("Status field is required");
        }

        Bookings existing = bookingService.getBookingById(bookingId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            logger.warn("User {} tried to update booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        Bookings updated = bookingService.updateBookingStatus(bookingId, bookingDetails.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{userId}/{bookingId}/extend")
    public ResponseEntity<?> extendBooking(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        String newCheckOutTimeStr = body.get("newCheckOutTime");
        ZonedDateTime newCheckOutTime = ZonedDateTime.parse(newCheckOutTimeStr);
        Bookings updated = bookingService.extendBooking(bookingId, newCheckOutTime);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{userId}/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable String userId, @PathVariable String bookingId) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            logger.warn("User {} tried to delete booking {} not belonging to them", userId, bookingId);
            return ResponseEntity.notFound().build();
        }
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    // ==================== FINANCIAL/PENALTY OPERATIONS ====================

    @GetMapping("/{userId}/{bookingId}/penalty")
    public ResponseEntity<?> getPenaltyInfo(@PathVariable String userId, @PathVariable String bookingId) {
        Bookings booking = bookingService.getBookingById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime checkIn = booking.getCheckInTime().toInstant().atZone(ZoneId.systemDefault());
        long minutesSinceCheckIn = java.time.Duration.between(checkIn, now).toMinutes();
        double penalty = 0;
        if (minutesSinceCheckIn > 10) {
            penalty = (minutesSinceCheckIn - 10) * 2.0;
        }
        return ResponseEntity.ok(penalty);
    }

    // ==================== ADMIN OPERATIONS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Bookings>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lotId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ZonedDateTime from = fromDate != null ? ZonedDateTime.parse(fromDate) : null;
        ZonedDateTime to = toDate != null ? ZonedDateTime.parse(toDate) : null;
        List<Bookings> bookings = bookingService.getAllBookingsFiltered(status, lotId, from, to, page, size);
        return bookings.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(bookings);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper method to extract authenticated user ID from security context
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Users) {
            Users user = (Users) authentication.getPrincipal();
            return user.getId();
        }
        return null;
    }
}
