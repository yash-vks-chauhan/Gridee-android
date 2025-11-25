package com.parking.app.controller;

import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // Get wallet balance for user
    @GetMapping("")
    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
        return walletService.getWalletByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get transaction history for user
    @GetMapping("/transactions")
    public ResponseEntity<List<Transactions>> getTransactions(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getUserTransactions(userId));
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
            return ResponseEntity.badRequest().body("Amount must be positive.");
        }
        String type = request.get("type") != null ? request.get("type").toString() : null;
        String source = request.get("source") != null ? request.get("source").toString() : null;
        Wallet wallet = walletService.topUpWallet(userId, amount, type, source);
        return ResponseEntity.ok(wallet);
    }


}
