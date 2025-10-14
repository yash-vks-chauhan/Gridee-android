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

    // ===== Constants =====
    private static final String GATEWAY_NAME = "razorpay";
    private static final String CURRENCY_INR = "INR";
    private static final String PAYMENT_METHOD = "wallet_recharge";
    private static final String RECEIPT_PREFIX = "wallet_recharge_";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String ORDER_ID_KEY = "id";
    private static final String JSON_AMOUNT_KEY = "amount";
    private static final String JSON_CURRENCY_KEY = "currency";
    private static final String JSON_RECEIPT_KEY = "receipt";
    private static final int PAISE_MULTIPLIER = 100;

    // Error messages
    private static final String ERROR_MISSING_CREDENTIALS = "Razorpay credentials are missing";
    private static final String ERROR_PAYMENT_INITIATION_FAILED = "Payment initiation failed";

    // Log messages
    private static final String LOG_ORDER_CREATED = "Razorpay order created for user {}: {}";
    private static final String LOG_WALLET_RECHARGED = "Wallet recharged for user {} via payment {}";
    private static final String LOG_PAYMENT_FAILED = "Payment failed for user {}: order {}";
    private static final String LOG_PAYMENT_INITIATION_FAILED = "Payment initiation failed for user {}: {}";
    private static final String LOG_CALLBACK_ERROR = "Error handling payment callback: {}";

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
            validateRazorpayCredentials();

            JSONObject orderRequest = createOrderRequest(userId, amount);
            RazorpayClient razorpay = new RazorpayClient(razorpayKey, razorpaySecret);
            Order order = razorpay.orders.create(orderRequest);
            String orderId = order.get(ORDER_ID_KEY);

            logger.info(LOG_ORDER_CREATED, userId, orderId);
            return orderId;
        } catch (Exception e) {
            logger.error(LOG_PAYMENT_INITIATION_FAILED, userId, e.getMessage());
            throw new RuntimeException(ERROR_PAYMENT_INITIATION_FAILED);
        }
    }

    // Handle payment callback/webhook
    public boolean handlePaymentCallback(String orderId, String paymentId, boolean success, String userId, double amount) {
        try {
            if (success) {
                return processSuccessfulPayment(orderId, paymentId, userId, amount);
            } else {
                recordFailedTransaction(orderId, paymentId, userId, amount);
                logger.warn(LOG_PAYMENT_FAILED, userId, orderId);
            }
        } catch (Exception e) {
            logger.error(LOG_CALLBACK_ERROR, e.getMessage());
        }
        return false;
    }

    // ===== Private Helper Methods =====

    private void validateRazorpayCredentials() {
        if (razorpayKey == null || razorpayKey.isEmpty() || razorpaySecret == null || razorpaySecret.isEmpty()) {
            logger.error(ERROR_MISSING_CREDENTIALS);
            throw new RuntimeException(ERROR_MISSING_CREDENTIALS);
        }
    }

    private JSONObject createOrderRequest(String userId, double amount) {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put(JSON_AMOUNT_KEY, (int)(amount * PAISE_MULTIPLIER));
        orderRequest.put(JSON_CURRENCY_KEY, CURRENCY_INR);
        orderRequest.put(JSON_RECEIPT_KEY, RECEIPT_PREFIX + userId);
        return orderRequest;
    }

    private boolean processSuccessfulPayment(String orderId, String paymentId, String userId, double amount) {
        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setBalance(wallet.getBalance() + amount);
            wallet.setLastUpdated(new java.util.Date());
            walletRepository.save(wallet);

            logger.info(LOG_WALLET_RECHARGED, userId, paymentId);
            recordSuccessfulTransaction(orderId, paymentId, userId, amount);
            return true;
        }
        return false;
    }

    private void recordSuccessfulTransaction(String orderId, String paymentId, String userId, double amount) {
        Transactions transaction = createTransactionBase(orderId, paymentId, userId, amount);
        transaction.setStatus(STATUS_SUCCESS);
        transactionService.recordTransaction(transaction);
    }

    private void recordFailedTransaction(String orderId, String paymentId, String userId, double amount) {
        Transactions transaction = createTransactionBase(orderId, paymentId, userId, amount);
        transaction.setStatus(STATUS_FAILED);
        transactionService.recordTransaction(transaction);
    }

    private Transactions createTransactionBase(String orderId, String paymentId, String userId, double amount) {
        Transactions transaction = new Transactions();
        transaction.setUserId(userId);
        transaction.setGateway(GATEWAY_NAME);
        transaction.setGatewayOrderId(orderId);
        transaction.setGatewayPaymentId(paymentId);
        transaction.setAmount(amount);
        transaction.setCurrency(CURRENCY_INR);
        transaction.setMethod(PAYMENT_METHOD);
        transaction.setTimestamp(new java.util.Date());
        return transaction;
    }
}
