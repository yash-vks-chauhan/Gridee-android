package com.parking.app.service;

import com.parking.app.model.ParkingSpot;
import com.parking.app.repository.ParkingSpotRepository;
import com.parking.app.repository.ParkingLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ParkingSpotService {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private MongoOperations mongoOperations;

    public ParkingSpot createParkingSpot(ParkingSpot spot) {
        if (spot.getAvailable() == 0) {
            spot.setAvailable(spot.getCapacity());
        }
        return parkingSpotRepository.save(spot);
    }

    public List<ParkingSpot> getAllParkingSpots() {
        return parkingSpotRepository.findAll();
    }

    public List<ParkingSpot> getParkingSpotsByLotId(String lotId) {
        return parkingSpotRepository.findByLotId(lotId);
    }

    public ParkingSpot getParkingSpotById(String spotId) {
        return parkingSpotRepository.findById(spotId).orElse(null);
    }

    public ParkingSpot updateParkingSpot(String spotId, ParkingSpot spotDetails) {
        ParkingSpot existingSpot = parkingSpotRepository.findById(spotId).orElse(null);
        if (existingSpot != null) {
            if (spotDetails.getLotId() != null) existingSpot.setLotId(spotDetails.getLotId());
            if (spotDetails.getZoneName() != null) existingSpot.setZoneName(spotDetails.getZoneName());
            if (spotDetails.getCapacity() > 0) existingSpot.setCapacity(spotDetails.getCapacity());
            if (spotDetails.getAvailable() >= 0) existingSpot.setAvailable(spotDetails.getAvailable());
            return parkingSpotRepository.save(existingSpot);
        }
        return null;
    }

    public void deleteParkingSpot(String spotId) {
        parkingSpotRepository.deleteById(spotId);
    }

    /**
     * Atomically hold spot: decrement available by 1 if available > 0, set heldBy and heldAt.
     * Return updated spot or null if none available.
     */
    public ParkingSpot holdSpot(String spotId, String userId) {
        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update holdUpdate = new Update()
                .inc("available", -1)
                .set("heldBy", userId)
                .set("heldAt", new Date());

        return mongoOperations.findAndModify(spotQuery, holdUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    /**
     * Release spot hold: increment available by 1 and clear heldBy and heldAt.
     */
    public ParkingSpot releaseSpot(String spotId) {
        Query spotQuery = new Query(Criteria.where("_id").is(spotId));
        Update releaseUpdate = new Update()
                .inc("available", 1)
                .unset("heldBy")
                .unset("heldAt");

        return mongoOperations.findAndModify(spotQuery, releaseUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    /**
     * Release all expired holds older than expiryCutoff.
     */
    }

