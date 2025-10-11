package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_lots")
@Getter
@Setter
public class ParkingLot {

    @Id
    private String id;

    private String name;
    private String location;
    private int totalSpots;
    private int availableSpots;
    private String address;
    private double latitude;
    private double longitude;

    public ParkingLot() {}
}
