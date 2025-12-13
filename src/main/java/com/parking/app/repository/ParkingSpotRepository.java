package com.parking.app.repository;

import com.parking.app.model.ParkingSpot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSpotRepository extends MongoRepository<ParkingSpot, String> {

    List<ParkingSpot> findByLotId(String lotId);
    List<ParkingSpot> findByLotName(String lotName);

}
