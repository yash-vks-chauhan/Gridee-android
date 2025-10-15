package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_spots")
@Getter
@Setter
public class ParkingSpot {

    @Id
    private String id;

    private String lotId;       // Reference to ParkingLot
    private String zoneName;    // Zone like "TP Avenue Parking"
    private int capacity;       // Total slots in this zone
    private int available;      // Current available slots
    private String status;// available | held | active
    private double bookingRate;         // Rate per hour or unit
    private double checkInPenaltyRate;  // Penalty rate per minute for late check-in
    private double checkOutPenaltyRate;
    private String description; // Optional description

    public ParkingSpot() {}
}
