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
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive.");
        }
        Wallet wallet = walletService.topUpWallet(userId, amount);
        return ResponseEntity.ok(wallet);
    }
// Add this to WalletController

    @PostMapping("/deduct-penalty")
    public ResponseEntity<?> deductPenalty(
            @PathVariable String userId,
            @RequestBody Map<String, Double> request) {
        Double penalty = request.get("penalty");
        if (penalty == null || penalty <= 0) {
            return ResponseEntity.badRequest().body("Penalty must be positive.");
        } 
        Wallet wallet = walletService.deductPenalty(userId, penalty);
        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Wallet not found or insufficient balance.");
        }
        return ResponseEntity.ok(wallet);
    }



}
