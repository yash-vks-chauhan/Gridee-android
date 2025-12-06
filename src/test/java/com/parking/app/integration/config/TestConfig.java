package com.parking.app.integration.config;

/**
 * Centralized configuration for integration tests
 * All test parameters can be modified here for different test scenarios
 */
public class TestConfig {

    // ==================== USER CONFIGURATION ====================
    public static final int TOTAL_TEST_USERS = 10;
    public static final int CONCURRENT_BOOKING_USERS = 10; // Should be <= TOTAL_TEST_USERS

    // ==================== PARKING LOT CONFIGURATION ====================
    public static final int TOTAL_PARKING_LOTS = 3;
    public static final int SPOTS_PER_LOT = 2;
    public static final int CAPACITY_PER_SPOT = 5;
    public static final int TOTAL_PARKING_CAPACITY = TOTAL_PARKING_LOTS * SPOTS_PER_LOT * CAPACITY_PER_SPOT;

    // ==================== FINANCIAL CONFIGURATION ====================
    public static final double MIN_WALLET_TOPUP = 500.0;
    public static final double MAX_WALLET_TOPUP = 1000.0;
    public static final double BOOKING_RATE_PER_HOUR = 50.0;

    // ==================== TIMING CONFIGURATION ====================
    public static final int BOOKING_DURATION_HOURS = 2;
    public static final int BOOKING_START_DELAY_HOURS = 2;
    public static final long RATE_LIMIT_DELAY_MS = 1000; // Delay to avoid 429 errors
    public static final int USERS_BATCH_SIZE = 10; // Process users in batches to avoid rate limiting

    // ==================== ADMIN CONFIGURATION ====================
    public static final String ADMIN_EMAIL = "admin@parking.com";
    public static final String ADMIN_PASSWORD = "Admin@123";

    // ==================== TEST USER DEFAULTS ====================
    public static final String TEST_USER_PASSWORD = "Test@123";
    public static final String TEST_USER_PHONE_PREFIX = "555000000";

    // ==================== PARKING LOT NAMES ====================
    public static final String[] PARKING_LOT_NAMES = {
        "Test Lot 1 - Downtown",
        "Test Lot 2 - Airport",
        "Test Lot 3 - Mall"
    };

    public static final String[] PARKING_LOT_LOCATIONS = {
        "Downtown District",
        "Airport Area",
        "Shopping Mall"
    };

    // ==================== VALIDATION ====================
    static {
        if (CONCURRENT_BOOKING_USERS > TOTAL_TEST_USERS) {
            throw new IllegalStateException(
                "CONCURRENT_BOOKING_USERS (" + CONCURRENT_BOOKING_USERS +
                ") cannot exceed TOTAL_TEST_USERS (" + TOTAL_TEST_USERS + ")"
            );
        }

        if (PARKING_LOT_NAMES.length != TOTAL_PARKING_LOTS) {
            throw new IllegalStateException(
                "PARKING_LOT_NAMES array size must match TOTAL_PARKING_LOTS"
            );
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get parking lot name by index
     */
    public static String getParkingLotName(int index) {
        if (index < 0 || index >= PARKING_LOT_NAMES.length) {
            throw new IllegalArgumentException("Invalid parking lot index: " + index);
        }
        return PARKING_LOT_NAMES[index];
    }

    /**
     * Get parking lot location by index
     */
    public static String getParkingLotLocation(int index) {
        if (index < 0 || index >= PARKING_LOT_LOCATIONS.length) {
            throw new IllegalArgumentException("Invalid parking lot index: " + index);
        }
        return PARKING_LOT_LOCATIONS[index];
    }

    /**
     * Generate test username
     */
    public static String getTestUsername(int userNum) {
        return "testuser_" + userNum;
    }

    /**
     * Generate test email
     */
    public static String getTestEmail(int userNum) {
        return getTestUsername(userNum) + "@test.com";
    }

    /**
     * Generate test phone number
     */
    public static String getTestPhone(int userNum) {
        return TEST_USER_PHONE_PREFIX + userNum;
    }

    /**
     * Get spot ID
     */
    public static String getSpotId(int lotIndex, int spotNum) {
        return "TEST-LOT" + (lotIndex + 1) + "-SPOT" + spotNum;
    }

    /**
     * Get zone name
     */
    public static String getZoneName(int spotNum) {
        return "Zone " + spotNum;
    }

    /**
     * Get vehicle number for test
     */
    public static String getVehicleNumber(int userIndex) {
        return "TEST" + String.format("%03d", userIndex);
    }

    /**
     * Print configuration summary
     */
    public static String getConfigurationSummary() {
        return String.format(
            "Test Configuration:\n" +
            "  Total Users: %d\n" +
            "  Concurrent Booking Users: %d\n" +
            "  Parking Lots: %d\n" +
            "  Spots per Lot: %d\n" +
            "  Capacity per Spot: %d\n" +
            "  Total Parking Capacity: %d\n" +
            "  Booking Rate: $%.2f/hour\n" +
            "  Booking Duration: %d hours\n" +
            "  Rate Limit Delay: %dms",
            TOTAL_TEST_USERS,
            CONCURRENT_BOOKING_USERS,
            TOTAL_PARKING_LOTS,
            SPOTS_PER_LOT,
            CAPACITY_PER_SPOT,
            TOTAL_PARKING_CAPACITY,
            BOOKING_RATE_PER_HOUR,
            BOOKING_DURATION_HOURS,
            RATE_LIMIT_DELAY_MS
        );
    }
}

