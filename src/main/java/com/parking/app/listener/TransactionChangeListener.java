package com.parking.app.listener;

import com.parking.app.model.Transactions;
import com.parking.app.model.Wallet;
import com.parking.app.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

@Component
public class TransactionChangeListener {

    @Autowired
    private WalletService walletService;

    // This method listens to MongoDB after-save events for Transaction entities
    @EventListener
    public void handleTransactionChange(AfterSaveEvent<?> event) {
        Object source = event.getSource();

        if (!(source instanceof Transactions)) {
            // Ignore events not related to Transactions
            return;
        }

        Transactions tx = (Transactions) source;

        // Only act on completed transactions
        if (!"completed".equalsIgnoreCase(tx.getStatus()) || tx.getUserId() == null) return;

        Optional<Wallet> walletOpt = walletService.getWalletByUserId(tx.getUserId());
        Wallet wallet = walletOpt.orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(tx.getUserId());
            w.setBalance(0);
            w.setLastUpdated(new Date());
            w.setTransactions(new ArrayList<>());
            return w;
        });

        // Build a stable unique key for this transaction to avoid double-application
        String key = tx.getGatewayPaymentId();
        if (key == null || key.isBlank()) key = tx.getGatewayOrderId();
        if (key == null || key.isBlank()) key = tx.getReferenceId();
        if (key == null || key.isBlank()) key = tx.getId();

        ArrayList<Wallet.TransactionRef> txnList = wallet.getTransactions() == null
                ? new ArrayList<>()
                : new ArrayList<>(wallet.getTransactions());

        boolean alreadyApplied = false;
        if (key != null) {
            for (Wallet.TransactionRef r : txnList) {
                if (r != null && key.equals(r.getReferenceId())) {
                    alreadyApplied = true;
                    break;
                }
            }
        }

        if (!alreadyApplied) {
            double updatedBalance = wallet.getBalance();
            // Add to balance for topup/refund, subtract for payment
            String type = tx.getType() != null ? tx.getType().toLowerCase() : "";
            double amount = tx.getAmount();
            double absoluteAmount = Math.abs(amount);
            if ("wallet_topup".equals(type) || "refund".equals(type) || "reward_bonus".equals(type) || "bonus".equals(type)) {
                updatedBalance += absoluteAmount;
            } else if ("payment".equals(type) || "penalty_deduction".equals(type)) {
                updatedBalance -= absoluteAmount;
            }

            wallet.setBalance(updatedBalance);
            wallet.setLastUpdated(new Date());

            // Add TransactionRef with the stable key
            Wallet.TransactionRef ref = new Wallet.TransactionRef();
            ref.setReferenceId(key);
            ref.setType(tx.getType());
            ref.setAmount(tx.getAmount());
            ref.setStatus(tx.getStatus());
            txnList.add(ref);
            wallet.setTransactions(txnList);

            walletService.saveWallet(wallet);
        }
    }
}
