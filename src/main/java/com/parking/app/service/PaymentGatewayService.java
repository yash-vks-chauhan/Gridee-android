package com.parking.app.service;

import com.parking.app.model.Wallet;
import com.parking.app.model.Transactions;
import com.parking.app.repository.WalletRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Value("${razorpay.key:}")
    private String razorpayKey;

    @Value("${razorpay.secret:}")
    private String razorpaySecret;

    // Initiate payment (Razorpay or mock)

    public String initiatePayment(String userId, double amount) {
        try {
            if (razorpayKey == null || razorpayKey.isEmpty() || razorpaySecret == null || razorpaySecret.isEmpty()) {
                logger.error("Razorpay credentials are missing");
                throw new RuntimeException("Razorpay credentials are missing");
            }
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(amount * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "wallet_recharge_" + userId);

            RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);
            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get("id");
            logger.info("Razorpay order created for user {}: {}", userId, orderId);
            return orderId;
        } catch (Exception e) {
            logger.error("Payment initiation failed for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Payment initiation failed");
        }
    }


    // Handle payment callback/webhook
    public boolean handlePaymentCallback(String orderId, String paymentId, boolean success, String userId, double amount, String status) {
        try {
            // Idempotency + consolidation:
            // If any transaction already exists for this payment/order, update that record instead of creating new.
            com.parking.app.model.Transactions existing = null;
            try {
                if (paymentId != null && !paymentId.isEmpty()) {
                    java.util.List<com.parking.app.model.Transactions> list = transactionService.getTransactionsByGatewayPaymentId(paymentId);
                    if (list != null && !list.isEmpty()) existing = list.get(0);
                }
                if (existing == null && orderId != null && !orderId.isEmpty()) {
                    java.util.List<com.parking.app.model.Transactions> list = transactionService.getTransactionsByGatewayOrderId(orderId);
                    if (list != null && !list.isEmpty()) existing = list.get(0);
                }
            } catch (Exception e) {
                logger.warn("Lookup existing transaction failed: {}", e.getMessage());
            }

            String normalizedFailure = (status != null && !status.isEmpty()) ? status.toLowerCase() : "failed";
            if (!normalizedFailure.equals("failed") && !normalizedFailure.equals("cancelled") && !normalizedFailure.equals("canceled")) {
                normalizedFailure = "failed";
            }

            if (existing != null) {
                // Update in place
                existing.setGatewayOrderId(orderId);
                existing.setGatewayPaymentId(paymentId);
                existing.setAmount(amount);
                existing.setCurrency("INR");
                existing.setMethod("wallet_recharge");
                if (success) {
                    existing.setStatus("completed");
                } else {
                    if (!"completed".equalsIgnoreCase(existing.getStatus())) {
                        existing.setStatus(normalizedFailure);
                    }
                }
                transactionService.save(existing);
                return success;
            }
            if (success) {
                // Record a completed transaction; listener will update wallet balance
                logger.info("Payment success for user {} via payment {}. Recording transaction.", userId, paymentId);
                Transactions transaction = new Transactions();
                transaction.setType("wallet_topup");
                transaction.setUserId(userId);
                transaction.setGateway("razorpay");
                transaction.setGatewayOrderId(orderId);
                transaction.setGatewayPaymentId(paymentId);
                transaction.setAmount(amount);
                transaction.setCurrency("INR");
                transaction.setStatus("completed");
                transaction.setMethod("wallet_recharge");
                transaction.setTimestamp(new java.util.Date());
                // Set a stable reference id to help listener deduplicate
                try {
                    String ref = (paymentId != null && !paymentId.isEmpty()) ? paymentId
                            : (orderId != null && !orderId.isEmpty()) ? orderId
                            : java.util.UUID.randomUUID().toString();
                    transaction.setReferenceId(ref);
                } catch (Exception ignored) {}
                transactionService.recordTransaction(transaction);
                return true;
            } else {
                // Record failed transaction
                Transactions transaction = new Transactions();
                transaction.setType("wallet_topup");
                transaction.setUserId(userId);
                transaction.setGateway("razorpay");
                transaction.setGatewayOrderId(orderId);
                transaction.setGatewayPaymentId(paymentId);
                transaction.setAmount(amount);
                transaction.setCurrency("INR");
                String statusValue = (status != null && !status.isEmpty()) ? status.toLowerCase() : "failed";
                if (!statusValue.equals("failed") && !statusValue.equals("cancelled") && !statusValue.equals("canceled")) {
                    statusValue = "failed";
                }
                transaction.setStatus(statusValue);
                transaction.setMethod("wallet_recharge");
                transaction.setTimestamp(new java.util.Date());
                transactionService.recordTransaction(transaction);

                logger.warn("Payment {} for user {}: order {}", transaction.getStatus(), userId, orderId);
            }
        } catch (Exception e) {
            logger.error("Error handling payment callback: {}", e.getMessage());
        }
        return false;
    }
}
