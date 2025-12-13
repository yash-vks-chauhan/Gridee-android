package com.parking.app.dto;

import com.parking.app.model.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parking.app.constants.Role;

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

    // Defensive getters to avoid nulls in JSON serialization
    public String getPhone() {
        return phone == null ? "" : phone;
    }

    public List<String> getVehicleNumbers() {
        return vehicleNumbers == null ? List.of() : vehicleNumbers;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public String getName() {
        return name == null ? getEmail() : name;
    }

    public String getRole() {
        return role == null ? Role.USER.name() : role;
    }

    public String getId() {
        return id == null ? "" : id;
    }

    /**
     * Factory method to create UserResponseDto from Users entity
     */
    public static UserResponseDto fromEntity(Users user) {
        if (user == null) {
            return null;
        }

        // Avoid nulls so clients with non-nullable models don't crash
        String safePhone = user.getPhone() != null ? user.getPhone() : "";
        List<String> safeVehicles = user.getVehicleNumbers() != null ? user.getVehicleNumbers() : List.of();
        String safeEmail = user.getEmail() != null ? user.getEmail() : "";
        String safeName = user.getName() != null ? user.getName() : safeEmail;
        String safeRole = user.getRole() != null ? user.getRole() : Role.USER.name();
        String safeId = user.getId() != null ? user.getId() : "";

        return UserResponseDto.builder()
                .id(safeId)
                .name(safeName)
                .email(safeEmail)
                .phone(safePhone)
                .vehicleNumbers(safeVehicles)
                .firstUser(user.isFirstUser())
                .walletCoins(user.getWalletCoins())
                .createdAt(user.getCreatedAt())
                .role(safeRole)
                .parkingLotId(user.getParkingLotId())
                .parkingLotName(user.getParkingLotName())
                .active(user.isActive())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
