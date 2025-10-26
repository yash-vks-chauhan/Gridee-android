package com.parking.app.controller;

import com.parking.app.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key:}")
    private String razorpayKeyId;

    // Endpoint to initiate payment
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            Object amountObj = request.get("amount");
            if (userId == null || amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and amount are required"));
            }
            double amount;
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).doubleValue();
            } else {
                amount = Double.parseDouble(amountObj.toString());
            }
            String orderId = paymentGatewayService.initiatePayment(userId, amount);
            return ResponseEntity.ok(Map.of(
                    "orderId", orderId,
                    "keyId", razorpayKeyId
            ));
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
            Object successObj = payload.get("success");
            String userId = (String) payload.get("userId");
            Object amountObj = payload.get("amount");
            String status = payload.get("status") != null ? payload.get("status").toString() : null;

            if (orderId == null || paymentId == null || successObj == null || userId == null || amountObj == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "orderId, paymentId, success, userId, and amount are required"));
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

            boolean result = paymentGatewayService.handlePaymentCallback(orderId, paymentId, success, userId, amount, status);
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
