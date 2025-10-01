package com.parking.app.service;

import com.parking.app.model.Wallet;
import com.parking.app.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import java.util.Optional;

@Service
public class PaymentGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);

    @Autowired
    private WalletRepository walletRepository;

    // Initiate payment (Razorpay example)
    public String initiatePayment(String userId, double amount) {
        try {
            // Use Razorpay SDK or REST API here
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(amount * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "wallet_recharge_" + userId);

            // Call Razorpay API to create order (pseudo-code)
            // RazorpayClient razorpay = new RazorpayClient("api_key", "api_secret");
            // Order order = razorpay.Orders.create(orderRequest);

            // Return order ID to frontend for payment
            String orderId = "order_xyz"; // Replace with actual order.get("id")
            return orderId;
        } catch (Exception e) {
            logger.error("Payment initiation failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Payment initiation failed");
        }
    }

    // Handle payment callback/webhook
    public boolean handlePaymentCallback(String orderId, String paymentId, boolean success, String userId, double amount) {
        try {
            if (success) {
                Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
                if (walletOpt.isPresent()) {
                    Wallet wallet = walletOpt.get();
                    wallet.setBalance(wallet.getBalance() + amount);
                    wallet.setLastUpdated(new java.util.Date());
                    walletRepository.save(wallet);
                    logger.info("Wallet recharged for user {} via payment {}", userId, paymentId);
                    return true;
                }
            } else {
                logger.warn("Payment failed for user {}: order {}", userId, orderId);
            }
        } catch (Exception e) {
            logger.error("Error handling payment callback: {}", e.getMessage());
        }
        return false;
    }
}
