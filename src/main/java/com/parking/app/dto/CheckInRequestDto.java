package com.parking.app.dto;

import com.parking.app.constants.CheckInMode;

public class CheckInRequestDto {

    private CheckInMode mode;  // Authentication mode: QR_CODE, VEHICLE_NUMBER, or PIN
    private String qrCode;
    private String vehicleNumber;
    private String pin;

    public CheckInRequestDto() {
    }

    public CheckInRequestDto(CheckInMode mode, String qrCode, String vehicleNumber, String pin) {
        this.mode = mode;
        this.qrCode = qrCode;
        this.vehicleNumber = vehicleNumber;
        this.pin = pin;
    }

    public CheckInMode getMode() {
        return mode;
    }

    public void setMode(CheckInMode mode) {
        this.mode = mode;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
