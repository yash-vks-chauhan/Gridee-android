package com.parking.app.integration.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model to hold comprehensive test metrics for reporting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestMetrics {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMetrics {
        private String userId;
        private String username;
        private String email;
        private double initialBalance;
        private double currentBalance;
        private double utilizedAmount;
        private int successfulBookings;
        private int failedBookings;
        private List<BookingDetail> bookings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingDetail {
        private String bookingId;
        private String spotId;
        private String lotId;
        private String vehicleNumber;
        private String checkInTime;
        private String checkOutTime;
        private double amount;
        private String status; // SUCCESS, FAILED, CANCELLED
        private String failureReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParkingLotMetrics {
        private String lotId;
        private String lotName;
        private int totalCapacity;
        private int totalSpots;
        private int bookedSpots;
        private int availableSpots;
        private List<SpotDetail> spots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotDetail {
        private String spotId;
        private String zoneName;
        private int capacity;
        private int booked;
        private int available;
        private double bookingRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }

    // Overall metrics
    private List<UserMetrics> userMetrics = new ArrayList<>();
    private List<ParkingLotMetrics> parkingLotMetrics = new ArrayList<>();

    // Summary totals
    private int totalUsers;
    private int totalParkingLots;
    private int totalParkingSpots;
    private int totalCapacity;
    private int totalBookingAttempts;
    private int totalSuccessfulBookings;
    private int totalFailedBookings;
    private int totalCancelledBookings;
    private double totalInitialBalance;
    private double totalCurrentBalance;
    private double totalUtilizedAmount;
    private double totalExpectedRevenue;

    // Validation
    private ValidationResult validationResult;
}

