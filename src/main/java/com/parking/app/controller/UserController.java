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

    // Social sign-in (Google, Apple, etc.)
    @PostMapping("/social-signin")
    public ResponseEntity<?> socialSignIn(@RequestBody Map<String, String> credentials) {
        try {
            String provider = credentials.get("provider");
            String email = credentials.get("email");
            String name = credentials.get("name");
            
            // Validate required fields
            if (provider == null || email == null || name == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing required fields: provider, email, name"));
            }
            
            // Check if user exists by email
            Users user = userService.getUserByEmail(email);
            
            if (user == null) {
                // Create new user for social sign-in
                user = new Users();
                user.setName(name);
                user.setEmail(email.trim().toLowerCase());
                user.setRole(Users.Role.USER);
                user.setWalletCoins(0);
                user.setFirstUser(true);
                user.setCreatedAt(new java.util.Date());
                
                // Set a special password hash for social sign-in users
                // This prevents them from logging in with password
                user.setPasswordHash("SOCIAL_SIGNIN_" + provider.toUpperCase() + "_" + System.currentTimeMillis());
                
                // For Google Sign-In, we can store additional data
                if ("google".equalsIgnoreCase(provider)) {
                    String idToken = credentials.get("idToken");
                    String profilePicture = credentials.get("profilePicture");
                    // TODO: Verify Google ID token if needed
                    // TODO: Store profile picture URL in user profile if field added
                } 
                // For Apple Sign-In
                else if ("apple".equalsIgnoreCase(provider)) {
                    String authCode = credentials.get("authorizationCode");
                    // TODO: Verify Apple authorization code if needed
                }
                
                // Save the new social user
                user = userService.createSocialUser(user);
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
            
            // Return response with token (same format as registration)
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("role", user.getRole().name());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Social sign-in failed: " + e.getMessage()));
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
