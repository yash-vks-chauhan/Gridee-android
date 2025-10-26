package com.parking.app.model;

import com.parking.app.constants.BookingStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "bookings")
@CompoundIndexes({
    @CompoundIndex(name = "vehicleNumber_status_idx", def = "{'vehicleNumber': 1, 'status': 1}"),
    @CompoundIndex(name = "userId_status_idx", def = "{'userId': 1, 'status': 1}")
})
@Getter
@Setter
public class Bookings {

    // Field name constants for MongoDB queries
    public static final String FIELD_ID = "_id";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_LOT_ID = "lotId";
    public static final String FIELD_SPOT_ID = "spotId";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_QR_CODE = "qrCode";
    public static final String FIELD_CHECK_IN_TIME = "checkInTime";
    public static final String FIELD_CHECK_OUT_TIME = "checkOutTime";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_VEHICLE_NUMBER = "vehicleNumber";
    public static final String FIELD_QR_CODE_SCANNED = "qrCodeScanned";
    public static final String FIELD_ACTUAL_CHECK_IN_TIME = "actualCheckInTime";
    public static final String FIELD_AUTO_COMPLETED = "autoCompleted";
    public static final String FIELD_CHECK_IN_OPERATOR_ID = "checkInOperatorId";
    public static final String FIELD_CHECK_OUT_OPERATOR_ID = "checkOutOperatorId";

    @Id
    private String id;

    @Indexed
    private String userId;
    private String lotId;
    private String spotId;
    @Indexed
    private String status;     // e.g., "pending", "active", "cancelled"
    private double amount;     // optional payment amount
    private String qrCode;     // optional QR code for parking entry
    private Date checkInTime;
    private Date checkOutTime;
    private Date createdAt;
    @Indexed
    private String vehicleNumber;   // just the one picked for this booking
    private boolean qrCodeScanned;
    private Date actualCheckInTime;
    private Boolean autoCompleted;
    @Indexed
    private String checkInOperatorId;  // ID of the operator who performed check-in
    @Indexed
    private String checkOutOperatorId;  // ID of the operator who performed check-out

    public Bookings() {
        this.status = BookingStatus.PENDING.name();  // initialize status as pending
        this.createdAt = new Date();  // initialize createdAt as now
    }
}
