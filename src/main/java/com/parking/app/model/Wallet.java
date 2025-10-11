package com.parking.app.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "wallets")
@Getter
@Setter
public class Wallet {

    @Id
    private String id;
    private String userId;
    private double balance;
    private Date lastUpdated;
    private Date createdAt;
    private List<TransactionRef> transactions;

    public Wallet() {
        this.balance = 0;
        this.lastUpdated = new Date();
        this.createdAt = new Date();
    }

    // --- Nested static class for transaction references ---
    @Getter
    @Setter
    public static class TransactionRef {
        private String referenceId;
        private String type;
        private double amount;
        private String status;

        public TransactionRef() {}
    }
}
