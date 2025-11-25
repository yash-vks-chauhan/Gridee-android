package com.parking.app.controller;

import com.parking.app.dto.WalletRewardRequest;
import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.service.TransactionService;
import com.parking.app.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
//TODO : try extracing userId from JWT token instead of path variable for better security
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    // Get wallet details for a user
    // In WalletController.java

    @GetMapping
    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
        Wallet wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(wallet);
    }


    // Get all transactions for a user
    @GetMapping("/transactions")
    public ResponseEntity<List<Transactions>> getUserTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId));
    }

    // Top up wallet
    @PostMapping("/topup")
    public ResponseEntity<?> topUpWallet(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        Object amountObj = request.get("amount");
        Double amount = null;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else if (amountObj instanceof String) {
            amount = Double.valueOf((String) amountObj);
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        Object typeObj = request.get("type");
        String txType = typeObj != null ? typeObj.toString() : null;
        Object sourceObj = request.get("source");
        String source = sourceObj != null ? sourceObj.toString() : null;
        Wallet wallet = walletService.topUpWallet(userId, amount, txType, source);
        return ResponseEntity.ok(wallet);
    }

    // Deduct penalty from wallet
    @PostMapping("/deduct-penalty")
    public ResponseEntity<?> deductPenalty(
            @PathVariable String userId,
            @RequestBody Map<String, Double> request) {
        Double penalty = request.get("penalty");
        if (penalty == null || penalty <= 0) {
            throw new IllegalArgumentException("Penalty must be positive.");
        }
        Wallet wallet = walletService.deductPenalty(userId, penalty);
        if (wallet == null) {
            throw new com.parking.app.exception.NotFoundException("Wallet not found or insufficient balance.");
        }
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/reward")
    public ResponseEntity<Wallet> grantReward(
            @PathVariable String userId,
            @RequestBody WalletRewardRequest request) {
        double amount = (request.getAmount() != null) ? request.getAmount() : 10.0;
        if (amount <= 0) {
            throw new IllegalArgumentException("Reward amount must be positive.");
        }
        Wallet wallet = walletService.applyReward(
                userId,
                amount,
                request.getSource(),
                request.getRewardId()
        );
        return ResponseEntity.ok(wallet);
    }
}
