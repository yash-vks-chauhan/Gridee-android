package com.parking.app.service.booking;

import com.parking.app.model.Bookings;
import com.parking.app.model.Users;
import com.parking.app.repository.BookingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Service responsible for querying and retrieving booking data
 */
@Service
public class BookingQueryService {

    private final BookingRepository bookingRepository;
    private final MongoOperations mongoOperations;

    public BookingQueryService(BookingRepository bookingRepository, MongoOperations mongoOperations) {
        this.bookingRepository = bookingRepository;
        this.mongoOperations = mongoOperations;
    }

    public List<Bookings> getAllBookingsFiltered(String status, String lotId,
                                                  ZonedDateTime fromDate, ZonedDateTime toDate,
                                                  int page, int size) {
        Query query = new Query();
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (lotId != null && !lotId.isEmpty()) {
            query.addCriteria(Criteria.where("lotId").is(lotId));
        }
        if (fromDate != null) {
            query.addCriteria(Criteria.where("checkInTime").gte(Date.from(fromDate.toInstant())));
        }
        if (toDate != null) {
            query.addCriteria(Criteria.where("checkOutTime").lte(Date.from(toDate.toInstant())));
        }
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<Bookings> getBookingHistoryByUserId(String userId) {
        Date now = Date.from(ZonedDateTime.now().toInstant());
        Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("status").is("completed")
                        .and("checkOutTime").lt(now)
        );
        query.with(Sort.by(Sort.Direction.DESC, "checkOutTime"));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<Bookings> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Bookings getBookingById(String id) {
        return bookingRepository.findById(id).orElse(null);
    }

    public List<Bookings> getBookingsByUserId(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Bookings> findByLotIdAndTimeWindow(String lotId, ZonedDateTime startTime,
                                                    ZonedDateTime endTime) {
        return bookingRepository.findByLotIdAndTimeWindow(lotId, startTime, endTime);
    }

    public List<String> getVehicleNumbersByUserId(String userId, Users user) {
        return user.getVehicleNumbers();
    }
}

