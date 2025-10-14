package com.parking.app.constants;

/**
 * Constants for JWT Authentication Filter
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new IllegalStateException("Constants class");
    }

    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = 7;

    // Public endpoints that don't require authentication
    public static final String[] PUBLIC_ENDPOINTS = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/parking-lots/list/by-names",
        "/error",
        "/actuator/health",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    // Error messages
    public static final String ERROR_INVALID_TOKEN = "Invalid or expired JWT token";
    public static final String ERROR_USER_NOT_FOUND = "User not found for token";
    public static final String ERROR_TOKEN_EXTRACTION_FAILED = "Failed to extract token claims";
}

