package com.parking.app.service;

import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionService transactionService;

    // Get or create a user's wallet
    public Wallet getOrCreateWallet(String userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(0);
            wallet.setLastUpdated(new Date());
            wallet.setTransactions(new ArrayList<>());
            return walletRepository.save(wallet);
        });
    }

    // Get wallet by userId
    public Optional<Wallet> getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId);
    }

    // List all wallet transactions by wallet/user
    public List<Transactions> getUserTransactions(String userId) {
        return transactionService.getTransactionsByUserId(userId);
    }

    // Top up wallet and create a transaction (let listener update wallet)
    public Wallet topUpWallet(String userId, double amount) {
        getOrCreateWallet(userId);
        // Create a finalized transaction; TransactionChangeListener will update balance + refs exactly once
        Transactions tx = new Transactions();
        tx.setReferenceId(UUID.randomUUID().toString());
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType("wallet_topup");
        tx.setStatus("completed");
        tx.setTimestamp(new Date());
        transactionService.recordTransaction(tx);
        // Return latest wallet snapshot
        return walletRepository.findByUserId(userId).orElseGet(() -> getOrCreateWallet(userId));
    }

    // Save wallet (encapsulate repository access)
    public Wallet saveWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    public Wallet deductPenalty(String userId, double penaltyAmount) {
        Optional<Wallet> walletOpt = getWalletByUserId(userId);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            if (wallet.getBalance() >= penaltyAmount) {
                // Create penalty transaction
                Transactions tx = new Transactions();
                tx.setReferenceId(UUID.randomUUID().toString());
                tx.setUserId(userId);
                tx.setAmount(-penaltyAmount);
                tx.setType("penalty_deduction");
                tx.setStatus("completed");
                tx.setTimestamp(new Date());
                transactionService.recordTransaction(tx);

                // Update wallet
                wallet.setBalance(wallet.getBalance() - penaltyAmount);
                wallet.setLastUpdated(new Date());
                Wallet.TransactionRef ref = new Wallet.TransactionRef();
                ref.setReferenceId(tx.getReferenceId());
                ref.setType(tx.getType());
                ref.setAmount(-penaltyAmount);
                ref.setStatus("completed");

                List<Wallet.TransactionRef> txnList = wallet.getTransactions() == null ? new ArrayList<>() : wallet.getTransactions();
                txnList.add(ref);
                wallet.setTransactions(txnList);

                return walletRepository.save(wallet);
            }
        }
        return null;
    }

    public java.util.Optional<Wallet> findByUserId(String userId) {
        return walletRepository.findByUserId(userId);
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    public void refundWalletAndRecordTransaction(Bookings booking) {
        Wallet wallet = walletRepository.findByUserId(booking.getUserId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        wallet.setBalance(wallet.getBalance() + booking.getAmount());
        wallet.setLastUpdated(new Date());
        walletRepository.save(wallet);
    }

    // Recompute wallet balance from transactions ledger (idempotent repair)
    public Wallet reconcileBalance(String userId) {
        Wallet wallet = getOrCreateWallet(userId);
        List<Transactions> txs = transactionService.getTransactionsByUserId(userId);

        double newBalance = 0.0;
        if (txs != null) {
            for (Transactions tx : txs) {
                if (tx == null) continue;
                String status = tx.getStatus() != null ? tx.getStatus().toLowerCase() : "";
                if (!("completed".equals(status) || "success".equals(status))) {
                    continue; // only finalized
                }
                String type = tx.getType() != null ? tx.getType().toLowerCase() : "";
                double amt = tx.getAmount();
                double delta;
                if ("payment".equals(type) || "penalty_deduction".equals(type)) {
                    // Payments reduce balance; handle sign defensively
                    delta = (amt >= 0) ? -Math.abs(amt) : amt; // negative amounts remain negative
                } else {
                    // Credits increase balance
                    delta = Math.abs(amt);
                }
                newBalance += delta;
            }
        }

        wallet.setBalance(newBalance);
        wallet.setLastUpdated(new Date());
        return walletRepository.save(wallet);
    }
}
