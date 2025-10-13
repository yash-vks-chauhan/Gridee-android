package com.parking.app.service;

import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.repository.ParkingSpotRepository;
import com.parking.app.repository.ParkingLotRepository;
import com.parking.app.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ParkingSpotService {

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private BookingRepository bookingRepository;

    public ParkingSpot findById(String id) {
        return parkingSpotRepository.findById(id).orElse(null);
    }

    public ParkingSpot createParkingSpot(ParkingSpot spot) {
        if (spot.getAvailable() == 0) {
            spot.setAvailable(spot.getCapacity());
        }
        setProperZoneName(spot);
        return parkingSpotRepository.save(spot);
    }

    public List<ParkingSpot> getAllParkingSpots() {
        List<ParkingSpot> spots = parkingSpotRepository.findAll();
        return spots.stream()
                .map(this::ensureProperZoneName)
                .collect(Collectors.toList());
    }

    public List<ParkingSpot> getParkingSpotsByLotId(String lotId) {
        List<ParkingSpot> spots = parkingSpotRepository.findByLotId(lotId);
        return spots.stream()
                .map(this::ensureProperZoneName)
                .collect(Collectors.toList());
    }

    public ParkingSpot getParkingSpotById(String spotId) {
        ParkingSpot spot = parkingSpotRepository.findById(spotId).orElse(null);
        if (spot != null) {
            return ensureProperZoneName(spot);
        }
        return null;
    }

    private void setProperZoneName(ParkingSpot spot) {
        if (spot.getZoneName() == null ||
                spot.getZoneName().isEmpty() ||
                spot.getZoneName().equals("nil") ||
                spot.getZoneName().equals("null")) {

            String spotId = spot.getId();
            String zoneName = generateZoneNameFromId(spotId);
            spot.setZoneName(zoneName);
        }
    }

    private ParkingSpot ensureProperZoneName(ParkingSpot spot) {
        boolean needsUpdate = false;

        if (spot.getZoneName() == null ||
                spot.getZoneName().isEmpty() ||
                spot.getZoneName().equals("nil") ||
                spot.getZoneName().equals("null")) {

            String zoneName = generateZoneNameFromId(spot.getId());
            spot.setZoneName(zoneName);
            needsUpdate = true;
        }

        if (needsUpdate) {
            System.out.println("🔧 Updating zone name for spot " + spot.getId() + " to: " + spot.getZoneName());
            parkingSpotRepository.save(spot);
        }

        return spot;
    }

    private String generateZoneNameFromId(String spotId) {
        if (spotId == null || spotId.isEmpty()) {
            return "Unknown Zone";
        }
        switch (spotId.toLowerCase()) {
            case "ps1":
                return "Main Parking Zone";
            case "ps2":
                return "Secondary Parking Zone";
            case "ps3":
                return "Tertiary Parking Zone";
            default:
                if (spotId.toLowerCase().startsWith("ps")) {
                    String number = spotId.substring(2);
                    return "Parking Zone " + number.toUpperCase();
                } else if (spotId.contains("-")) {
                    String[] parts = spotId.split("-");
                    return parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1) + " Area";
                } else {
                    return "Zone " + spotId.toUpperCase();
                }
        }
    }

    public void fixAllZoneNames() {
        System.out.println("🔧 Starting zone name fix for all parking spots...");
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();

        int updatedCount = 0;
        for (ParkingSpot spot : allSpots) {
            if (spot.getZoneName() == null ||
                    spot.getZoneName().equals("nil") ||
                    spot.getZoneName().equals("null") ||
                    spot.getZoneName().isEmpty()) {

                String newZoneName = generateZoneNameFromId(spot.getId());
                spot.setZoneName(newZoneName);
                parkingSpotRepository.save(spot);
                updatedCount++;

                System.out.println("✅ Updated spot " + spot.getId() + " with zone name: " + newZoneName);
            }
        }

        System.out.println("🎉 Zone name fix complete! Updated " + updatedCount + " parking spots.");
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

    public ParkingSpot holdSpot(String spotId, String userId) {
        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update holdUpdate = new Update()
                .inc("available", -1)
                .set("heldBy", userId)
                .set("heldAt", new Date());

        return mongoOperations.findAndModify(spotQuery, holdUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    public ParkingSpot releaseSpot(String spotId) {
        Query spotQuery = new Query(Criteria.where("_id").is(spotId));
        Update releaseUpdate = new Update()
                .inc("available", 1)
                .unset("heldBy")
                .unset("heldAt");

        return mongoOperations.findAndModify(spotQuery, releaseUpdate,
                FindAndModifyOptions.options().returnNew(true), ParkingSpot.class);
    }

    // NEW: Reset all parking spots to full capacity
    public void resetAllSpotsCapacity() {
        List<ParkingSpot> allSpots = parkingSpotRepository.findAll();
        for (ParkingSpot spot : allSpots) {
            spot.setAvailable(spot.getCapacity());
            parkingSpotRepository.save(spot);
        }
        System.out.println("✅ All parking spots have been reset to full capacity.");
    }

    // NEW: Get available spots for a time window
    public List<ParkingSpot> getAvailableSpots(String lotId, ZonedDateTime startTime, ZonedDateTime endTime,List<Bookings> overlappingBookings) {
        List<ParkingSpot> allSpots = parkingSpotRepository.findByLotId(lotId);
        Set<String> bookedSpotIds = overlappingBookings.stream()
                .map(Bookings::getSpotId)
                .collect(Collectors.toSet());
        return allSpots.stream()
                .filter(spot -> !bookedSpotIds.contains(spot.getId()))
                .collect(Collectors.toList());
    }

    public void decrementSpotAvailability(String spotId) {
        Query spotQuery = new Query(Criteria.where("_id").is(spotId).and("available").gt(0));
        Update decUpdate = new Update().inc("available", -1);
        ParkingSpot updatedSpot = mongoOperations.findAndModify(
                spotQuery, decUpdate, ParkingSpot.class);
        if (updatedSpot == null) {
            throw new RuntimeException("No spots available");
        }
    }

    public void incrementSpotAvailability(String spotId) {
        Update incUpdate = new Update().inc("available", 1);
        mongoOperations.updateFirst(new Query(Criteria.where("_id").is(spotId)), incUpdate, ParkingSpot.class);
        ensureAvailableNotExceedCapacity(spotId);
    }

    public void ensureAvailableNotExceedCapacity(String spotId) {
        Optional<ParkingSpot> spot = parkingSpotRepository.findById(spotId);
        if (spot.isPresent() && spot.get().getAvailable() > spot.get().getCapacity()) {
            spot.get().setAvailable(spot.get().getCapacity());
            mongoOperations.save(spot);
        }
    }

    public void resetParkingSpotsAvailability() {
        List<ParkingSpot> spots = mongoOperations.findAll(ParkingSpot.class);
        for (ParkingSpot spot : spots) {
            spot.setAvailable(spot.getCapacity());
            mongoOperations.save(spot);
        }
    }
}
