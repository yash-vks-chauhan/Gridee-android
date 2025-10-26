package com.parking.app.dto;

public class CreateBookingRequestDto {

    private String spotId;
    private String lotId;
    private String checkInTime;
    private String checkOutTime;
    private String vehicleNumber;

    public CreateBookingRequestDto() {
    }

    public CreateBookingRequestDto(String spotId, String lotId, String checkInTime,
                                   String checkOutTime, String vehicleNumber) {
        this.spotId = spotId;
        this.lotId = lotId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.vehicleNumber = vehicleNumber;
    }

    public String getSpotId() {
        return spotId;
    }

    public void setSpotId(String spotId) {
        this.spotId = spotId;
    }

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}
