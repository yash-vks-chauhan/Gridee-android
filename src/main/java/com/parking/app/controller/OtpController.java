package com.parking.app.controller;

import com.parking.app.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/generate")
    public String generateOtp(@RequestParam String key) {
        return otpService.generateOtp(key);
    }

    @PostMapping("/validate")
    public boolean validateOtp(@RequestParam String key, @RequestParam String otp) {
        return otpService.validateOtp(key, otp);
    }
}
