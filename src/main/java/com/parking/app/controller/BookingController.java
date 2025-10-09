// Java
package com.parking.app.controller;

import com.parking.app.exception.*;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.model.Bookings;
import com.parking.app.service.BookingService;
import com.parking.app.service.BookingService.QrValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Bookings>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String lotId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            ZonedDateTime from = fromDate != null ? ZonedDateTime.parse(fromDate) : null;
            ZonedDateTime to = toDate != null ? ZonedDateTime.parse(toDate) : null;
            List<Bookings> bookings = bookingService.getAllBookingsFiltered(status, lotId, from, to, page, size);
            return bookings.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(bookings);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format for fromDate: {}, toDate: {}. Error: {}", fromDate, toDate, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Error fetching bookings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/users/{userId}/bookings/start")
    public ResponseEntity<?> startBooking(
            @PathVariable String userId,
            @RequestParam String spotId,
            @RequestParam String lotId,
            @RequestParam String checkInTime,
            @RequestParam String checkOutTime,
            @RequestParam String vehicleNumber
    ) {
        try {
            ZonedDateTime checkIn = ZonedDateTime.parse(checkInTime);
            ZonedDateTime checkOut = ZonedDateTime.parse(checkOutTime);
            Bookings booking = bookingService.startBooking(spotId, userId, lotId, checkIn, checkOut, vehicleNumber);
            return ResponseEntity.ok(booking);
        } catch (DateTimeParseException e) {
            logger.error("Invalid date format for checkInTime: {}, checkOutTime: {}. Error: {}", checkInTime, checkOutTime, e.getMessage());
            return ResponseEntity.badRequest().body("Invalid date format: " + e.getParsedString());
        } catch (ConflictException e) {
            logger.warn("No spots available for spotId: {}", spotId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("No spots available");
        } catch (Exception e) {
            logger.error("Error starting booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/bookings/{bookingId}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable String userId, @PathVariable String bookingId) {
        try {
            Bookings booking = bookingService.confirmBooking(bookingId);
            if (!booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to confirm booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(booking);
        } catch (IllegalStateException | NotFoundException e) {
            logger.error("Error confirming booking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PutMapping("/users/{userId}/bookings/{bookingId}/extend")
    public ResponseEntity<?> extendBooking(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                return ResponseEntity.notFound().build();
            }
            String newCheckOutTimeStr = body.get("newCheckOutTime");
            ZonedDateTime newCheckOutTime = ZonedDateTime.parse(newCheckOutTimeStr);
            Bookings updated = bookingService.extendBooking(bookingId, newCheckOutTime);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/users/{userId}/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable String userId, @PathVariable String bookingId) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to cancel booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            boolean cancelled = bookingService.cancelBooking(bookingId);
            return cancelled ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Error cancelling booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Check-in: expects { "qrCode": "..." } in body, but bookingId is used as QR code ---
    @PostMapping("/users/{userId}/bookings/{bookingId}/checkin")
    public ResponseEntity<?> checkIn(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to check in for booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            String qrCode = body.get("qrCode");
            booking = bookingService.checkIn(bookingId, qrCode);
            return ResponseEntity.ok(booking);
        } catch (IllegalStateException e) {
            logger.error("Error during check-in: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- Check-out: expects { "qrCode": "..." } in body, but bookingId is used as QR code ---
    @PostMapping("/users/{userId}/bookings/{bookingId}/checkout")
    public ResponseEntity<?> checkOut(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to check out for booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            String qrCode = body.get("qrCode");
            booking = bookingService.checkOut(bookingId, qrCode);
            return ResponseEntity.ok(booking);
        } catch (InsufficientFundsException e) {
            logger.error("Insufficient funds for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Insufficient wallet coins");
        } catch (IllegalStateException e) {
            logger.error("Error during check-out: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/{userId}/bookings/{bookingId}")
    public ResponseEntity<Bookings> getBookingById(@PathVariable String userId, @PathVariable String bookingId) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to access booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error fetching booking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/users/{userId}/bookings/{bookingId}")
    public ResponseEntity<?> updateBookingStatus(@PathVariable String userId,
                                                 @PathVariable String bookingId,
                                                 @RequestBody Bookings bookingDetails) {
        if (bookingDetails.getStatus() == null) {
            logger.warn("Status field is required for booking update");
            return ResponseEntity.badRequest().body("Status field is required");
        }
        try {
            Bookings existing = bookingService.getBookingById(bookingId);
            if (existing == null || !existing.getUserId().equals(userId)) {
                logger.warn("User {} tried to update booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            Bookings updated = bookingService.updateBookingStatus(bookingId, bookingDetails.getStatus());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating booking status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/users/{userId}/bookings/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable String userId, @PathVariable String bookingId) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                logger.warn("User {} tried to delete booking {} not belonging to them", userId, bookingId);
                return ResponseEntity.notFound().build();
            }
            bookingService.deleteBooking(bookingId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting booking: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/{userId}/all-bookings")
    public ResponseEntity<List<Bookings>> getAllBookingsForUser(@PathVariable String userId) {
        try {
            List<Bookings> bookings = bookingService.getBookingsByUserId(userId);
            return bookings.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(bookings);
        } catch (Exception e) {
            logger.error("Error fetching user bookings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/users/{userId}/all-bookings/history")
    public ResponseEntity<List<Bookings>> getBookingHistory(@PathVariable String userId) {
        try {
            List<Bookings> history = bookingService.getBookingHistoryByUserId(userId);
            return history.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching booking history: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
    @PostMapping("/admin/reset-spots")
    public ResponseEntity<String> resetAllSpots() {
        try {
            bookingService.resetParkingSpotsAvailability();
            return ResponseEntity.ok("All parking spots have been reset to max capacity.");
        } catch (Exception e) {
            logger.error("Error resetting parking spots: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reset parking spots.");
        }
    }

    // --- QR validation for check-in: expects { "qrCode": "..." } in body ---
    @PostMapping("/users/{userId}/bookings/{bookingId}/validate-qr-checkin")
    public ResponseEntity<?> validateQrCodeCheckIn(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                return ResponseEntity.notFound().build();
            }
            String qrCode = body.get("qrCode");
            QrValidationResult result = bookingService.validateQrCodeForCheckIn(bookingId, qrCode);
            if (!result.valid) {
                return ResponseEntity.badRequest().body(result.message);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating QR code for check-in for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body("Error validating QR code for check-in");
        }
    }

    // --- QR validation for checkout: expects { "qrCode": "..." } in body ---
    @PostMapping("/users/{userId}/bookings/{bookingId}/validate-qr-checkout")
    public ResponseEntity<?> validateQrCodeCheckOut(
            @PathVariable String userId,
            @PathVariable String bookingId,
            @RequestBody Map<String, String> body
    ) {
        try {
            Bookings booking = bookingService.getBookingById(bookingId);
            if (booking == null || !booking.getUserId().equals(userId)) {
                return ResponseEntity.notFound().build();
            }
            String qrCode = body.get("qrCode");
            QrValidationResult result = bookingService.validateQrCodeForCheckOut(bookingId, qrCode);
            if (!result.valid) {
                return ResponseEntity.badRequest().body(result.message);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating QR code for checkout for booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body("Error validating QR code for checkout");
        }
    }

    @GetMapping("/users/{userId}/bookings/{bookingId}/penalty")
    public ResponseEntity<?> getPenaltyInfo(@PathVariable String userId, @PathVariable String bookingId) {
        try {
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
        } catch (Exception e) {
            logger.error("Error fetching penalty info: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error fetching penalty info");
        }
    }
}
