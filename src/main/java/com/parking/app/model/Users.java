// src/main/java/com/parking/app/model/Users.java
package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
public class Users {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private List<String> vehicleNumbers;
    private boolean firstUser;
    private int walletCoins;
    private Date createdAt;
    private String passwordHash;  // Store hashed password securely
    private Role role;
    private String parkingLotId;
    private String parkingLotName;// Add this field

    public Users() {
        this.createdAt = new Date();
        this.walletCoins = 0;
        this.firstUser = true;
        this.role = Role.USER;    // Default role
    }

    public enum Role {
        USER,
        ADMIN
    }
}
