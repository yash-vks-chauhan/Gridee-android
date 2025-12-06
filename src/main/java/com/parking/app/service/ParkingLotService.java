package com.parking.app.service;

import com.parking.app.model.ParkingLot;
import com.parking.app.model.ParkingSpot;
import com.parking.app.repository.ParkingLotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ParkingLotService {

    private static final Logger logger = LoggerFactory.getLogger(ParkingLotService.class);

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private ParkingSpotService parkingSpotService;

    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        return parkingLotRepository.save(parkingLot);
    }

    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    public ParkingLot getParkingLotById(String id) {
        return parkingLotRepository.findById(id).orElse(null);
    }

    public Optional<ParkingLot> getParkingLotByName(String name) {
        return parkingLotRepository.findByName(name);
    }

    public ParkingLot updateParkingLot(String id, ParkingLot lotDetails) {
        ParkingLot existing = parkingLotRepository.findById(id).orElse(null);
        if (existing != null) {
            if (lotDetails.getName() != null) existing.setName(lotDetails.getName());
            if (lotDetails.getLocation() != null) existing.setLocation(lotDetails.getLocation());
            if (lotDetails.getTotalSpots() > 0) existing.setTotalSpots(lotDetails.getTotalSpots());
            return parkingLotRepository.save(existing);
        }
        return null;
    }

    public void deleteParkingLot(String id) {
        parkingLotRepository.deleteById(id);
    }

    // ==================== ASYNC SPOT AVAILABILITY UPDATES ====================

    /**
     * Asynchronously decrease available spots count when a booking is created
     * Runs outside transaction boundary - eventual consistency is acceptable
     */
    @Async
    public void decreaseAvailableSpots(String lotId, String spotId) {
        try {
            logger.debug("Async: Decreasing available spots for lotId={}, spotId={}", lotId, spotId);

            Query query = new Query(Criteria.where(ParkingLot.FIELD_NAME).is(lotId)
                    .and(ParkingLot.FIELD_AVAILABLE_SPOTS).gt(0));
            Update update = new Update().inc(ParkingLot.FIELD_AVAILABLE_SPOTS, -1);

            ParkingLot result = mongoOperations.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    ParkingLot.class
            );

            if (result != null) {
                logger.debug("Successfully decreased available spots for lotId={}, new count={}",
                        lotId, result.getAvailableSpots());
            } else {
                logger.warn("Could not decrease available spots for lotId={} - may be at zero", lotId);
            }
        } catch (Exception e) {
            logger.error("Error decreasing available spots for lotId={}: {}", lotId, e.getMessage(), e);
        }
    }

    /**
     * Asynchronously increase available spots count when a booking is cancelled/completed
     * Runs outside transaction boundary - eventual consistency is acceptable
     */
    @Async
    public void increaseAvailableSpots(String lotId, String spotId) {
        try {
            logger.debug("Async: Increasing available spots for lotId={}, spotId={}", lotId, spotId);

            Query query = new Query(Criteria.where(ParkingLot.FIELD_NAME).is(lotId));
            Update update = new Update().inc(ParkingLot.FIELD_AVAILABLE_SPOTS, 1);

            ParkingLot result = mongoOperations.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    ParkingLot.class
            );

            if (result != null) {
                // Ensure we don't exceed total capacity
                if (result.getAvailableSpots() > result.getTotalSpots()) {
                    logger.warn("Available spots ({}) exceeded total spots ({}) for lotId={}, correcting...",
                            result.getAvailableSpots(), result.getTotalSpots(), lotId);
                    correctAvailableSpots(lotId);
                } else {
                    logger.debug("Successfully increased available spots for lotId={}, new count={}",
                            lotId, result.getAvailableSpots());
                }
            } else {
                logger.warn("Could not increase available spots for lotId={} - lot not found", lotId);
            }
        } catch (Exception e) {
            logger.error("Error increasing available spots for lotId={}: {}", lotId, e.getMessage(), e);
        }
    }

    /**
     * Recalculate and sync available spots from actual ParkingSpot data
     * Called periodically or when inconsistencies are detected
     */
    @Async
    public void recalculateAvailableSpots(String lotId) {
        try {
            logger.info("Recalculating available spots for lotId={}", lotId);

            // Get all spots for this lot
            List<ParkingSpot> spots = parkingSpotService.getSpotsByLotId(lotId);

            // Sum up available and total capacity
            int totalAvailable = spots.stream()
                    .mapToInt(ParkingSpot::getAvailable)
                    .sum();

            int totalCapacity = spots.stream()
                    .mapToInt(ParkingSpot::getCapacity)
                    .sum();

            // Update lot with calculated values
            Query query = new Query(Criteria.where(ParkingLot.FIELD_NAME).is(lotId));
            Update update = new Update()
                    .set(ParkingLot.FIELD_AVAILABLE_SPOTS, totalAvailable)
                    .set(ParkingLot.FIELD_TOTAL_SPOTS, totalCapacity);

            mongoOperations.updateFirst(query, update, ParkingLot.class);

            logger.info("Recalculated lotId={}: totalSpots={}, availableSpots={}",
                    lotId, totalCapacity, totalAvailable);
        } catch (Exception e) {
            logger.error("Error recalculating available spots for lotId={}: {}", lotId, e.getMessage(), e);
        }
    }

    /**
     * Correct available spots to not exceed total capacity
     */
    private void correctAvailableSpots(String lotId) {
        try {
            ParkingLot lot = getParkingLotById(lotId);
            if (lot != null && lot.getAvailableSpots() > lot.getTotalSpots()) {
                Query query = new Query(Criteria.where(ParkingLot.FIELD_NAME).is(lotId));
                Update update = new Update().set(ParkingLot.FIELD_AVAILABLE_SPOTS, lot.getTotalSpots());
                mongoOperations.updateFirst(query, update, ParkingLot.class);
                logger.info("Corrected available spots for lotId={} to {}", lotId, lot.getTotalSpots());
            }
        } catch (Exception e) {
            logger.error("Error correcting available spots for lotId={}: {}", lotId, e.getMessage(), e);
        }
    }
}
