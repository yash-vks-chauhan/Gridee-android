package com.parking.app.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Component
public class BookingScheduler {
    private final BookingService bookingService;
    private final ParkingSpotService parkingSpotService;

    @Autowired
    public BookingScheduler(BookingService bookingService, ParkingSpotService parkingSpotService) {
        this.bookingService = bookingService;
        this.parkingSpotService = parkingSpotService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void autoCompleteLateBookings() {
        bookingService.autoCompleteLateBookings();
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Asia/Kolkata")
    public void resetParkingSpotsAvailability() {
        parkingSpotService.resetParkingSpotsAvailability();
    }
}

