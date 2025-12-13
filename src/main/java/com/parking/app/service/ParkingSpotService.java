package com.parking.app.service;

import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingLot;
import com.parking.app.model.ParkingSpot;
import com.parking.app.repository.ParkingLotRepository;
import com.parking.app.repository.ParkingSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParkingSpotService {

    // ===== Constants =====

    // MongoDB field names
    private static final String FIELD_ID = "_id";
    private static final String FIELD_AVAILABLE = "available";
    private static final String FIELD_HELD_BY = "heldBy";
    private static final String FIELD_HELD_AT = "heldAt";

    // Zone name constants
    private static final String ZONE_UNKNOWN = "Unknown Zone";
    private static final String ZONE_MAIN = "Main Parking Zone";
    private static final String ZONE_SECONDARY = "Secondary Parking Zone";
    private static final String ZONE_TERTIARY = "Tertiary Parking Zone";
    private static final String ZONE_PREFIX = "Parking Zone ";
    private static final String ZONE_SIMPLE_PREFIX = "Zone ";
    private static final String ZONE_AREA_SUFFIX = " Area";

    // Invalid zone name identifiers
    private static final String INVALID_ZONE_NIL = "nil";
    private static final String INVALID_ZONE_NULL = "null";

    // Spot ID prefixes and identifiers
    private static final String SPOT_ID_PS1 = "ps1";
    private static final String SPOT_ID_PS2 = "ps2";
    private static final String SPOT_ID_PS3 = "ps3";
    private static final String SPOT_ID_PREFIX_PS = "ps";
    private static final String SPOT_ID_DELIMITER = "-";
    private static final int SPOT_ID_PREFIX_LENGTH = 2;

    // Increment/Decrement values
    private static final int INCREMENT_VALUE = 1;
    private static final int DECREMENT_VALUE = -1;
    private static final int MIN_AVAILABLE_SPOTS = 0;

    // Error messages
    private static final String ERROR_NO_SPOTS_AVAILABLE = "No spots available";

    // Log messages
    private static final String LOG_UPDATING_ZONE = "🔧 Updating zone name for spot %s to: %s";
    private static final String LOG_ZONE_FIX_START = "🔧 Starting zone name fix for all parking spots...";
    private static final String LOG_ZONE_UPDATED = "✅ Updated spot %s with zone name: %s";
    private static final String LOG_ZONE_FIX_COMPLETE = "🎉 Zone name fix complete! Updated %d parking spots.";
    private static final String LOG_CAPACITY_RESET = "✅ All parking spots have been reset to full capacity.";

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    // ===== CRUD Operations =====

    public ParkingSpot createParkingSpot(ParkingSpot spot) {
        if (spot.getAvailable() == MIN_AVAILABLE_SPOTS) {
            spot.setAvailable(spot.getCapacity());
        }
        if (spot.getLotId() == null || spot.getLotId().isEmpty()) {
            spot.setLotId(spot.getLotName());
        }
        setProperZoneName(spot);
        return parkingSpotRepository.save(spot);
    }

    public ParkingSpot updateParkingSpot(String spotId, ParkingSpot spotDetails) {
        ParkingSpot existingSpot = parkingSpotRepository.findById(spotId).orElse(null);
        if (existingSpot != null) {
            if (spotDetails.getLotId() != null) existingSpot.setLotId(spotDetails.getLotId());
            if (spotDetails.getLotName() != null) existingSpot.setLotName(spotDetails.getLotName());
            if (spotDetails.getZoneName() != null) existingSpot.setZoneName(spotDetails.getZoneName());
            if (spotDetails.getCapacity() > MIN_AVAILABLE_SPOTS) existingSpot.setCapacity(spotDetails.getCapacity());
            if (spotDetails.getAvailable() >= MIN_AVAILABLE_SPOTS) existingSpot.setAvailable(spotDetails.getAvailable());
            if (existingSpot.getLotId() == null || existingSpot.getLotId().isEmpty()) {
                existingSpot.setLotId(existingSpot.getLotName());
            }
            return parkingSpotRepository.save(existingSpot);
        }
        return null;
    }

    public void deleteParkingSpot(String spotId) {
        parkingSpotRepository.deleteById(spotId);
    }

    // ===== Query Operations =====

    public ParkingSpot findById(String id) {
        return parkingSpotRepository.findById(id).orElse(null);
    }

    public ParkingSpot getParkingSpotById(String spotId) {
        ParkingSpot spot = parkingSpotRepository.findById(spotId).orElse(null);
        if (spot != null) {
            return ensureProperZoneName(spot);
        }
        return null;
    }

    public List<ParkingSpot> getAllParkingSpots() {
        List<ParkingSpot> spots = parkingSpotRepository.findAll();
        return spots.stream()
                .map(this::ensureProperZoneName)
                .collect(Collectors.toList());
    }

    public List<ParkingSpot> getSpotsByLotId(String lotId) {
        return findSpotsByLotReference(lotId);
    }

    public List<ParkingSpot> getAvailableSpots(String lotId, ZonedDateTime startTime, ZonedDateTime endTime, List<Bookings> overlappingBookings) {
        List<ParkingSpot> allSpots = findSpotsByLotReference(lotId);
        Set<String> bookedSpotIds = overlappingBookings.stream()
                .map(Bookings::getSpotId)
                .collect(Collectors.toSet());
        return allSpots.stream()
                .filter(spot -> !bookedSpotIds.contains(spot.getId()))
                .collect(Collectors.toList());
    }

    // ===== Availability Management =====

    /**
     * Atomically reserves a spot for booking - prevents race conditions
     * This method is thread-safe and ensures no overbooking even with concurrent requests
     *
     * @param spotId The spot to reserve
     * @return true if reservation successful, false if no spots available
     */
//    @Transactional
    public boolean atomicReserveSpotForBooking(String spotId) {
        Query spotQuery = new Query(
                Criteria.where(FIELD_ID).is(spotId)
                        .and(FIELD_AVAILABLE).gt(MIN_AVAILABLE_SPOTS)
        );
        Update decUpdate = new Update().inc(FIELD_AVAILABLE, DECREMENT_VALUE);

        // findAndModify is atomic in MongoDB - either succeeds or fails, no race condition
        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery,
                decUpdate,
                FindAndModifyOptions.options().returnNew(false), // Return old document to verify
                ParkingSpot.class
        );

        return updatedSpot != null && updatedSpot.getAvailable() > MIN_AVAILABLE_SPOTS;
    }

    public void decrementSpotAvailability(String spotId) {
        Query spotQuery = new Query(Criteria.where(FIELD_ID).is(spotId).and(FIELD_AVAILABLE).gt(MIN_AVAILABLE_SPOTS));
        Update decUpdate = new Update().inc(FIELD_AVAILABLE, DECREMENT_VALUE);
        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery, decUpdate, ParkingSpot.class);
        if (updatedSpot == null) {
            throw new RuntimeException(ERROR_NO_SPOTS_AVAILABLE);
        }
    }

    public void incrementSpotAvailability(String spotId) {
        Update incUpdate = new Update().inc(FIELD_AVAILABLE, INCREMENT_VALUE);
        mongoOperations.updateFirst(new Query(Criteria.where(FIELD_ID).is(spotId)), incUpdate, ParkingSpot.class);
        ensureAvailableNotExceedCapacity(spotId);
    }

    public void ensureAvailableNotExceedCapacity(String spotId) {
        Optional<ParkingSpot> spot = parkingSpotRepository.findById(spotId);
        if (spot.isPresent() && spot.get().getAvailable() > spot.get().getCapacity()) {
            spot.get().setAvailable(spot.get().getCapacity());
            mongoOperations.save(spot);
        }
    }

    // ===== Spot Holding/Releasing Operations =====

    public ParkingSpot holdSpot(String spotId, String userId) {
        Query spotQuery = new Query(Criteria.where(FIELD_ID).is(spotId).and(FIELD_AVAILABLE).gt(MIN_AVAILABLE_SPOTS));
        Update holdUpdate = new Update()
                .inc(FIELD_AVAILABLE, DECREMENT_VALUE)
                .set(FIELD_HELD_BY, userId)
                .set(FIELD_HELD_AT, new Date());

        return mongoOperations.findAndModify(spotQuery, holdUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    public ParkingSpot releaseSpot(String spotId) {
        Query spotQuery = new Query(Criteria.where(FIELD_ID).is(spotId));
        Update releaseUpdate = new Update()
                .inc(FIELD_AVAILABLE, INCREMENT_VALUE)
                .unset(FIELD_HELD_BY)
                .unset(FIELD_HELD_AT);

        return mongoOperations.findAndModify(spotQuery, releaseUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    // ===== Zone Name Management =====

    private void setProperZoneName(ParkingSpot spot) {
        if (isInvalidZoneName(spot.getZoneName())) {
            String spotId = spot.getId();
            String zoneName = generateZoneNameFromId(spotId);
            spot.setZoneName(zoneName);
        }
    }

    private ParkingSpot ensureProperZoneName(ParkingSpot spot) {
        boolean needsUpdate = false;

        if (isInvalidZoneName(spot.getZoneName())) {
            String zoneName = generateZoneNameFromId(spot.getId());
            spot.setZoneName(zoneName);
            needsUpdate = true;
        }

        if (needsUpdate) {
            System.out.println(String.format(LOG_UPDATING_ZONE, spot.getId(), spot.getZoneName()));
            parkingSpotRepository.save(spot);
        }

        return spot;
    }

    private boolean isInvalidZoneName(String zoneName) {
        return zoneName == null ||
                zoneName.isEmpty() ||
                zoneName.equals(INVALID_ZONE_NIL) ||
                zoneName.equals(INVALID_ZONE_NULL);
    }

    private String generateZoneNameFromId(String spotId) {
        if (spotId == null || spotId.isEmpty()) {
            return ZONE_UNKNOWN;
        }

        String lowerSpotId = spotId.toLowerCase();
        switch (lowerSpotId) {
            case SPOT_ID_PS1:
                return ZONE_MAIN;
            case SPOT_ID_PS2:
                return ZONE_SECONDARY;
            case SPOT_ID_PS3:
                return ZONE_TERTIARY;
            default:
                return generateDynamicZoneName(spotId, lowerSpotId);
        }
    }

    private String generateDynamicZoneName(String spotId, String lowerSpotId) {
        if (lowerSpotId.startsWith(SPOT_ID_PREFIX_PS)) {
            String number = spotId.substring(SPOT_ID_PREFIX_LENGTH);
            return ZONE_PREFIX + number.toUpperCase();
        } else if (spotId.contains(SPOT_ID_DELIMITER)) {
            String[] parts = spotId.split(SPOT_ID_DELIMITER);
            return parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1) + ZONE_AREA_SUFFIX;
        } else {
            return ZONE_SIMPLE_PREFIX + spotId.toUpperCase();
        }
    }

    public void fixAllZoneNames() {
        System.out.println(LOG_ZONE_FIX_START);
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();

        int updatedCount = 0;
        for (ParkingSpot spot : allSpots) {
            if (isInvalidZoneName(spot.getZoneName())) {
                String newZoneName = generateZoneNameFromId(spot.getId());
                spot.setZoneName(newZoneName);
                parkingSpotRepository.save(spot);
                updatedCount++;

                System.out.println(String.format(LOG_ZONE_UPDATED, spot.getId(), newZoneName));
            }
        }

        System.out.println(String.format(LOG_ZONE_FIX_COMPLETE, updatedCount));
    }

    public void resetAllSpotsCapacity() {
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();
        for (ParkingSpot spot : allSpots) {
            spot.setAvailable(spot.getCapacity());
            parkingSpotRepository.save(spot);
        }
        System.out.println(LOG_CAPACITY_RESET);
    }

    // Fetch spots regardless of whether lotId or lotName was stored; dedupe by spot id.
    private List<ParkingSpot> findSpotsByLotReference(String lotIdOrName) {
        Map<String, ParkingSpot> uniqueSpots = new LinkedHashMap<>();

        // Resolve lot name from lotId if available (legacy data stored lotName only)
        String resolvedLotName = null;
        try {
            if (parkingLotRepository != null) {
                Optional<ParkingLot> lot = parkingLotRepository.findById(lotIdOrName);
                if (lot.isPresent()) {
                    resolvedLotName = lot.get().getName();
                }
            }
        } catch (Exception ignored) {}

        parkingSpotRepository.findByLotId(lotIdOrName)
                .forEach(spot -> uniqueSpots.put(spot.getId(), ensureProperZoneName(spot)));

        parkingSpotRepository.findByLotName(lotIdOrName)
                .forEach(spot -> uniqueSpots.put(spot.getId(), ensureProperZoneName(spot)));

        if (resolvedLotName != null && !resolvedLotName.equals(lotIdOrName)) {
            parkingSpotRepository.findByLotName(resolvedLotName)
                    .forEach(spot -> uniqueSpots.put(spot.getId(), ensureProperZoneName(spot)));
        }

        return new ArrayList<>(uniqueSpots.values());
    }
}
