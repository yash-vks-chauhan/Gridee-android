package com.parking.app.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String generateOtp(String key) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStorage.put(key, otp);
        return otp;
    }

    public boolean validateOtp(String key, String otp) {
        String storedOtp = otpStorage.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStorage.remove(key);
            return true;
        }
        return false;
    }

    public void clearOtp(String key) {
        otpStorage.remove(key);
    }
}
