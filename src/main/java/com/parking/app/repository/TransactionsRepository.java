package com.parking.app.repository;

import com.parking.app.model.Transactions;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionsRepository extends MongoRepository<Transactions, String> {
    List<Transactions> findByUserId(String userId);
    List<Transactions> findByType(String type);
    List<Transactions> findByStatus(String status);

    // Additional query methods for new fields
    List<Transactions> findByGateway(String gateway);
    List<Transactions> findByGatewayOrderId(String gatewayOrderId);
    List<Transactions> findByGatewayPaymentId(String gatewayPaymentId);
    List<Transactions> findByCurrency(String currency);
    List<Transactions> findByMethod(String method);
    List<Transactions> findByReferenceId(String referenceId);
}
