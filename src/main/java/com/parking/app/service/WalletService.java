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

    // Deduct amount from wallet
    public Wallet deductFromWallet(String userId, double amount) {
        Optional<Wallet> walletOpt = getWalletByUserId(userId);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            if (wallet.getBalance() >= amount) {
                wallet.setBalance(wallet.getBalance() - amount);
                wallet.setLastUpdated(new Date());
                return walletRepository.save(wallet);
            }
        }
        return null;
    }

    // Record a wallet transaction
    public Transactions recordWalletTransaction(String userId, double amount, String type) {
        Transactions tx = new Transactions();
        tx.setReferenceId(UUID.randomUUID().toString());
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setStatus("completed");
        tx.setTimestamp(new Date());
        return transactionService.recordTransaction(tx);
    }

    // Grouped: Wallet retrieval methods
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

    public Optional<Wallet> getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId);
    }

    public java.util.Optional<Wallet> findByUserId(String userId) {
        return walletRepository.findByUserId(userId);
    }

    // Grouped: Wallet transaction methods
    public List<Transactions> getUserTransactions(String userId) {
        return transactionService.getTransactionsByUserId(userId);
    }

    public Wallet topUpWallet(String userId, double amount) {
        Wallet wallet = getOrCreateWallet(userId);
        Transactions tx = recordWalletTransaction(userId, amount, "wallet_topup");
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setLastUpdated(new Date());
        Wallet.TransactionRef ref = new Wallet.TransactionRef();
        ref.setReferenceId(tx.getReferenceId());
        ref.setType(tx.getType());
        ref.setAmount(amount);
        ref.setStatus("completed");
        List<Wallet.TransactionRef> txnList = wallet.getTransactions() == null ? new ArrayList<>() : wallet.getTransactions();
        txnList.add(ref);
        wallet.setTransactions(txnList);
        return walletRepository.save(wallet);
    }

    public Wallet deductPenalty(String userId, double penaltyAmount) {
        Wallet wallet = deductFromWallet(userId, penaltyAmount);
        if (wallet != null) {
            Transactions tx = recordWalletTransaction(userId, -penaltyAmount, "penalty_deduction");
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
        return null;
    }

    // Grouped: Wallet persistence methods
    public Wallet saveWallet(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    public Wallet save(Wallet wallet) {
        return walletRepository.save(wallet);
    }

    // Grouped: Refund method
    public void refundWalletAndRecordTransaction(Bookings booking) {
        Wallet wallet = walletRepository.findByUserId(booking.getUserId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        wallet.setBalance(wallet.getBalance() + booking.getAmount());
        wallet.setLastUpdated(new Date());
        walletRepository.save(wallet);
    }
}