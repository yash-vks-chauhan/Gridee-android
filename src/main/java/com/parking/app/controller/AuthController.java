package com.parking.app.controller;

import com.parking.app.config.JwtUtil;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Users user = userService.authenticate(request.getEmail(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        // Defensive defaults for older records that may have null fields
        String userId = user.getId();
        String name = user.getName();
        Users.Role role = user.getRole();

        if (role == null) {
            role = Users.Role.USER;
        }
        if (name == null || name.isBlank()) {
            // fallback to email or phone if name is missing
            name = user.getEmail() != null ? user.getEmail() : (user.getPhone() != null ? user.getPhone() : "User");
        }

        String token = jwtUtil.generateToken(userId, role.name());

        // Avoid Map.of (disallows null values) to prevent NPEs
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("token", token);
        response.put("id", userId);
        response.put("name", name);
        response.put("role", role.name());

        return ResponseEntity.ok(response);
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
