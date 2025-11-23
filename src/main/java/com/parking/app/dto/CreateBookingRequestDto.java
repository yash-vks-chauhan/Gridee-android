package com.parking.app.dto;

import lombok.Data;

@Data
public class CreateBookingRequestDto {

    private String spotId;
    private String checkInTime;
    private String checkOutTime;
    private String vehicleNumber;
}
