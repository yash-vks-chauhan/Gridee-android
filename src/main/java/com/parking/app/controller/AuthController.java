package com.parking.app.controller;

import com.parking.app.config.JwtUtil;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        Map<String, Object> response = Map.of(
                "token", token,
                "id", user.getId(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "parkingLotId", user.getParkingLotId(),
                "parkingLotName", user.getParkingLotName(),
                "vehiclenumbers", user.getVehicleNumbers()
        );
        return ResponseEntity.ok(response);
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Users user) {
        try {
            String parkingLotName = user.getParkingLotName();
            Users createdUser = userService.createUser(user, parkingLotName);
            String token = jwtUtil.generateToken(createdUser.getId(), createdUser.getRole().name());
            System.out.println(token);
            Map<String, Object> response = Map.of(
                    "token", token,
                    "id", createdUser.getId(),
                    "name", createdUser.getName(),
                    "role", createdUser.getRole().name(),
                    "parkingLotName", createdUser.getParkingLotName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }
}
