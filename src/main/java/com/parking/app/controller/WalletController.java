package com.parking.app.controller;

import com.parking.app.model.Wallet;
import com.parking.app.model.Transactions;
import com.parking.app.service.WalletService;
import com.parking.app.service.TransactionService;
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

    @Autowired
    private TransactionService transactionService;

    // Get wallet details for a user (create if missing)
    @GetMapping("")
    public ResponseEntity<Wallet> getWallet(@PathVariable String userId) {
        // Ensure a wallet always exists for the user to avoid 404s in clients
        Wallet wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(wallet);
    }

    // Get all transactions for a user
    @GetMapping("/transactions")
    public ResponseEntity<List<Transactions>> getUserTransactions(@PathVariable String userId) {
        List<Transactions> txs = transactionService.getTransactionsByUserId(userId);
        if (txs == null || txs.isEmpty()) return ResponseEntity.ok(List.of());

        // Deduplicate by gatewayPaymentId -> gatewayOrderId -> referenceId -> id
        java.util.LinkedHashMap<String, Transactions> map = new java.util.LinkedHashMap<>();
        for (Transactions tx : txs) {
            if (tx == null) continue;
            String key = tx.getGatewayPaymentId();
            if (key == null || key.isBlank()) key = tx.getGatewayOrderId();
            if (key == null || key.isBlank()) key = tx.getReferenceId();
            if (key == null || key.isBlank()) key = tx.getId();
            if (key == null) continue;

            Transactions existing = map.get(key);
            if (existing == null) {
                map.put(key, tx);
                continue;
            }
            // Prefer a finalized record over a non-finalized one
            String s1 = existing.getStatus() != null ? existing.getStatus().toLowerCase() : "";
            String s2 = tx.getStatus() != null ? tx.getStatus().toLowerCase() : "";
            boolean finalized1 = s1.equals("completed") || s1.equals("success");
            boolean finalized2 = s2.equals("completed") || s2.equals("success");
            if (finalized2 && !finalized1) {
                map.put(key, tx);
            } else if (finalized2 == finalized1) {
                // If both same finality, keep the latest timestamp
                java.util.Date d1 = existing.getTimestamp();
                java.util.Date d2 = tx.getTimestamp();
                if (d2 != null && (d1 == null || d2.after(d1))) {
                    map.put(key, tx);
                }
            }
        }
        return ResponseEntity.ok(new java.util.ArrayList<>(map.values()));
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

    // Admin/repair: Recompute wallet balance from transaction history
    @PostMapping("/reconcile")
    public ResponseEntity<Wallet> reconcile(@PathVariable String userId) {
        Wallet wallet = walletService.reconcileBalance(userId);
        return ResponseEntity.ok(wallet);
    }
}
