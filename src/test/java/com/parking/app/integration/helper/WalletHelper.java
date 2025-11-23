package com.parking.app.integration.helper;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.restassured.RestAssured.given;

/**
 * Helper class for wallet operations
 */
public class WalletHelper {

    private static final Logger logger = LoggerFactory.getLogger(WalletHelper.class);
    private final RequestSpecification requestSpec;
    private final Random random = new Random();

    public WalletHelper(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @Step("Top up wallet for user: {userId}")
    public double topUpWallet(String userId, String token) {
        // Generate random amount between 500 and 1000 to ensure sufficient balance
        double amount = 500 + (random.nextDouble() * 500);
        amount = Math.round(amount * 100.0) / 100.0; // Round to 2 decimal places

        return topUpWallet(userId, amount, token);
    }

    @Step("Top up wallet for user: {userId} with amount: {amount}")
    public double topUpWallet(String userId, double amount, String token) {
        Map<String, Double> topUpRequest = new HashMap<>();
        topUpRequest.put("amount", amount);

        logger.info("💰 Topping up wallet for user: {} with amount: {}", userId, amount);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .body(topUpRequest)
            .when()
            .post("/api/users/" + userId + "/wallet/topup")
            .then()
            .extract().response();

        if (response.statusCode() == 200) {
            double balance = response.jsonPath().getDouble("balance");
            logger.info("✅ Wallet topped up successfully. New balance: {}", balance);
            return balance;
        } else {
            logger.error("❌ Failed to top up wallet for user: {} - Status: {}, Body: {}",
                userId, response.statusCode(), response.asString());
            throw new RuntimeException("Failed to top up wallet for user: " + userId);
        }
    }

    @Step("Get wallet for user: {userId}")
    public Response getWallet(String userId, String token) {
        logger.info("📊 Getting wallet for user: {}", userId);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/users/" + userId + "/wallet")
            .then()
            .extract().response();

        if (response.statusCode() == 200) {
            double balance = response.jsonPath().getDouble("balance");
            logger.info("✅ Wallet retrieved. Balance: {}", balance);
        } else {
            logger.error("❌ Failed to get wallet for user: {} - Status: {}", userId, response.statusCode());
        }

        return response;
    }
}
