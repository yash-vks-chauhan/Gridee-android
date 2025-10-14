package com.parking.app.service.booking;

import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.service.TransactionService;
import com.parking.app.service.WalletService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service responsible for wallet operations related to bookings
 */
@Service
public class BookingWalletService {

    private final WalletService walletService;
    private final TransactionService transactionService;

    public BookingWalletService(WalletService walletService, TransactionService transactionService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
    }

    public void deductWalletBalance(Wallet wallet, double amount) {
        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setLastUpdated(new Date());
        walletService.save(wallet);
    }

    public void recordWalletTransaction(String userId, double amount, String description) {
        transactionService.save(new Transactions(userId, amount, description, new Date()));
    }

    public void deductAndRecord(String userId, double amount, String description) {
        walletService.deductFromWallet(userId, amount);
        walletService.recordWalletTransaction(userId, -amount, description);
    }

    public void applyPenaltyToWallet(String userId, double totalPenalty,
                                     double lateCheckInPenalty, double lateCheckOutPenalty) {
        if (totalPenalty > 0) {
            walletService.deductFromWallet(userId, totalPenalty);
            if (lateCheckInPenalty > 0) {
                transactionService.save(new Transactions(userId, -lateCheckInPenalty,
                    "Late check-in penalty", new Date()));
            }
            if (lateCheckOutPenalty > 0) {
                transactionService.save(new Transactions(userId, -lateCheckOutPenalty,
                    "Late check-out penalty", new Date()));
            }
        }
    }

    public void refundToWallet(String userId, double amount, String description) {
        walletService.refundWalletAndRecordTransaction(
            new com.parking.app.model.Bookings() {{
                setUserId(userId);
                setAmount(amount);
            }}
        );
        transactionService.save(new Transactions(userId, amount, description, new Date()));
    }
}

