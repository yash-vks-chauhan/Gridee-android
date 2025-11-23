package com.parking.app.integration.helper;

import com.parking.app.integration.model.TestUser;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Helper class for user authentication and registration
 */
public class AuthHelper {

    private static final Logger logger = LoggerFactory.getLogger(AuthHelper.class);
    private final RequestSpecification requestSpec;

    public AuthHelper(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    @Step("Register user: {username}")
    public TestUser registerUser(String username, String email, String password, String phoneNumber, String lotName) {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("name", username);
        registerRequest.put("email", email);
        registerRequest.put("password", password);
        registerRequest.put("phone", phoneNumber);
        registerRequest.put("parkingLotName", lotName);  // Changed from parkingLotId to parkingLotName

        logger.info("📝 Registering user: {} for parking lot: {}", username, lotName);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .body(registerRequest)
            .when()
            .post("/api/auth/register")
            .then()
            .extract().response();

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            // Extract userId from response - it's at 'id' field
            String userId = response.jsonPath().getString("user.id");
            logger.info("✅ User registered successfully: {} (ID: {})", username, userId);

            return TestUser.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .password(password)
                .phoneNumber(phoneNumber)
                .lotId(lotName)  // Store the lot name
                .build();
        } else {
            logger.error("❌ Failed to register user: {} - Status: {}, Body: {}",
                username, response.statusCode(), response.asString());
            throw new RuntimeException("Failed to register user: " + username);
        }
    }

    @Step("Login user: {username}")
    public String login(String username, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", username);  // API expects 'email' field, not 'username'
        loginRequest.put("password", password);

        logger.info("🔐 Logging in user: {}", username);

        Response response = given(requestSpec)
            .contentType("application/json")
            .accept("application/json")
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .extract().response();

        if (response.statusCode() == 200) {
            String token = response.jsonPath().getString("token");
            logger.info("✅ Login successful: {} (Token: {}...)", username, token.substring(0, Math.min(20, token.length())));
            return token;
        } else {
            logger.error("❌ Failed to login user: {} - Status: {}, Body: {}",
                username, response.statusCode(), response.asString());
            throw new RuntimeException("Failed to login user: " + username);
        }
    }

    @Step("Register and login user: {username}")
    public TestUser registerAndLogin(String username, String email, String password, String phoneNumber, String lotName) {
        TestUser user = registerUser(username, email, password, phoneNumber, lotName);
        String token = login(email, password);  // Use email for login, not username
        user.setBearerToken(token);
        logger.info("✅ User fully authenticated: {} with token", username);
        return user;
    }
}
