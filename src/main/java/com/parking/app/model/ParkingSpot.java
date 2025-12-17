package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_spots")
@Getter
@Setter
public class ParkingSpot {

    // Field name constants for MongoDB queries
    public static final String FIELD_ID = "_id";
    public static final String FIELD_LOT_NAME = "lotName";
    public static final String FIELD_ZONE_NAME = "zoneName";
    public static final String FIELD_CAPACITY = "capacity";
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_BOOKING_RATE = "bookingRate";
    public static final String FIELD_CHECK_IN_PENALTY_RATE = "checkInPenaltyRate";
    public static final String FIELD_CHECK_OUT_PENALTY_RATE = "checkOutPenaltyRate";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ACTIVE = "active";

    @Id
    private String id;

    private String lotName;// Reference to ParkingLot
    private String lotId;
    private String zoneName;    // Zone like "TP Avenue Parking"
    private int capacity;       // Total slots in this zone
    private int available;      // Current available slots
    private String status;// available | held | active
    private double bookingRate;         // Rate per hour or unit
    private double checkInPenaltyRate;  // Penalty rate per minute for late check-in
    private double checkOutPenaltyRate;
    private String description; // Optional description
    private boolean active;    // Is the spot active

    public ParkingSpot() {}
}
