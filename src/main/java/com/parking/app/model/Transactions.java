package com.parking.app.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "transactions")
public class Transactions {
    @Id
    private String id;
    private String userId;
    private String type;              // e.g., "payment", "refund"
    private double amount;
    private String currency;          // e.g., "INR", "USD"
    private String method;            // e.g., "credit_card", "paypal"
    private String referenceId;       // transaction ref from payment gateway
    private String gateway;           // e.g., "razorpay", "paytm"
    private String gatewayOrderId;    // order id from gateway
    private String gatewayPaymentId;  // payment id from gateway
    private Date timestamp = new Date();
    private String status = "pending";
    private String failureReason;     // error message if failed
    private Map<String, Object> metadata; // additional info

    public Transactions(String userId, double amount, String type, Date timestamp) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.status = "pending";
    }
}
