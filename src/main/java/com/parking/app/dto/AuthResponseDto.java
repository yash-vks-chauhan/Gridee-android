package com.parking.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Authentication Response (Login/Register)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private UserResponseDto user;
    private String message;

    /**
     * Create successful authentication response with custom message
     */
    public static AuthResponseDto success(String token, UserResponseDto user, String message) {
        return AuthResponseDto.builder()
                .token(token)
                .tokenType("Bearer")
                .user(user)
                .message(message)
                .build();
    }
}

