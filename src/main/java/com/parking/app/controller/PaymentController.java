package com.parking.app.controller;

import com.parking.app.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    // Endpoint to initiate payment
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            double amount = Double.parseDouble(request.get("amount").toString());
            String orderId = paymentGatewayService.initiatePayment(userId, amount);
            return ResponseEntity.ok(Map.of("orderId", orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint to handle payment callback/webhook
    @PostMapping("/callback")
    public ResponseEntity<?> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        try {
            String orderId = (String) payload.get("orderId");
            String paymentId = (String) payload.get("paymentId");
            boolean success = Boolean.parseBoolean(payload.get("success").toString());
            String userId = (String) payload.get("userId");
            double amount = Double.parseDouble(payload.get("amount").toString());

            boolean result = paymentGatewayService.handlePaymentCallback(orderId, paymentId, success, userId, amount);
            if (result) {
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                return ResponseEntity.status(400).body(Map.of("status", "failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
