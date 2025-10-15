package com.parking.app.config.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Business Events Logger for analytics and KPI tracking
 * Logs business-critical events in a standardized format for cloud analytics tools
 * Compatible with: Datadog, New Relic, Splunk, ELK Stack, AWS CloudWatch
 */
@Component
public class BusinessEventsLogger {

    private static final Logger businessLogger = LoggerFactory.getLogger("BUSINESS_EVENTS");

    /**
     * Log booking creation event
     */
    public void logBookingCreated(String bookingId, String userId, String spotId, double amount) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "booking_created");
        eventData.put("eventType", "transaction");
        eventData.put("bookingId", bookingId);
        eventData.put("userId", userId);
        eventData.put("spotId", spotId);
        eventData.put("amount", String.valueOf(amount));
        eventData.put("currency", "INR");

        logBusinessEvent(eventData);
    }

    /**
     * Log booking cancellation event
     */
    public void logBookingCancelled(String bookingId, String userId, String reason) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "booking_cancelled");
        eventData.put("eventType", "transaction");
        eventData.put("bookingId", bookingId);
        eventData.put("userId", userId);
        eventData.put("reason", reason);

        logBusinessEvent(eventData);
    }

    /**
     * Log payment event
     */
    public void logPaymentProcessed(String transactionId, String userId, double amount, String status, String gateway) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "payment_processed");
        eventData.put("eventType", "payment");
        eventData.put("transactionId", transactionId);
        eventData.put("userId", userId);
        eventData.put("amount", String.valueOf(amount));
        eventData.put("paymentStatus", status);
        eventData.put("paymentGateway", gateway);
        eventData.put("currency", "INR");

        logBusinessEvent(eventData);
    }

    /**
     * Log wallet operation
     */
    public void logWalletOperation(String userId, String operation, double amount, double balanceBefore, double balanceAfter) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "wallet_operation");
        eventData.put("eventType", "financial");
        eventData.put("userId", userId);
        eventData.put("operation", operation);
        eventData.put("amount", String.valueOf(amount));
        eventData.put("balanceBefore", String.valueOf(balanceBefore));
        eventData.put("balanceAfter", String.valueOf(balanceAfter));
        eventData.put("currency", "INR");

        logBusinessEvent(eventData);
    }

    /**
     * Log user registration
     */
    public void logUserRegistered(String userId, String email, String role) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "user_registered");
        eventData.put("eventType", "user_lifecycle");
        eventData.put("userId", userId);
        eventData.put("email", email);
        eventData.put("role", role);

        logBusinessEvent(eventData);
    }

    /**
     * Log user login
     */
    public void logUserLogin(String userId, String loginMethod, boolean success) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put("eventName", "user_login");
        eventData.put("eventType", "authentication");
        eventData.put("userId", userId);
        eventData.put("loginMethod", loginMethod);
        eventData.put("success", String.valueOf(success));

        logBusinessEvent(eventData);
    }

    /**
     * Generic business event logger with standardized MDC fields
     */
    private void logBusinessEvent(Map<String, String> eventData) {
        try {
            // Add all event data to MDC for structured logging
            eventData.forEach(MDC::put);

            // Add standard fields
            MDC.put("timestamp", String.valueOf(System.currentTimeMillis()));
            MDC.put("service.name", "gridee-parking");

            // Log the event
            businessLogger.info("Business Event: {}", eventData.get("eventName"));

        } finally {
            // Clean up MDC
            eventData.keySet().forEach(MDC::remove);
            MDC.remove("timestamp");
            MDC.remove("service.name");
        }
    }
}

