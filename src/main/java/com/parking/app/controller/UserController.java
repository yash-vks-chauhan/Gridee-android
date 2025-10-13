package com.parking.app.controller;

import com.parking.app.config.JwtUtil;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private  UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    // Register user - create with validation and hashing handled in service

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Users user) {
        try {
            String parkingLotName = user.getParkingLotName();
            Users createdUser = userService.createUser(user, parkingLotName);
            String token = jwtUtil.generateToken(createdUser.getId(), createdUser.getRole().name());
            Map<String, Object> response = Map.of(
                    "token", token,
                    "id", createdUser.getId(),
                    "name", createdUser.getName(),
                    "role", createdUser.getRole().name()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }



    // Login user - authenticate by email/phone and password (accepts JSON)
    // src/main/java/com/parking/app/controller/UserController.java


    @PutMapping("/{userId}/vehicles")
    public ResponseEntity<?> addUserVehicles(@PathVariable String userId, @RequestBody List<String> vehicleNumbers) {
        try {
            Users updatedUser = userService.addUserVehicles(userId, vehicleNumbers);
            if (updatedUser == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            return ResponseEntity.ok(updatedUser.getVehicleNumbers());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }



    // Get all users
    @GetMapping
    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        Users user = userService.getUserById(id);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        return ResponseEntity.ok(user);
    }
    @GetMapping("/{userId}/vehicles")
    public ResponseEntity<List<String>> getUserVehicles(@PathVariable String userId) {
        Users user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user.getVehicleNumbers());
    }

    // Update user by ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody Users userDetails) {
        try {
            Users updatedUser = userService.updateUser(id, userDetails);
            if (updatedUser == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    // Delete user by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
// In UserController.java



}
