package com.parking.app.integration.report;

import com.parking.app.integration.helper.BookingHelper;
import com.parking.app.integration.helper.ParkingHelper;
import com.parking.app.integration.helper.WalletHelper;
import com.parking.app.integration.model.TestParkingLot;
import com.parking.app.integration.model.TestParkingSpot;
import com.parking.app.integration.model.TestUser;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Comprehensive report generator for integration test results
 * Generates detailed Allure reports with:
 * - User wallet balances and booking details
 * - Parking lot and spot utilization
 * - Financial reconciliation
 * - Data validation and consistency checks
 */
public class AllureReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AllureReportGenerator.class);

    private final WalletHelper walletHelper;
    private final ParkingHelper parkingHelper;
    private final BookingHelper bookingHelper;
    private final String adminToken;

    private TestMetrics metrics;
    private Map<String, Double> userInitialBalances = new HashMap<>();
    private Map<String, List<TestMetrics.BookingDetail>> userBookingDetails = new HashMap<>();

    public AllureReportGenerator(WalletHelper walletHelper,
                                  ParkingHelper parkingHelper,
                                  BookingHelper bookingHelper,
                                  String adminToken) {
        this.walletHelper = walletHelper;
        this.parkingHelper = parkingHelper;
        this.bookingHelper = bookingHelper;
        this.adminToken = adminToken;
        this.metrics = new TestMetrics();
    }

    /**
     * Record initial balance for a user
     */
    public void recordInitialBalance(String userId, double balance) {
        userInitialBalances.put(userId, balance);
    }

    /**
     * Record a booking attempt
     */
    public void recordBookingAttempt(String userId, String bookingId, String spotId, String lotId,
                                      String vehicleNumber, String checkInTime, String checkOutTime,
                                      double amount, boolean success, String failureReason) {
        TestMetrics.BookingDetail booking = TestMetrics.BookingDetail.builder()
            .bookingId(bookingId)
            .spotId(spotId)
            .lotId(lotId)
            .vehicleNumber(vehicleNumber)
            .checkInTime(checkInTime)
            .checkOutTime(checkOutTime)
            .amount(amount)
            .status(success ? "SUCCESS" : "FAILED")
            .failureReason(failureReason)
            .build();

        userBookingDetails.computeIfAbsent(userId, k -> new ArrayList<>()).add(booking);
    }

    /**
     * Record a cancelled booking
     */
    public void recordCancelledBooking(String userId, String bookingId) {
        List<TestMetrics.BookingDetail> bookings = userBookingDetails.get(userId);
        if (bookings != null) {
            bookings.stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .ifPresent(b -> b.setStatus("CANCELLED"));
        }
    }

    /**
     * Generate comprehensive report after all tests complete
     */
    @Step("Generate Comprehensive Test Report")
    public void generateComprehensiveReport(List<TestUser> testUsers,
                                            List<TestParkingLot> parkingLots,
                                            List<TestParkingSpot> parkingSpots) {

        logger.info("\n\n");
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("📊 GENERATING COMPREHENSIVE TEST REPORT");
        logger.info("═══════════════════════════════════════════════════════════\n");

        // Collect user metrics
        collectUserMetrics(testUsers);

        // Collect parking lot metrics
        collectParkingLotMetrics(parkingLots, parkingSpots);

        // Calculate summary totals
        calculateSummaryTotals();

        // Validate data consistency
        validateDataConsistency();

        // Generate Allure attachments
        generateAllureAttachments();

        // Print detailed console report
        printDetailedReport();

        logger.info("\n✅ Report generation complete\n");
    }

    private void collectUserMetrics(List<TestUser> testUsers) {
        logger.info("📋 Collecting user metrics...\n");

        for (TestUser user : testUsers) {
            try {
                // Get current wallet balance
                Response walletResponse = walletHelper.getWallet(user.getUserId(), user.getBearerToken());
                double currentBalance = walletResponse.statusCode() == 200
                    ? walletResponse.jsonPath().getDouble("balance")
                    : 0.0;

                // Get initial balance (or use current if not recorded)
                double initialBalance = userInitialBalances.getOrDefault(user.getUserId(), currentBalance);

                // Get booking details
                List<TestMetrics.BookingDetail> bookings = userBookingDetails.getOrDefault(
                    user.getUserId(), new ArrayList<>());

                // Calculate metrics
                long successCount = bookings.stream().filter(b -> "SUCCESS".equals(b.getStatus())).count();
                long failedCount = bookings.stream().filter(b -> "FAILED".equals(b.getStatus())).count();
                double utilizedAmount = bookings.stream()
                    .filter(b -> "SUCCESS".equals(b.getStatus()) || "CANCELLED".equals(b.getStatus()))
                    .mapToDouble(TestMetrics.BookingDetail::getAmount)
                    .sum();

                TestMetrics.UserMetrics userMetrics = TestMetrics.UserMetrics.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .initialBalance(initialBalance)
                    .currentBalance(currentBalance)
                    .utilizedAmount(utilizedAmount)
                    .successfulBookings((int) successCount)
                    .failedBookings((int) failedCount)
                    .bookings(bookings)
                    .build();

                metrics.getUserMetrics().add(userMetrics);

                logger.info("   ✅ User: {} - Initial: ${}, Current: ${}, Utilized: ${}",
                    user.getUsername(),
                    String.format("%.2f", initialBalance),
                    String.format("%.2f", currentBalance),
                    String.format("%.2f", utilizedAmount));

            } catch (Exception e) {
                logger.warn("   ⚠️  Failed to collect metrics for user {}: {}",
                    user.getUsername(), e.getMessage());
            }
        }

        logger.info("");
    }

    private void collectParkingLotMetrics(List<TestParkingLot> parkingLots,
                                          List<TestParkingSpot> parkingSpots) {
        logger.info("📋 Collecting parking lot metrics...\n");

        // Debug: Log all recorded bookings
        logger.info("   📝 Debug: Total users with bookings: {}", userBookingDetails.size());
        int totalBookingsRecorded = userBookingDetails.values().stream()
            .mapToInt(List::size)
            .sum();
        logger.info("   📝 Debug: Total bookings recorded: {}", totalBookingsRecorded);

        for (TestParkingLot lot : parkingLots) {
            try {
                // Find spots for this lot
                List<TestParkingSpot> lotSpots = parkingSpots.stream()
                    .filter(spot -> spot.getLotName().equals(lot.getName()))
                    .collect(Collectors.toList());

                logger.info("   📝 Processing lot: {} with {} spots", lot.getName(), lotSpots.size());

                // Calculate spot details based on ACTUAL bookings recorded
                List<TestMetrics.SpotDetail> spotDetails = new ArrayList<>();
                int totalCapacity = 0;
                int totalBooked = 0;

                for (TestParkingSpot spot : lotSpots) {
                    // Count ACTUAL successful bookings for this spot from our records
                    int bookedCount = (int) userBookingDetails.values().stream()
                        .flatMap(List::stream)
                        .filter(booking -> {
                            boolean spotMatches = booking.getSpotId() != null && booking.getSpotId().equals(spot.getId());
                            boolean isSuccess = "SUCCESS".equals(booking.getStatus());
                            if (spotMatches) {
                                logger.info("      🎯 Found booking for spot {} - Status: {}", spot.getId(), booking.getStatus());
                            }
                            return spotMatches && isSuccess;
                        })
                        .count();

                    int capacity = spot.getCapacity();
                    int available = capacity - bookedCount;

                    totalCapacity += capacity;
                    totalBooked += bookedCount;

                    logger.info("      ✅ Spot: {} - Capacity: {}, Booked: {}, Available: {}",
                        spot.getId(), capacity, bookedCount, available);

                    TestMetrics.SpotDetail spotDetail = TestMetrics.SpotDetail.builder()
                        .spotId(spot.getId())
                        .zoneName(spot.getZoneName())
                        .capacity(capacity)
                        .booked(bookedCount)
                        .available(available)
                        .bookingRate(spot.getBookingRate())
                        .build();

                    spotDetails.add(spotDetail);
                }

                TestMetrics.ParkingLotMetrics lotMetrics = TestMetrics.ParkingLotMetrics.builder()
                    .lotId(lot.getId())
                    .lotName(lot.getName())
                    .totalCapacity(totalCapacity)
                    .totalSpots(lotSpots.size())
                    .bookedSpots(totalBooked)
                    .availableSpots(totalCapacity - totalBooked)
                    .spots(spotDetails)
                    .build();

                metrics.getParkingLotMetrics().add(lotMetrics);

                logger.info("   ✅ Lot: {} - Capacity: {}, Booked: {}, Available: {}",
                    lot.getName(), totalCapacity, totalBooked, (totalCapacity - totalBooked));

            } catch (Exception e) {
                logger.warn("   ⚠️  Failed to collect metrics for lot {}: {}",
                    lot.getName(), e.getMessage());
                e.printStackTrace();
            }
        }

        logger.info("");
    }

    private void calculateSummaryTotals() {
        logger.info("📋 Calculating summary totals...\n");

        metrics.setTotalUsers(metrics.getUserMetrics().size());
        metrics.setTotalParkingLots(metrics.getParkingLotMetrics().size());

        // User totals
        metrics.setTotalInitialBalance(
            metrics.getUserMetrics().stream()
                .mapToDouble(TestMetrics.UserMetrics::getInitialBalance)
                .sum()
        );

        metrics.setTotalCurrentBalance(
            metrics.getUserMetrics().stream()
                .mapToDouble(TestMetrics.UserMetrics::getCurrentBalance)
                .sum()
        );

        metrics.setTotalUtilizedAmount(
            metrics.getUserMetrics().stream()
                .mapToDouble(TestMetrics.UserMetrics::getUtilizedAmount)
                .sum()
        );

        // Count successful bookings (excluding cancelled ones)
        int successfulBookings = (int) metrics.getUserMetrics().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "SUCCESS".equals(b.getStatus()))
            .count();

        metrics.setTotalSuccessfulBookings(successfulBookings);

        // Count failed bookings
        int failedBookings = (int) metrics.getUserMetrics().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "FAILED".equals(b.getStatus()))
            .count();

        metrics.setTotalFailedBookings(failedBookings);

        // Count cancelled bookings
        int cancelledBookings = (int) metrics.getUserMetrics().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "CANCELLED".equals(b.getStatus()))
            .count();

        metrics.setTotalCancelledBookings(cancelledBookings);

        // Total booking attempts includes ALL bookings: successful + failed + cancelled
        metrics.setTotalBookingAttempts(successfulBookings + failedBookings + cancelledBookings);

        // Parking lot totals
        metrics.setTotalParkingSpots(
            metrics.getParkingLotMetrics().stream()
                .mapToInt(TestMetrics.ParkingLotMetrics::getTotalSpots)
                .sum()
        );

        metrics.setTotalCapacity(
            metrics.getParkingLotMetrics().stream()
                .mapToInt(TestMetrics.ParkingLotMetrics::getTotalCapacity)
                .sum()
        );

        // Calculate expected revenue from successful bookings (excluding cancelled)
        metrics.setTotalExpectedRevenue(
            metrics.getUserMetrics().stream()
                .flatMap(u -> u.getBookings().stream())
                .filter(b -> "SUCCESS".equals(b.getStatus()))
                .mapToDouble(TestMetrics.BookingDetail::getAmount)
                .sum()
        );

        logger.info("   ✅ Summary totals calculated");
        logger.info("      Total Attempts: {} (Success: {}, Failed: {}, Cancelled: {})",
            metrics.getTotalBookingAttempts(), successfulBookings, failedBookings, cancelledBookings);
    }

    private void validateDataConsistency() {
        logger.info("\n📋 Validating data consistency...\n");

        TestMetrics.ValidationResult validation = TestMetrics.ValidationResult.builder()
            .valid(true)
            .errors(new ArrayList<>())
            .warnings(new ArrayList<>())
            .build();

        // Validate wallet balance consistency
        double expectedRemainingBalance = metrics.getTotalInitialBalance() - metrics.getTotalUtilizedAmount();
        double actualRemainingBalance = metrics.getTotalCurrentBalance();
        double balanceDifference = Math.abs(expectedRemainingBalance - actualRemainingBalance);

        if (balanceDifference > 0.01) { // Allow 1 cent tolerance for rounding
            validation.addError(String.format(
                "Wallet balance mismatch! Expected: $%.2f, Actual: $%.2f, Difference: $%.2f",
                expectedRemainingBalance, actualRemainingBalance, balanceDifference
            ));
        } else {
            logger.info("   ✅ Wallet balances match (within tolerance)");
        }

        // Validate total capacity vs bookings
        int activeBookings = metrics.getTotalSuccessfulBookings() - metrics.getTotalCancelledBookings();
        if (activeBookings > metrics.getTotalCapacity()) {
            validation.addError(String.format(
                "Booking count exceeds capacity! Active bookings: %d, Total capacity: %d",
                activeBookings, metrics.getTotalCapacity()
            ));
        } else {
            logger.info("   ✅ Booking counts within capacity limits");
        }

        // Validate each parking lot
        for (TestMetrics.ParkingLotMetrics lot : metrics.getParkingLotMetrics()) {
            int expectedAvailable = lot.getTotalCapacity() - lot.getBookedSpots();
            if (expectedAvailable < 0) {
                validation.addWarning(String.format(
                    "Lot %s: Negative availability calculated (%d)",
                    lot.getLotName(), expectedAvailable
                ));
            }
        }

        // Validate financial reconciliation
        double totalBookingRevenue = metrics.getUserMetrics().stream()
            .flatMap(u -> u.getBookings().stream())
            .filter(b -> "SUCCESS".equals(b.getStatus()))
            .mapToDouble(TestMetrics.BookingDetail::getAmount)
            .sum();

        if (Math.abs(totalBookingRevenue - metrics.getTotalUtilizedAmount()) > 0.01) {
            validation.addWarning(String.format(
                "Revenue calculation mismatch: Booking revenue: $%.2f, Utilized amount: $%.2f",
                totalBookingRevenue, metrics.getTotalUtilizedAmount()
            ));
        } else {
            logger.info("   ✅ Financial reconciliation successful");
        }

        metrics.setValidationResult(validation);

        if (validation.isValid()) {
            logger.info("\n   ✅ ALL VALIDATIONS PASSED\n");
        } else {
            logger.warn("\n   ⚠️  VALIDATION ERRORS FOUND:\n");
            validation.getErrors().forEach(error -> logger.warn("      ❌ {}", error));
            validation.getWarnings().forEach(warning -> logger.warn("      ⚠️  {}", warning));
            logger.info("");
        }
    }

    private void generateAllureAttachments() {
        logger.info("📋 Generating Allure report attachments...\n");

        // User Wallet Report
        StringBuilder userReport = new StringBuilder();
        userReport.append("═══════════════════════════════════════════════════════════\n");
        userReport.append("                   USER WALLET REPORT\n");
        userReport.append("═══════════════════════════════════════════════════════════\n\n");

        for (TestMetrics.UserMetrics user : metrics.getUserMetrics()) {
            userReport.append(String.format("User: %s (%s)\n", user.getUsername(), user.getEmail()));
            userReport.append(String.format("  Initial Balance:     $%,.2f\n", user.getInitialBalance()));
            userReport.append(String.format("  Current Balance:     $%,.2f\n", user.getCurrentBalance()));
            userReport.append(String.format("  Utilized Amount:     $%,.2f\n", user.getUtilizedAmount()));
            userReport.append(String.format("  Successful Bookings: %d\n", user.getSuccessfulBookings()));
            userReport.append(String.format("  Failed Bookings:     %d\n", user.getFailedBookings()));
            userReport.append("\n  Bookings:\n");

            for (TestMetrics.BookingDetail booking : user.getBookings()) {
                userReport.append(String.format("    - [%s] Spot: %s, Amount: $%.2f, Vehicle: %s\n",
                    booking.getStatus(), booking.getSpotId(), booking.getAmount(), booking.getVehicleNumber()));
                if (booking.getFailureReason() != null) {
                    userReport.append(String.format("      Reason: %s\n", booking.getFailureReason()));
                }
            }
            userReport.append("\n");
        }

        Allure.addAttachment("User Wallet Report", "text/plain", userReport.toString());

        // Parking Lot Report
        StringBuilder lotReport = new StringBuilder();
        lotReport.append("═══════════════════════════════════════════════════════════\n");
        lotReport.append("                PARKING LOT UTILIZATION REPORT\n");
        lotReport.append("═══════════════════════════════════════════════════════════\n\n");

        for (TestMetrics.ParkingLotMetrics lot : metrics.getParkingLotMetrics()) {
            lotReport.append(String.format("Parking Lot: %s\n", lot.getLotName()));
            lotReport.append(String.format("  Total Spots:     %d\n", lot.getTotalSpots()));
            lotReport.append(String.format("  Total Capacity:  %d bookings\n", lot.getTotalCapacity()));
            lotReport.append(String.format("  Booked:          %d bookings\n", lot.getBookedSpots()));
            lotReport.append(String.format("  Available:       %d bookings\n", lot.getAvailableSpots()));
            lotReport.append(String.format("  Utilization:     %.1f%%\n",
                (lot.getBookedSpots() * 100.0 / lot.getTotalCapacity())));
            lotReport.append("\n  Spots:\n");

            for (TestMetrics.SpotDetail spot : lot.getSpots()) {
                lotReport.append(String.format("    - %s (%s): Capacity: %d, Booked: %d, Available: %d, Rate: $%.2f/hr\n",
                    spot.getSpotId(), spot.getZoneName(), spot.getCapacity(),
                    spot.getBooked(), spot.getAvailable(), spot.getBookingRate()));
            }
            lotReport.append("\n");
        }

        Allure.addAttachment("Parking Lot Report", "text/plain", lotReport.toString());

        // Financial Reconciliation Report
        StringBuilder financeReport = new StringBuilder();
        financeReport.append("═══════════════════════════════════════════════════════════\n");
        financeReport.append("             FINANCIAL RECONCILIATION REPORT\n");
        financeReport.append("═══════════════════════════════════════════════════════════\n\n");
        financeReport.append(String.format("Total Users:               %d\n", metrics.getTotalUsers()));
        financeReport.append(String.format("Total Initial Balance:     $%,.2f\n", metrics.getTotalInitialBalance()));
        financeReport.append(String.format("Total Current Balance:     $%,.2f\n", metrics.getTotalCurrentBalance()));
        financeReport.append(String.format("Total Utilized:            $%,.2f\n", metrics.getTotalUtilizedAmount()));
        financeReport.append(String.format("Total Expected Revenue:    $%,.2f\n\n", metrics.getTotalExpectedRevenue()));

        financeReport.append("Balance Verification:\n");
        double expectedRemaining = metrics.getTotalInitialBalance() - metrics.getTotalUtilizedAmount();
        financeReport.append(String.format("  Expected Remaining:      $%,.2f\n", expectedRemaining));
        financeReport.append(String.format("  Actual Remaining:        $%,.2f\n", metrics.getTotalCurrentBalance()));
        financeReport.append(String.format("  Difference:              $%,.2f\n",
            Math.abs(expectedRemaining - metrics.getTotalCurrentBalance())));
        financeReport.append(String.format("  Status:                  %s\n\n",
            Math.abs(expectedRemaining - metrics.getTotalCurrentBalance()) < 0.01 ? "✅ MATCHED" : "❌ MISMATCH"));

        Allure.addAttachment("Financial Reconciliation", "text/plain", financeReport.toString());

        // Summary Report
        StringBuilder summaryReport = new StringBuilder();
        summaryReport.append("═══════════════════════════════════════════════════════════\n");
        summaryReport.append("                     SUMMARY REPORT\n");
        summaryReport.append("═══════════════════════════════════════════════════════════\n\n");
        summaryReport.append("Infrastructure:\n");
        summaryReport.append(String.format("  Parking Lots:            %d\n", metrics.getTotalParkingLots()));
        summaryReport.append(String.format("  Parking Spots:           %d\n", metrics.getTotalParkingSpots()));
        summaryReport.append(String.format("  Total Capacity:          %d bookings\n\n", metrics.getTotalCapacity()));

        summaryReport.append("Booking Statistics:\n");
        summaryReport.append(String.format("  Total Attempts:          %d\n", metrics.getTotalBookingAttempts()));
        summaryReport.append(String.format("  Successful:              %d (%.1f%%)\n",
            metrics.getTotalSuccessfulBookings(),
            metrics.getTotalBookingAttempts() > 0 ? (metrics.getTotalSuccessfulBookings() * 100.0 / metrics.getTotalBookingAttempts()) : 0));
        summaryReport.append(String.format("  Failed:                  %d (%.1f%%)\n",
            metrics.getTotalFailedBookings(),
            metrics.getTotalBookingAttempts() > 0 ? (metrics.getTotalFailedBookings() * 100.0 / metrics.getTotalBookingAttempts()) : 0));
        summaryReport.append(String.format("  Cancelled:               %d\n\n", metrics.getTotalCancelledBookings()));

        summaryReport.append("Validation Status:\n");
        if (metrics.getValidationResult().isValid()) {
            summaryReport.append("  ✅ ALL CHECKS PASSED\n");
        } else {
            summaryReport.append("  ❌ VALIDATION ERRORS:\n");
            for (String error : metrics.getValidationResult().getErrors()) {
                summaryReport.append(String.format("     - %s\n", error));
            }
        }
        if (!metrics.getValidationResult().getWarnings().isEmpty()) {
            summaryReport.append("\n  ⚠️  WARNINGS:\n");
            for (String warning : metrics.getValidationResult().getWarnings()) {
                summaryReport.append(String.format("     - %s\n", warning));
            }
        }

        Allure.addAttachment("Summary Report", "text/plain", summaryReport.toString());

        logger.info("   ✅ Allure attachments generated\n");
    }

    private void printDetailedReport() {
        logger.info("\n\n");
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("            COMPREHENSIVE TEST EXECUTION REPORT");
        logger.info("═══════════════════════════════════════════════════════════\n");

        logger.info("📊 INFRASTRUCTURE SUMMARY");
        logger.info("─────────────────────────────────────────────────────────");
        logger.info("  Parking Lots:     {}", metrics.getTotalParkingLots());
        logger.info("  Parking Spots:    {}", metrics.getTotalParkingSpots());
        logger.info("  Total Capacity:   {} bookings\n", metrics.getTotalCapacity());

        logger.info("👥 USER SUMMARY");
        logger.info("─────────────────────────────────────────────────────────");
        logger.info("  Total Users:      {}", metrics.getTotalUsers());
        logger.info("  Initial Balance:  ${}", String.format("%,.2f", metrics.getTotalInitialBalance()));
        logger.info("  Current Balance:  ${}", String.format("%,.2f", metrics.getTotalCurrentBalance()));
        logger.info("  Utilized Amount:  ${}\n", String.format("%,.2f", metrics.getTotalUtilizedAmount()));

        logger.info("📈 BOOKING STATISTICS");
        logger.info("─────────────────────────────────────────────────────────");
        logger.info("  Total Attempts:   {}", metrics.getTotalBookingAttempts());
        logger.info("  Successful:       {} ({}%)",
            metrics.getTotalSuccessfulBookings(),
            metrics.getTotalBookingAttempts() > 0 ?
                String.format("%.1f", metrics.getTotalSuccessfulBookings() * 100.0 / metrics.getTotalBookingAttempts()) : "0.0");
        logger.info("  Failed:           {} ({}%)",
            metrics.getTotalFailedBookings(),
            metrics.getTotalBookingAttempts() > 0 ?
                String.format("%.1f", metrics.getTotalFailedBookings() * 100.0 / metrics.getTotalBookingAttempts()) : "0.0");
        logger.info("  Cancelled:        {}\n", metrics.getTotalCancelledBookings());

        logger.info("💰 FINANCIAL RECONCILIATION");
        logger.info("─────────────────────────────────────────────────────────");
        double expectedRemaining = metrics.getTotalInitialBalance() - metrics.getTotalUtilizedAmount();
        logger.info("  Expected Remaining: ${}", String.format("%,.2f", expectedRemaining));
        logger.info("  Actual Remaining:   ${}", String.format("%,.2f", metrics.getTotalCurrentBalance()));
        logger.info("  Difference:         ${}",
            String.format("%,.2f", Math.abs(expectedRemaining - metrics.getTotalCurrentBalance())));
        logger.info("  Status:             {}\n",
            Math.abs(expectedRemaining - metrics.getTotalCurrentBalance()) < 0.01 ? "✅ MATCHED" : "❌ MISMATCH");

        logger.info("✅ VALIDATION RESULTS");
        logger.info("─────────────────────────────────────────────────────────");
        if (metrics.getValidationResult().isValid()) {
            logger.info("  ✅ All validation checks PASSED");
            logger.info("     - Wallet balances reconciled");
            logger.info("     - Booking counts within capacity");
            logger.info("     - Financial data consistent");
        } else {
            logger.info("  ❌ Validation FAILED:");
            metrics.getValidationResult().getErrors().forEach(error ->
                logger.info("     - {}", error));
        }

        if (!metrics.getValidationResult().getWarnings().isEmpty()) {
            logger.info("\n  ⚠️  Warnings:");
            metrics.getValidationResult().getWarnings().forEach(warning ->
                logger.info("     - {}", warning));
        }

        logger.info("\n═══════════════════════════════════════════════════════════");
        logger.info("                   REPORT COMPLETE");
        logger.info("═══════════════════════════════════════════════════════════\n");
    }

    public TestMetrics getMetrics() {
        return metrics;
    }
}

