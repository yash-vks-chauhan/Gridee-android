package com.parking.app.service;

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

    // Top up wallet and create a transaction (atomic)
    public Wallet topUpWallet(String userId, double amount) {
        Wallet wallet = getOrCreateWallet(userId);

        // Create transaction
        Transactions tx = new Transactions();
        tx.setReferenceId(UUID.randomUUID().toString());
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType("wallet_topup");
        tx.setStatus("completed");
        tx.setTimestamp(new Date());
        transactionService.recordTransaction(tx);

        // Update wallet
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
}
