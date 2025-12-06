package com.parking.app.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test user model with authentication token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestUser {
    private String userId;
    private String username;
    private String email;
    private String password;
    private String phoneNumber;
    private String bearerToken;
    private String lotId;

    public String getAuthHeader() {
        return "Bearer " + bearerToken;
    }
}

