package com.parking.app.repository;

import com.parking.app.model.Bookings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Bookings, String> {
    List<Bookings> findBySpotId(String spotId);
    List<Bookings> findByUserId(String userId);
    List<Bookings> findByStatus(String status);
    List<Bookings> findBySpotIdAndStatusNot(String spotId, String status);
    @Query("{ 'lotId': ?0, 'startTime': { $lt: ?2 }, 'endTime': { $gt: ?1 } }")
    List<Bookings> findByLotIdAndTimeWindow(String lotId, ZonedDateTime startTime, ZonedDateTime endTime);

    // Optimized queries for check-in lookup - status indexed for performance
    Optional<Bookings> findByVehicleNumberAndStatus(String vehicleNumber, String status);
    Optional<Bookings> findByUserIdAndStatus(String userId, String status);
}
