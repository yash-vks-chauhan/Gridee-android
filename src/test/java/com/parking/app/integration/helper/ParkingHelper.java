package com.parking.app.integration.helper;

import com.parking.app.integration.model.TestParkingLot;
import com.parking.app.integration.model.TestParkingSpot;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper class for parking lot and spot management
 */
public class ParkingHelper {

    private static final Logger logger = LoggerFactory.getLogger(ParkingHelper.class);
    private final RequestSpecification requestSpec;

    public ParkingHelper(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @Step("Create parking lot: {name}")
    public TestParkingLot createParkingLot(String name, String location, int totalSpots, String adminToken) {
        Map<String, Object> lotRequest = new HashMap<>();
        lotRequest.put("name", name);
        lotRequest.put("location", location);
        lotRequest.put("totalSpots", totalSpots);
        lotRequest.put("availableSpots", totalSpots);
        lotRequest.put("address", location + " Address");
        lotRequest.put("latitude", 37.7749 + Math.random());
        lotRequest.put("longitude", -122.4194 + Math.random());
        lotRequest.put("active", true);

        logger.info("🏢 Creating parking lot: {}", name);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + adminToken)
            .body(lotRequest)
            .when()
            .post("/api/parking-lots")
            .then()
            .extract().response();

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            String lotId = response.jsonPath().getString("id");
            logger.info("✅ Parking lot created: {} (ID: {})", name, lotId);

            return TestParkingLot.builder()
                .id(lotId)
                .name(name)
                .location(location)
                .totalSpots(totalSpots)
                .availableSpots(totalSpots)
                .active(true)
                .build();
        } else {
            logger.error("❌ Failed to create lot: {} - Status: {}, Body: {}",
                name, response.statusCode(), response.asString());
            throw new RuntimeException("Failed to create parking lot: " + name);
        }
    }

    @Step("Create parking spot: {spotId} in lot {lotName}")
    public TestParkingSpot createParkingSpot(String spotId, String lotName, String zoneName,
                                              int capacity, double bookingRate, String adminToken) {
        Map<String, Object> spotRequest = new HashMap<>();
        spotRequest.put("id", spotId);
        spotRequest.put("lotName", lotName);  // Changed from lotId to lotName
        spotRequest.put("zoneName", zoneName);
        spotRequest.put("capacity", capacity);
        spotRequest.put("available", capacity);
        spotRequest.put("status", "available");
        spotRequest.put("bookingRate", bookingRate);
        spotRequest.put("checkInPenaltyRate", 2.0);
        spotRequest.put("checkOutPenaltyRate", 5.0);
        spotRequest.put("description", "Test spot " + spotId);
        spotRequest.put("active", true);

        logger.info("🅿️  Creating parking spot: {} in lot {}", spotId, lotName);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + adminToken)
            .body(spotRequest)
            .when()
            .post("/api/parking-spots")
            .then()
            .extract().response();

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            logger.info("✅ Parking spot created: {}", spotId);

            return TestParkingSpot.builder()
                .id(spotId)
                .lotName(lotName)  // Changed from lotId to lotName
                .zoneName(zoneName)
                .capacity(capacity)
                .available(capacity)
                .bookingRate(bookingRate)
                .active(true)
                .build();
        } else {
            logger.error("❌ Failed to create spot: {} - Status: {}, Body: {}",
                spotId, response.statusCode(), response.asString());
            throw new RuntimeException("Failed to create parking spot: " + spotId);
        }
    }

    @Step("Get parking lot: {lotId}")
    public TestParkingLot getParkingLot(String lotId, String token) {
        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/parking-lots/" + lotId)
            .then()
            .extract().response();

        if (response.statusCode() == 200) {
            return TestParkingLot.builder()
                .id(response.jsonPath().getString("id"))
                .name(response.jsonPath().getString("name"))
                .totalSpots(response.jsonPath().getInt("totalSpots"))
                .availableSpots(response.jsonPath().getInt("availableSpots"))
                .build();
        } else {
            throw new RuntimeException("Failed to get parking lot: " + lotId);
        }
    }
}
