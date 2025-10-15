package com.parking.app.controller;

import com.parking.app.config.JwtUtil;
import com.parking.app.dto.AuthResponseDto;
import com.parking.app.dto.LoginRequestDto;
import com.parking.app.dto.UserRequestDto;
import com.parking.app.dto.UserResponseDto;
import com.parking.app.exception.IllegalStateException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Users;
import com.parking.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        Users user = userService.authenticate(request.getEmail(), request.getPassword());
        if (user == null) {
            throw new NotFoundException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        UserResponseDto userDto = UserResponseDto.fromEntity(user);
        AuthResponseDto response = AuthResponseDto.success(token, userDto, "Login successful");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRequestDto userRequest) {
        try {
            Users createdUser = userService.createUser(userRequest);
            String token = jwtUtil.generateToken(createdUser.getId(), createdUser.getRole());

            UserResponseDto userDto = UserResponseDto.fromEntity(createdUser);
            AuthResponseDto response = AuthResponseDto.success(token, userDto, "Registration successful");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid input",e);
        }
    }
}
