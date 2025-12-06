package com.parking.app.integration.helper;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper class for booking operations
 */
public class BookingHelper {

    private static final Logger logger = LoggerFactory.getLogger(BookingHelper.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private final RequestSpecification requestSpec;

    public BookingHelper(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @Step("Create booking for user: {userId}, spot: {spotId}")
    public Response createBooking(String userId, String spotId, String lotId,
                                  LocalDateTime checkIn, LocalDateTime checkOut,
                                  String vehicleNumber, String token) {
        Map<String, String> bookingRequest = new HashMap<>();
        bookingRequest.put("spotId", spotId);
        bookingRequest.put("lotId", lotId);
        bookingRequest.put("checkInTime", checkIn.format(DATE_FORMAT));
        bookingRequest.put("checkOutTime", checkOut.format(DATE_FORMAT));
        bookingRequest.put("vehicleNumber", vehicleNumber);

        logger.debug("📅 Creating booking: userId={}, spot={}, time={} to {}",
            userId, spotId, checkIn.format(DATE_FORMAT), checkOut.format(DATE_FORMAT));

        // Validate userId before making the API call
        if (userId == null || userId.isEmpty()) {
            logger.error("❌ userId is NULL or empty! Cannot create booking.");
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .body(bookingRequest)
            .when()
            .post("/api/bookings/" + userId + "/create")
            .then()
            .extract().response();

        if (response.statusCode() == 200) {
            String bookingId = response.jsonPath().getString("id");
            logger.debug("✅ Booking created: {} for user {}", bookingId, userId);
        } else if (response.statusCode() == 409) {
            logger.debug("⚠️  Booking conflict for user {} on spot {}", userId, spotId);
        } else {
            logger.debug("❌ Booking failed for user {}: status={}, body={}", userId, response.statusCode(), response.asString());
        }

        return response;
    }

    @Step("Cancel booking: {bookingId}")
    public Response cancelBooking(String userId, String bookingId, String token) {
        logger.info("❌ Cancelling booking: {} for user {}", bookingId, userId);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .when()
            .post("/api/bookings/" + userId + "/" + bookingId + "/cancel")
            .then()
            .extract().response();

        if (response.statusCode() == 204 || response.statusCode() == 200) {
            logger.info("✅ Booking cancelled: {}", bookingId);
        } else {
            logger.error("❌ Failed to cancel booking: {} - Status: {}", bookingId, response.statusCode());
        }

        return response;
    }

    @Step("Get user bookings: {userId}")
    public Response getUserBookings(String userId, String token) {
        return given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/bookings/" + userId + "/all")
            .then()
            .extract().response();
    }
}
