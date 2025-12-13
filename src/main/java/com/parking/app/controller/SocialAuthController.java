package com.parking.app.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.parking.app.config.JwtUtil;
import com.parking.app.dto.AuthResponseDto;
import com.parking.app.dto.SocialSignInRequest;
import com.parking.app.dto.UserResponseDto;
import com.parking.app.model.Users;
import com.parking.app.service.GoogleTokenVerifier;
import com.parking.app.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles social sign-in flows (currently Google).
 */
@RestController
@RequestMapping("/api/users")
public class SocialAuthController {

    private static final Logger logger = LoggerFactory.getLogger(SocialAuthController.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public SocialAuthController(GoogleTokenVerifier googleTokenVerifier, UserService userService, JwtUtil jwtUtil) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/social-signin")
    public ResponseEntity<?> socialSignIn(@RequestBody SocialSignInRequest request) {
        if (request == null || !StringUtils.hasText(request.getProvider())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Provider is required");
        }
        if (!"google".equalsIgnoreCase(request.getProvider())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported provider");
        }

        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Google ID token");
        }

        String email = payload.getEmail();
        if (!StringUtils.hasText(email)) {
            email = request.getEmail();
        }
        if (!StringUtils.hasText(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
        }

        String name = StringUtils.hasText(request.getName()) ? request.getName() :
                (String) payload.get("name");

        try {
            Users user = userService.getActiveUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No account found for this email. Please register first with the same Google email.");
            }
            String jwt = jwtUtil.generateToken(user.getId(), user.getRole());
            UserResponseDto userDto = UserResponseDto.fromEntity(user);
            if (userDto.getPhone() == null) {
                userDto.setPhone("");
            }
            if (userDto.getVehicleNumbers() == null) {
                userDto.setVehicleNumbers(java.util.List.of());
            }
            AuthResponseDto response = AuthResponseDto.success(jwt, userDto, "Login successful");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Social sign-in failed validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Social sign-in failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Social sign-in failed");
        }
    }
}
