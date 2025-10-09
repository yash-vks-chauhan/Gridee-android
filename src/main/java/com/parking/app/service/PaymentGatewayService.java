package com.parking.app.service;

import com.parking.app.model.Wallet;
import com.parking.app.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import java.util.Optional;

// Uncomment if you add Razorpay SDK to your build.gradle
// import com.razorpay.Order;
// import com.razorpay.RazorpayClient;

@Service
public class PaymentGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);

    @Autowired
    private WalletRepository walletRepository;

    @Value("${razorpay.key:}")
    private String razorpayKey;

    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    // Initiate payment (Razorpay or mock)
    public String initiatePayment(String userId, double amount) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(amount * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "wallet_recharge_" + userId);

            // If credentials are set, use Razorpay SDK
            if (razorpayKey != null && !razorpayKey.isEmpty() && razorpaySecret != null && !razorpaySecret.isEmpty()) {
                // Uncomment after adding Razorpay SDK
                // RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);
                // Order order = razorpay.Orders.create(orderRequest);
                // String orderId = order.get("id");
                // return orderId;
                // For now, just log and fall through to mock
                logger.info("Razorpay credentials found, but SDK code is commented for now.");
            }

            // Mock order for local/test
            String orderId = "order_test_" + System.currentTimeMillis();
            logger.info("Mock payment order created for user {}: {}", userId, orderId);
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
