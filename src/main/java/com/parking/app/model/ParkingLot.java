package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_lots")
@Getter
@Setter
public class ParkingLot {

    // Field name constants for MongoDB queries
    public static final String FIELD_ID = "_id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_LOCATION = "location";
    public static final String FIELD_TOTAL_SPOTS = "totalSpots";
    public static final String FIELD_AVAILABLE_SPOTS = "availableSpots";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_LATITUDE = "latitude";
    public static final String FIELD_LONGITUDE = "longitude";
    public static final String FIELD_ACTIVE = "active";

    @Id
    private String id;

    private String name;
    private String location;
    private int totalSpots;
    private int availableSpots;
    private String address;
    private double latitude;
    private double longitude;
    private boolean active;

    public ParkingLot() {}
}
