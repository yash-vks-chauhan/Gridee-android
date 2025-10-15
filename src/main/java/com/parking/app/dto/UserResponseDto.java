package com.parking.app.dto;

import com.parking.app.model.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * DTO for User Response
 * Excludes sensitive information like password hash
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private String id;
    private String name;
    private String email;
    private String phone;
    private List<String> vehicleNumbers;
    private boolean firstUser;
    private int walletCoins;
    private Date createdAt;
    private Date updatedAt;
    private String role;
    private String parkingLotId;
    private String parkingLotName;
    private boolean active;

    /**
     * Factory method to create UserResponseDto from Users entity
     */
    public static UserResponseDto fromEntity(Users user) {
        if (user == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .vehicleNumbers(user.getVehicleNumbers())
                .firstUser(user.isFirstUser())
                .walletCoins(user.getWalletCoins())
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                .parkingLotId(user.getParkingLotId())
                .parkingLotName(user.getParkingLotName())
                .active(user.isActive())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

