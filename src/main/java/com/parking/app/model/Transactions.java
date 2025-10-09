package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "transactions")
public class Transactions {
    @Id
    private String id;
    private String userId;
    private String type;        // e.g., "payment", "refund"
    private double amount;
    private String method;      // e.g., "credit_card", "paypal"
    private String referenceId; // transaction ref from payment gateway
    private Date timestamp = new Date();
    private String status = "pending";

    public Transactions(String userId, double amount, String type, Date timestamp) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.status = "pending";
    }
}
