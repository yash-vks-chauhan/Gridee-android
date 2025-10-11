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

                    // Record transaction
                    Transactions transaction = new Transactions();
                    transaction.setUserId(userId);
                    transaction.setGateway("razorpay");
                    transaction.setGatewayOrderId(orderId);
                    transaction.setGatewayPaymentId(paymentId);
                    transaction.setAmount(amount);
                    transaction.setCurrency("INR");
                    transaction.setStatus("success");
                    transaction.setMethod("wallet_recharge");
                    transaction.setTimestamp(new java.util.Date());
                    transactionService.recordTransaction(transaction);

                    return true;
                }
            } else {
                // Record failed transaction
                Transactions transaction = new Transactions();
                transaction.setUserId(userId);
                transaction.setGateway("razorpay");
                transaction.setGatewayOrderId(orderId);
                transaction.setGatewayPaymentId(paymentId);
                transaction.setAmount(amount);
                transaction.setCurrency("INR");
                transaction.setStatus("failed");
                transaction.setMethod("wallet_recharge");
                transaction.setTimestamp(new java.util.Date());
                transactionService.recordTransaction(transaction);

                logger.warn("Payment failed for user {}: order {}", userId, orderId);
            }
        } catch (Exception e) {
            logger.error("Error handling payment callback: {}", e.getMessage());
        }
        return false;
    }
}
