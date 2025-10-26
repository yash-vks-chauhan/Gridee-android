# Missing Booking Features in Frontend

## Overview
This document lists all the booking-related features that are **implemented in the backend** but **NOT implemented in the Android frontend**.

---

## ✅ Currently Implemented in Frontend

1. **Start Booking** - `POST /api/users/{userId}/bookings/start`
2. **Confirm Booking** - `POST /api/users/{userId}/bookings/{bookingId}/confirm`
3. **Cancel Booking** - `POST /api/users/{userId}/bookings/{bookingId}/cancel`
4. **Get User Bookings** - `GET /api/users/{userId}/bookings`
5. **Get Booking History** - `GET /api/users/{userId}/bookings/history`

---

## ❌ Missing Backend Features in Frontend

### 1. **Check-In Functionality**
**Backend Endpoint:** `POST /api/users/{userId}/bookings/{bookingId}/checkin`

**What it does:**
- Allows user to check-in to their parking spot using QR code
- Validates QR code matches booking ID
- Changes booking status from "pending" to "active"
- Records actual check-in time
- Calculates late check-in penalty (grace period: 10 minutes)

**Request Body:**
```json
{
  "qrCode": "booking_id_as_qr"
}
```

**Frontend Implementation Needed:**
- QR Scanner screen/activity
- Check-in button on booking detail screen
- Handle QR code validation
- Display check-in confirmation
- Show late penalty warning if applicable

---

### 2. **Check-Out Functionality**
**Backend Endpoint:** `POST /api/users/{userId}/bookings/{bookingId}/checkout`

**What it does:**
- Allows user to check-out from parking spot using QR code
- Validates QR code matches booking ID
- Changes booking status from "active" to "completed"
- Calculates late check-out penalty (grace period: 10 minutes)
- Deducts penalties from wallet
- Releases parking spot (increments availability)
- Applies refund/breakup

**Request Body:**
```json
{
  "qrCode": "booking_id_as_qr"
}
```

**Frontend Implementation Needed:**
- QR Scanner for check-out
- Check-out button on active booking screen
- Display final charges breakdown
- Show penalties if applicable
- Confirmation screen with receipt

---

### 3. **Validate QR Code for Check-In**
**Backend Endpoint:** `POST /api/users/{userId}/bookings/{bookingId}/validate-qr-checkin`

**What it does:**
- Pre-validates QR code before actual check-in
- Returns if QR is valid and any penalty amount
- Allows showing penalty warning to user before check-in

**Request Body:**
```json
{
  "qrCode": "booking_id_as_qr"
}
```

**Response:**
```json
{
  "valid": true,
  "penalty": 50.0,
  "message": "Penalty applies for late check-in"
}
```

**Frontend Implementation Needed:**
- Pre-validation before check-in
- Show penalty warning dialog
- Allow user to confirm check-in with penalty

---

### 4. **Validate QR Code for Check-Out**
**Backend Endpoint:** `POST /api/users/{userId}/bookings/{bookingId}/validate-qr-checkout`

**What it does:**
- Pre-validates QR code before actual check-out
- Returns if QR is valid and any penalty amount
- Shows user final charges before check-out

**Request Body:**
```json
{
  "qrCode": "booking_id_as_qr"
}
```

**Response:**
```json
{
  "valid": true,
  "penalty": 100.0,
  "message": "Penalty applies for late check-out"
}
```

**Frontend Implementation Needed:**
- Pre-validation before check-out
- Show final charges dialog
- Allow user to confirm check-out with charges

---

### 5. **Extend Booking**
**Backend Endpoint:** `PUT /api/users/{userId}/bookings/{bookingId}/extend`

**What it does:**
- Extends booking duration for active bookings
- Checks if spot is available for extended time
- Calculates additional charges
- Deducts from wallet
- Updates booking end time

**Request Body:**
```json
{
  "newCheckOutTime": "2025-10-24T18:00:00+05:30"
}
```

**Frontend Implementation Needed:**
- "Extend Booking" button on active booking screen
- Time picker to select new end time
- Show additional charges calculation
- Confirm extension dialog
- Handle insufficient balance error

---

