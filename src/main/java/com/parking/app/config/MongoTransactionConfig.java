package com.parking.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * MongoDB Transaction Configuration
 *
 * IMPORTANT: MongoDB transactions require:
 * 1. MongoDB 4.0+ for replica set transactions
 * 2. MongoDB 4.2+ for sharded cluster transactions
 * 3. MongoDB must be running as a replica set (even for single-node deployments)
 *
 * To enable replica set on local MongoDB:
 * 1. Start MongoDB with: mongod --replSet rs0
 * 2. Initialize replica set: mongo --eval "rs.initiate()"
 */
@Configuration
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}

