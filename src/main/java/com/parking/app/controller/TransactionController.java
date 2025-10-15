// src/main/java/com/parking/app/controller/TransactionController.java
package com.parking.app.controller;

import com.parking.app.model.Transactions;
import com.parking.app.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transactions> createTransaction(@RequestBody Transactions transaction) {
        Transactions created = transactionService.recordTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Transactions>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transactions> getTransactionById(@PathVariable String id) {
        Transactions transaction = transactionService.getTransactionById(id);
        if (transaction == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Transactions>> getTransactionsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Transactions>> getTransactionsByType(@PathVariable String type) {
        return ResponseEntity.ok(transactionService.getTransactionsByType(type));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Transactions>> getTransactionsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(transactionService.getTransactionsByStatus(status));
    }

    @GetMapping("/gateway/{gateway}")
    public ResponseEntity<List<Transactions>> getTransactionsByGateway(@PathVariable String gateway) {
        return ResponseEntity.ok(transactionService.getTransactionsByGateway(gateway));
    }

    @GetMapping("/gatewayOrderId/{gatewayOrderId}")
    public ResponseEntity<List<Transactions>> getTransactionsByGatewayOrderId(@PathVariable String gatewayOrderId) {
        return ResponseEntity.ok(transactionService.getTransactionsByGatewayOrderId(gatewayOrderId));
    }

    @GetMapping("/gatewayPaymentId/{gatewayPaymentId}")
    public ResponseEntity<List<Transactions>> getTransactionsByGatewayPaymentId(@PathVariable String gatewayPaymentId) {
        return ResponseEntity.ok(transactionService.getTransactionsByGatewayPaymentId(gatewayPaymentId));
    }

    @GetMapping("/currency/{currency}")
    public ResponseEntity<List<Transactions>> getTransactionsByCurrency(@PathVariable String currency) {
        return ResponseEntity.ok(transactionService.getTransactionsByCurrency(currency));
    }

    @GetMapping("/method/{method}")
    public ResponseEntity<List<Transactions>> getTransactionsByMethod(@PathVariable String method) {
        return ResponseEntity.ok(transactionService.getTransactionsByMethod(method));
    }

    @GetMapping("/referenceId/{referenceId}")
    public ResponseEntity<List<Transactions>> getTransactionsByReferenceId(@PathVariable String referenceId) {
        return ResponseEntity.ok(transactionService.getTransactionsByReferenceId(referenceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transactions> updateTransaction(@PathVariable String id, @RequestBody Transactions transactionDetails) {
        Transactions updated = transactionService.updateTransaction(id, transactionDetails);
        if (updated == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
