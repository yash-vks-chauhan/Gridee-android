package com.parking.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for partial user updates (profile/vehicles).
 * All fields are optional; validation applies only when values are provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;

    private String password;

    private List<String> vehicleNumbers;

    private String parkingLotName;
}