### 6. **Get Booking Breakup**
**Backend Endpoint:** `GET /api/users/{userId}/bookings/{bookingId}/breakup`

**What it does:**
- Returns detailed cost breakdown for completed/cancelled bookings
- Shows booking charge, penalties, refunds
- Displays:
  - Base booking charge
  - Late check-in penalty
  - Late check-out penalty
  - Subtotal
  - Refund amount
  - Total deducted

**Response Example:**
```json
{
  "bookingCharge": 200.0,
  "lateCheckInPenalty": 50.0,
  "lateCheckOutPenalty": 100.0,
  "subtotal": 350.0,
  "refundAmount": 0.0,
  "totalDeducted": 350.0,
  "status": "completed",
  "bookingRate": 100.0,
  "checkInPenaltyRate": 5.0,
  "checkOutPenaltyRate": 10.0,
  "autoCompleted": false
}
```

**Frontend Implementation Needed:**
- Booking receipt/invoice screen
- Show itemized cost breakdown
- Display on completed booking detail screen
- Show in transaction history

---

### 7. **Get Penalty Information**
**Backend Endpoint:** `GET /api/users/{userId}/bookings/{bookingId}/penalty`

**What it does:**
- Calculates real-time penalty for late check-out
- Returns penalty amount based on minutes late (10-minute grace period)
- Used for showing live penalty counter

**Response:**
```json
50.0
```

**Frontend Implementation Needed:**
- Live penalty counter on active booking screen
- Warning when approaching scheduled end time
- Red alert when overtime
- Display penalty rate per minute

---

### 8. **Get Booking by ID**
**Backend Endpoint:** `GET /api/users/{userId}/bookings/{bookingId}`

**What it does:**
- Fetches detailed information for a single booking
- Used for refreshing booking status
- Gets latest data for booking detail screen

**Frontend Implementation Needed:**
- Pull-to-refresh on booking detail screen
- Real-time booking status updates
- Sync booking data

---

### 9. **Update Booking Status**
**Backend Endpoint:** `PUT /api/users/{userId}/bookings/{bookingId}`

**What it does:**
- Admin/manual status update for booking
- Changes status (pending, active, completed, cancelled)

**Request Body:**
```json
{
  "status": "active"
}
```

**Frontend Implementation Needed:**
- Admin panel (if needed)
- Manual status override
- Emergency cancel/complete

---

### 10. **Delete Booking**
**Backend Endpoint:** `DELETE /api/users/{userId}/bookings/{bookingId}`

**What it does:**
- Permanently deletes a booking record
- Used for cleanup/testing

**Frontend Implementation Needed:**
- Probably not needed in production
- Could be admin-only feature

---

### 11. **Get All Bookings (Admin)**
**Backend Endpoint:** `GET /api/bookings`

**What it does:**
- Admin endpoint to get all bookings
- Supports filtering by:
  - Status
  - Lot ID
  - Date range
  - Pagination

**Query Parameters:**
- `status` - Filter by booking status
- `lotId` - Filter by parking lot
- `fromDate` - Start date
- `toDate` - End date
- `page` - Page number
- `size` - Page size

**Frontend Implementation Needed:**
- Admin dashboard
- Booking management screen
- Filtering and search

---

### 12. **Get All Bookings for User (Legacy)**
**Backend Endpoint:** `GET /api/users/{userId}/all-bookings`

**What it does:**
- Gets ALL user bookings (not just active/completed)
- Includes cancelled bookings
- Different from `/api/users/{userId}/bookings`

**Frontend Implementation Needed:**
- Complete booking history screen
- Filter by all statuses
- Search and sort

---

## 📊 Summary

### Total Backend Booking Endpoints: 17
### Implemented in Frontend: 5
### **Missing in Frontend: 12**

---

## 🎯 Priority Implementation Order

### **High Priority (Core Features)**
1. ✅ **Check-In Functionality** - Essential for parking flow
2. ✅ **Check-Out Functionality** - Essential for parking flow
3. ✅ **QR Code Validation (both check-in/out)** - User experience
4. ✅ **Get Booking Breakup** - Transparency on charges
5. ✅ **Extend Booking** - User convenience

