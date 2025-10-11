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
    private String status;      // available | held | active

    public ParkingSpot() {}
}
