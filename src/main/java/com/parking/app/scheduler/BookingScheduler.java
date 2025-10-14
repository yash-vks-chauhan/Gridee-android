package com.parking.app.scheduler;

import com.parking.app.service.BookingService;
import com.parking.app.service.ParkingSpotService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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