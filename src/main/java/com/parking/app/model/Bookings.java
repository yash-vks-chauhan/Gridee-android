package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "bookings")
@Getter
@Setter
public class Bookings {

    @Id
    private String id;

    private String userId;
    private String lotId;
    private String spotId;
    private String status;     // e.g., "pending", "active", "cancelled"
    private double amount;     // optional payment amount
    private String qrCode;     // optional QR code for parking entry
    private Date checkInTime;
    private Date checkOutTime;
    private Date createdAt;
    private String vehicleNumber;   // just the one picked for this booking
    private boolean qrCodeScanned;
    private Date actualCheckInTime;
    private Boolean autoCompleted;

    public Bookings() {
        this.status = "pending";  // initialize status as pending
        this.createdAt = new Date();  // initialize createdAt as now
    }
}
