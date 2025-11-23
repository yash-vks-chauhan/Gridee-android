package com.parking.app.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestParkingSpot {
    private String id;
    private String lotName;  // Changed from lotId to lotName
    private String zoneName;
    private int capacity;
    private int available;
    private String status;
    private double bookingRate;
    private double checkInPenaltyRate;
    private double checkOutPenaltyRate;
    private String description;
    private boolean active;
}
