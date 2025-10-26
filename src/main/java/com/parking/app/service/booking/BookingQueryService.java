package com.parking.app.service.booking;

import com.parking.app.constants.BookingStatus;
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
            query.addCriteria(Criteria.where(Bookings.FIELD_STATUS).is(status));
        }
        if (lotId != null && !lotId.isEmpty()) {
            query.addCriteria(Criteria.where(Bookings.FIELD_LOT_ID).is(lotId));
        }
        if (fromDate != null) {
            query.addCriteria(Criteria.where(Bookings.FIELD_CHECK_IN_TIME).gte(Date.from(fromDate.toInstant())));
        }
        if (toDate != null) {
            query.addCriteria(Criteria.where(Bookings.FIELD_CHECK_OUT_TIME).lte(Date.from(toDate.toInstant())));
        }
        query.with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, Bookings.FIELD_CREATED_AT)));
        return mongoOperations.find(query, Bookings.class);
    }

    public List<Bookings> getBookingHistoryByUserId(String userId) {
        Date now = Date.from(ZonedDateTime.now().toInstant());
        Query query = new Query(
                Criteria.where(Bookings.FIELD_USER_ID).is(userId)
                        .and(Bookings.FIELD_STATUS).is(BookingStatus.COMPLETED.name())
                        .and(Bookings.FIELD_CHECK_OUT_TIME).lt(now)
        );
        query.with(Sort.by(Sort.Direction.DESC, Bookings.FIELD_CHECK_OUT_TIME));
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
