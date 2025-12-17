package com.parking.app.controller;

import com.parking.app.config.JwtUtil;
import com.parking.app.dto.AuthResponseDto;
import com.parking.app.dto.LoginRequestDto;
import com.parking.app.dto.UserRequestDto;
import com.parking.app.dto.UserResponseDto;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        Users user = userService.authenticate(request.getEmail(), request.getPassword());
        if (user == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        UserResponseDto userDto = UserResponseDto.fromEntity(user);
        AuthResponseDto response = AuthResponseDto.success(token, userDto, "Login successful");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequestDto userRequest) {
        Users createdUser = userService.createUser(userRequest);
        String token = jwtUtil.generateToken(createdUser.getId(), createdUser.getRole());

        UserResponseDto userDto = UserResponseDto.fromEntity(createdUser);
        AuthResponseDto response = AuthResponseDto.success(token, userDto, "Registration successful");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDto> googleLogin(@RequestBody Map<String, String> request) {
        String credential = request.get("credential");

        if (credential == null || credential.isEmpty()) {
            throw new BadCredentialsException("Google credential is required");
        }

        try {
            // Build Google ID token verifier
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            // Verify the Google ID token
            GoogleIdToken idToken = verifier.verify(credential);

            if (idToken == null) {
                throw new BadCredentialsException("Invalid Google credential");
            }

            // Extract verified payload
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            Boolean emailVerified = payload.getEmailVerified();

            // Verify email is verified
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new BadCredentialsException("Email not verified by Google");
            }

            // ✅ Check if user exists (for determining if new user)
            boolean isNewUser = !userService.existsByEmail(email);

            // Find or create user
            Users user = userService.findOrCreateGoogleUser(email, name, pictureUrl);

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            UserResponseDto userDto = UserResponseDto.fromEntity(user);

            // ✅ Use socialSignIn method instead of success
            String message = isNewUser ? "Account created with Google" : "Google login successful";
            AuthResponseDto authResponse = AuthResponseDto.socialSignIn(token, userDto, message, isNewUser);

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            throw new BadCredentialsException("Google authentication failed: " + e.getMessage());
        }
    }
}
