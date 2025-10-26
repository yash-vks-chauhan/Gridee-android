package com.parking.app.controller;

import com.parking.app.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    // calls gors to razorpay

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    // Endpoint to initiate payment
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        Object amountObj = payload.get("amount");

        if (userId == null || amountObj == null) {
            throw new IllegalArgumentException("userId and amount are required");
        }

        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            amount = Double.parseDouble(amountObj.toString());
        }
        String orderId = paymentGatewayService.initiatePayment(userId, amount);
        return ResponseEntity.ok(Map.of("orderId", orderId));
    }

    // Endpoint to handle payment callback/webhook
    @PostMapping("/callback")
    public ResponseEntity<?> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        String orderId = (String) payload.get("orderId");
        String paymentId = (String) payload.get("paymentId");
        Object successObj = payload.get("success");
        String userId = (String) payload.get("userId");
        Object amountObj = payload.get("amount");

        if (orderId == null || paymentId == null || successObj == null || userId == null || amountObj == null) {
            throw new IllegalArgumentException("orderId, paymentId, success, userId, and amount are required");
        }

        boolean success;
        if (successObj instanceof Boolean) {
            success = (Boolean) successObj;
        } else {
            success = Boolean.parseBoolean(successObj.toString());
        }

        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            amount = Double.parseDouble(amountObj.toString());
        }

        boolean result = paymentGatewayService.handlePaymentCallback(orderId, paymentId, success, userId, amount);
        if (result) {
            return ResponseEntity.ok(Map.of("status", "success"));
        } else {
            throw new com.parking.app.exception.IllegalStateException("Payment callback failed");
        }
    }
}
