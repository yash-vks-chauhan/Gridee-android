package com.parking.app.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test data models for API requests/responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestParkingLot {
    private String id;
    private String name;
    private String location;
    private int totalSpots;
    private int availableSpots;
    private String address;
    private double latitude;
    private double longitude;
    private boolean active;
}

