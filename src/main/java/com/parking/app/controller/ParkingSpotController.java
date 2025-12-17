package com.parking.app.controller;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.NotFoundException;
import com.parking.app.model.Bookings;
import com.parking.app.model.ParkingSpot;
import com.parking.app.service.BookingService;
import com.parking.app.service.ParkingSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/parking-spots")
public class ParkingSpotController {

    @Autowired
    private ParkingSpotService parkingSpotService;

    @Autowired
    private BookingService bookingService;

    // =======================
    // BASIC FETCH APIS
    // =======================

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ParkingSpot>> getAllParkingSpots() {
        return ResponseEntity.ok(parkingSpotService.getAllParkingSpots());
    }

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<ParkingSpot>> getParkingSpotsByLot(@PathVariable String lotId) {
        return ResponseEntity.ok(parkingSpotService.getSpotsByLotId(lotId));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ParkingSpot> getParkingSpotById(@PathVariable String id) {
        ParkingSpot spot = parkingSpotService.getParkingSpotById(id);
        if (spot == null) {
            throw new NotFoundException("Parking spot not found with id: " + id);
        }
        return ResponseEntity.ok(spot);
    }

    // =======================
    // AVAILABILITY
    // =======================

    @GetMapping("/available")
    public ResponseEntity<List<ParkingSpot>> getAvailableSpots(
            @RequestParam String lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endTime) {
        List<Bookings> overlappingBookings = bookingService.findByLotIdAndTimeWindow(lotId, startTime, endTime);

        List<ParkingSpot> availableSpots = parkingSpotService.getAvailableSpots(lotId, startTime, endTime,
                overlappingBookings);

        return ResponseEntity.ok(availableSpots);
    }

    // =======================
    // ADMIN CRUD
    // =======================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingSpot> createParkingSpot(@RequestBody ParkingSpot spot) {
        ParkingSpot created = parkingSpotService.createParkingSpot(spot);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParkingSpot> updateParkingSpot(
            @PathVariable String id,
            @RequestBody ParkingSpot spotDetails) {
        ParkingSpot updated = parkingSpotService.updateParkingSpot(id, spotDetails);
        if (updated == null) {
            throw new NotFoundException("Parking spot not found with id: " + id);
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteParkingSpot(@PathVariable String id) {
        parkingSpotService.deleteParkingSpot(id);
        return ResponseEntity.noContent().build();
    }

    // =======================
    // HOLD / RELEASE (LOCKING)
    // =======================

    @PostMapping("/id/{id}/hold")
    public ResponseEntity<ParkingSpot> holdSpot(
            @PathVariable String id,
            @RequestParam String userId) {
        ParkingSpot spot = parkingSpotService.holdSpot(id, userId);
        if (spot == null) {
            throw new ConflictException("No availability for parking spot with id: " + id);
        }
        return ResponseEntity.ok(spot);
    }

    @PostMapping("/id/{id}/release")
    public ResponseEntity<ParkingSpot> releaseSpot(@PathVariable String id) {
        ParkingSpot spot = parkingSpotService.releaseSpot(id);
        if (spot == null) {
            throw new NotFoundException("Parking spot not found with id: " + id);
        }
        return ResponseEntity.ok(spot);
    }

    // =======================
    // MAINTENANCE / ADMIN
    // =======================

    @GetMapping("/fix-zone-names")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> fixZoneNames() {
        parkingSpotService.fixAllZoneNames();
        return ResponseEntity.ok("Zone names have been fixed for all parking spots");
    }

    @PostMapping("/reset-capacity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetAllSpotsCapacity() {
        parkingSpotService.resetAllSpotsCapacity();
        return ResponseEntity.ok("All parking spots reset to full capacity");
    }

    @PostMapping("/admin/reset-spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetAllSpots() {
        parkingSpotService.resetAllSpotsCapacity();
        return ResponseEntity.ok("All parking spots reset to max capacity");
    }
}