### **Medium Priority (Enhanced Features)**
6. **Get Penalty Information** - Live penalty tracking
7. **Get Booking by ID** - Refresh booking data
8. **Get All Bookings for User** - Complete history

### **Low Priority (Admin/Edge Cases)**
9. Update Booking Status (Admin)
10. Delete Booking (Admin)
11. Get All Bookings (Admin)

---

## 🔧 Implementation Requirements

### New Screens Needed:
1. **QR Scanner Screen** - For check-in/check-out
2. **Booking Receipt Screen** - Show detailed breakup
3. **Extend Booking Dialog** - Time picker + charges
4. **Penalty Warning Dialog** - Show before check-in/out with penalty

### New UI Components:
1. **Live Penalty Counter** - Show real-time penalty on active booking
2. **Breakup Card** - Itemized cost display
3. **QR Code Display** - Show user's booking QR code
4. **Time Extension Picker** - Select new end time

### Updates to Existing Screens:
1. **Booking Detail Screen** - Add check-in, check-out, extend buttons
2. **Active Bookings Tab** - Show penalty warnings
3. **Completed Bookings Tab** - Show breakup summary

### New Data Models Needed:
```kotlin
data class QrValidationResult(
    val valid: Boolean,
    val penalty: Double,
    val message: String
)

data class BookingBreakup(
    val bookingCharge: Double,
    val lateCheckInPenalty: Double,
    val lateCheckOutPenalty: Double,
    val subtotal: Double,
    val refundAmount: Double,
    val totalDeducted: Double,
    val status: String,
    val bookingRate: Double,
    val checkInPenaltyRate: Double,
    val checkOutPenaltyRate: Double,
    val autoCompleted: Boolean
)

data class ExtendBookingRequest(
    val newCheckOutTime: String
)

data class QrCodeRequest(
    val qrCode: String
)
```

---

## 📝 Backend Model vs Frontend Model

### Backend Model (Bookings.java):
```java
- String id
- String userId
- String lotId
- String spotId
- String status
- double amount
- String qrCode
- Date checkInTime
- Date checkOutTime
- Date createdAt
- String vehicleNumber
- boolean qrCodeScanned        // ❌ Missing in frontend
- Date actualCheckInTime        // ❌ Missing in frontend
- Boolean autoCompleted         // ❌ Missing in frontend
```

### Frontend Model (Booking.kt):
```kotlin
- String? id
- String userId
- String lotId
- String spotId
- String status
- Double amount
- String? qrCode
- Date? checkInTime
- Date? checkOutTime
- Date? createdAt
- String? vehicleNumber
// Missing fields:
// - qrCodeScanned
// - actualCheckInTime
// - autoCompleted
```

### Action Required:
Update `Booking.kt` to include missing fields:
```kotlin
data class Booking(
    // ... existing fields ...
    
    @SerializedName("qrCodeScanned")
    val qrCodeScanned: Boolean = false,
    
    @SerializedName("actualCheckInTime")
    val actualCheckInTime: Date? = null,
    
    @SerializedName("autoCompleted")
    val autoCompleted: Boolean? = false
)
```

---

## 🚀 Next Steps

1. **Update Booking data model** - Add missing fields
2. **Update ApiService.kt** - Add missing API endpoints
3. **Update BookingRepository.kt** - Add missing repository methods
4. **Implement QR Scanner** - Use ZXing library
5. **Create Booking Detail screens** - With check-in/out/extend
6. **Add Penalty tracking** - Live counter
7. **Implement Receipt screen** - Show booking breakup
8. **Update BookingsViewModel** - Add new state management
9. **Test with real backend** - Ensure compatibility

---

## 📚 Related Documentation
- Backend Controller: `/src/main/java/com/parking/app/controller/BookingController.java`
- Backend Service: `/src/main/java/com/parking/app/service/BookingService.java`
- Backend Model: `/src/main/java/com/parking/app/model/Bookings.java`
- Frontend API: `/app/src/main/java/com/gridee/parking/data/api/ApiService.kt`
- Frontend Repository: `/app/src/main/java/com/gridee/parking/data/repository/BookingRepository.kt`
- Frontend Model: `/app/src/main/java/com/gridee/parking/data/model/Booking.kt`
