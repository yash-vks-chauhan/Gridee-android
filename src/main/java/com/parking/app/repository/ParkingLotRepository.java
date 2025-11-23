package com.parking.app.repository;

import com.parking.app.model.ParkingLot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingLotRepository extends MongoRepository<ParkingLot, String> {
    Optional<ParkingLot> findByName(String name);
}
