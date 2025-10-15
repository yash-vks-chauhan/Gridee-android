package com.parking.app.controller;

import com.parking.app.dto.UserRequestDto;
import com.parking.app.dto.UserResponseDto;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Get all users - returns DTOs without sensitive data
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<Users> users = userService.getAllUsers();
        List<UserResponseDto> userDtos = users.stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    // Get user by ID - returns DTO without sensitive data
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable String id) {
        Users user = userService.findById(id).orElseThrow(()-> new NotFoundException("User not found with id: " + id));
        UserResponseDto userDto = UserResponseDto.fromEntity(user);
        return ResponseEntity.ok(userDto);
    }

    // Update user by ID - accepts DTO and returns DTO
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @Valid @RequestBody UserRequestDto userRequest) {
        Users updatedUser = userService.updateUser(id, userRequest);
        if (updatedUser == null) {
            throw new NotFoundException("User not found with id: " + id);
        }
        return ResponseEntity.ok("Updated Successfully");
    }

    // Delete user by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @PutMapping("/{userId}/add-vehicles")
    public ResponseEntity<Map<String,Object>> addUserVehicles(@PathVariable String userId, @RequestBody List<String> vehicleNumbers) {
        Users updatedUser = userService.addUserVehicles(userId, vehicleNumbers);
        if (updatedUser == null) {
            throw new NotFoundException("User not found with id: " + userId);
        }
        return ResponseEntity.ok(Map.of(
                "vehicleNumbers", updatedUser.getVehicleNumbers(),
                "message", "Vehicles added successfully"
        ));
    }
}
