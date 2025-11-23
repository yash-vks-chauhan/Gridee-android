package com.parking.app.repository;

import com.parking.app.model.BookingAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Repository for booking audit records
 */
@Repository
public interface BookingAuditRepository extends MongoRepository<BookingAudit, String> {


}
