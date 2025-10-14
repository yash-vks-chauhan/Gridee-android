package com.parking.app.controller;

import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.service.TransactionService;
import com.parking.app.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private TransactionService transactionService;

    // Get wallet details for a user
    // In WalletController.java

    @GetMapping("")
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
            @RequestBody Map<String, Double> request) {
        Double amount = request.get("amount");
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body("Amount must be positive.");
        }
        Wallet wallet = walletService.topUpWallet(userId, amount);
        return ResponseEntity.ok(wallet);
    }

    // Deduct penalty from wallet
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

//package com.parking.app.controller;
//
//import com.parking.app.model.Wallet;
//import com.parking.app.model.Transactions;
//import com.parking.app.service.WalletService;
//import com.parking.app.service.TransactionService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/users/{userId}/wallet")
//public class WalletController {
//
//    @Autowired
//    private WalletService walletService;
//
//    @Autowired
//    private TransactionService transactionService;
//
//    // Get wallet details for a user (auto-create if doesn't exist)
//    @GetMapping("")
//    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
//        System.out.println("💰 Getting wallet for user: " + userId);
//
//        Optional<Wallet> walletOpt = walletService.getWalletByUserId(userId);
//
//        if (walletOpt.isPresent()) {
//            System.out.println("✅ Wallet found with balance: " + walletOpt.get().getBalance());
//            return ResponseEntity.ok(walletOpt.get());
//        } else {
//            // ✅ Auto-create wallet if it doesn't exist
//            System.out.println("📝 Wallet not found - creating new wallet for user: " + userId);
//            try {
//                Wallet newWallet = walletService.createWallet(userId);
//                System.out.println("✅ New wallet created with balance: " + newWallet.getBalance());
//                return ResponseEntity.ok(newWallet);
//            } catch (Exception e) {
//                System.out.println("❌ Error creating wallet: " + e.getMessage());
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//            }
//        }
//    }
//
//    // Get all transactions for a user
//    @GetMapping("/transactions")
//    public ResponseEntity<List<Transactions>> getUserTransactions(@PathVariable String userId) {
//        System.out.println("📜 Fetching transactions for user: " + userId);
//        List<Transactions> transactions = transactionService.getTransactionsByUserId(userId);
//        System.out.println("✅ Found " + transactions.size() + " transactions");
//        return ResponseEntity.ok(transactions);
//    }
//
//    // Top up wallet
//    @PostMapping("/topup")
//    public ResponseEntity<?> topUpWallet(
//            @PathVariable String userId,
//            @RequestBody Map<String, Double> request) {
//        Double amount = request.get("amount");
//        System.out.println("💵 Top-up request for user " + userId + ": ₹" + amount);
//
//        if (amount == null || amount <= 0) {
//            System.out.println("❌ Invalid amount: " + amount);
//            return ResponseEntity.badRequest().body("Amount must be positive.");
//        }
//
//        try {
//            Wallet wallet = walletService.topUpWallet(userId, amount);
//            System.out.println("✅ Top-up successful. New balance: ₹" + wallet.getBalance());
//            return ResponseEntity.ok(wallet);
//        } catch (Exception e) {
//            System.out.println("❌ Top-up failed: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Failed to top up wallet: " + e.getMessage());
//        }
//    }
//
//    // Deduct penalty from wallet
//    @PostMapping("/deduct-penalty")
//    public ResponseEntity<?> deductPenalty(
//            @PathVariable String userId,
//            @RequestBody Map<String, Double> request) {
//        Double penalty = request.get("penalty");
//        System.out.println("⚠️ Penalty deduction request for user " + userId + ": ₹" + penalty);
//
//        if (penalty == null || penalty <= 0) {
//            System.out.println("❌ Invalid penalty amount: " + penalty);
//            return ResponseEntity.badRequest().body("Penalty must be positive.");
//        }
//
//        try {
//            Wallet wallet = walletService.deductPenalty(userId, penalty);
//            if (wallet == null) {
//                System.out.println("❌ Insufficient balance for penalty deduction");
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body("Wallet not found or insufficient balance.");
//            }
//            System.out.println("✅ Penalty deducted. New balance: ₹" + wallet.getBalance());
//            return ResponseEntity.ok(wallet);
//        } catch (Exception e) {
//            System.out.println("❌ Penalty deduction failed: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Failed to deduct penalty: " + e.getMessage());
//        }
//    }
//}