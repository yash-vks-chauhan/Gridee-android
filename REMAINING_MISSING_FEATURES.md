# Remaining Missing Booking Features (Still Not Implemented)

## Summary
Based on analysis of the codebase after QR check-in/checkout implementation:

---

## ✅ **COMPLETED FEATURES** (Now Implemented)

### High Priority QR Features (DONE ✅):
1. ✅ **Check-In Functionality** - Repository methods exist, UI partially done
2. ✅ **Check-Out Functionality** - Repository methods exist, UI partially done  
3. ✅ **Validate QR Code for Check-In** - Repository method exists
4. ✅ **Validate QR Code for Check-Out** - Repository method exists
5. ✅ **Get Penalty Information** - Repository method exists
6. ✅ **Get Booking by ID (Refresh)** - Repository method exists

**Status:** Repository layer (BookingRepository.kt) has all 6 QR methods implemented:
- `validateCheckInQr()`
- `checkIn()`
- `validateCheckOutQr()`
- `checkOut()`
- `getPenaltyInfo()`
- `refreshBooking()`

**⚠️ ISSUE:** ApiService.kt is **MISSING** the corresponding API endpoint declarations!

---

## ❌ **STILL MISSING - Need to be Added**

### 1. **API Endpoints in ApiService.kt** ⚠️ CRITICAL
**File:** `/app/src/main/java/com/gridee/parking/data/api/ApiService.kt`

**Missing Endpoints:**
```kotlin
// QR Check-in endpoints
@POST("api/users/{userId}/bookings/{bookingId}/validate-qr-checkin")
suspend fun validateQrCodeForCheckIn(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String,
    @Body request: QrCodeRequest
): Response<QrValidationResult>

@POST("api/users/{userId}/bookings/{bookingId}/checkin")
suspend fun checkInBooking(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String,
    @Body request: QrCodeRequest
): Response<Booking>

// QR Check-out endpoints
@POST("api/users/{userId}/bookings/{bookingId}/validate-qr-checkout")
suspend fun validateQrCodeForCheckOut(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String,
    @Body request: QrCodeRequest
): Response<QrValidationResult>

@POST("api/users/{userId}/bookings/{bookingId}/checkout")
suspend fun checkOutBooking(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String,
    @Body request: QrCodeRequest
): Response<Booking>

// Penalty and booking info
@GET("api/users/{userId}/bookings/{bookingId}/penalty")
suspend fun getPenaltyInfo(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String
): Response<Double>

@GET("api/users/{userId}/bookings/{bookingId}")
suspend fun getBookingById(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String
): Response<Booking>
```

---

### 2. **Cancel Booking UI Implementation** ⚠️ MEDIUM PRIORITY
**File:** `/app/src/main/java/com/gridee/parking/ui/fragments/BookingsFragmentNew.kt`

**Current Status:** Line 978-980 shows:
```kotlin
sheetBinding.actionCancel.setOnClickListener {
    showToast("Cancel booking action coming soon")  // ⚠️ Not implemented!
    dialog.dismiss()
}
```

**What's Needed:**
- Replace toast with actual `cancelBooking()` API call
- Show confirmation dialog before canceling
- Handle success/error responses
- Refresh booking list after cancellation
- Move cancelled booking to COMPLETED tab

**Backend Support:** ✅ EXISTS
- Endpoint: `POST /api/users/{userId}/bookings/{bookingId}/cancel`
- Repository method: ✅ `cancelBooking()` exists in BookingRepository.kt

---

### 3. **Extend Booking** ❌ NOT STARTED
**Backend Endpoint:** `PUT /api/users/{userId}/bookings/{bookingId}/extend`

**What it does:**
- Extends booking duration for active bookings
- Checks spot availability for extended time
- Calculates additional charges
- Deducts from wallet
- Updates booking end time

**Frontend Implementation Needed:**
```kotlin
// Add to ApiService.kt
@PUT("api/users/{userId}/bookings/{bookingId}/extend")
suspend fun extendBooking(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String,
    @Body request: Map<String, String>  // {"newCheckOutTime": "2025-10-24T18:00:00+05:30"}
): Response<Booking>

// Add to BookingRepository.kt
suspend fun extendBooking(
    bookingId: String,
    newCheckOutTime: String
): Result<Booking>

// Add UI components:
// - "Extend Booking" button on active booking detail screen
// - Time picker dialog
// - Show additional charges
// - Confirmation dialog
```

---

### 4. **Get Booking Breakup** ❌ NOT STARTED
**Backend Endpoint:** `GET /api/users/{userId}/bookings/{bookingId}/breakup`

**What it does:**
- Returns detailed cost breakdown for completed/cancelled bookings
- Shows: base charge, penalties, refunds, total

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
```kotlin
// 1. Add data model
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

// 2. Add to ApiService.kt
@GET("api/users/{userId}/bookings/{bookingId}/breakup")
suspend fun getBookingBreakup(
    @Path("userId") userId: String,
    @Path("bookingId") bookingId: String
): Response<BookingBreakup>

// 3. Add to BookingRepository.kt
suspend fun getBookingBreakup(bookingId: String): Result<BookingBreakup>

// 4. Create UI screen/bottomsheet to display breakup
// 5. Add "View Receipt" button on completed bookings
```

---

### 5. **Update Booking Status (Admin)** ❌ NOT STARTED
**Backend Endpoint:** `PUT /api/users/{userId}/bookings/{bookingId}`

**Priority:** LOW (Admin feature)

**What it does:**
- Manual status override (pending → active → completed → cancelled)
- Admin/support feature

**Frontend Implementation:**
- Probably not needed for user-facing app
- Could be added to admin panel if created

---

### 6. **Delete Booking (Admin)** ❌ NOT STARTED
**Backend Endpoint:** `DELETE /api/users/{userId}/bookings/{bookingId}`

**Priority:** LOW (Testing/cleanup feature)

**What it does:**
- Permanently deletes booking record
- Used for cleanup/testing

**Frontend Implementation:**
- Not needed in production app
- Admin-only feature

---

### 7. **Get All Bookings (Admin)** ❌ NOT STARTED
**Backend Endpoint:** `GET /api/bookings`

**Priority:** LOW (Admin feature)

**What it does:**
- Fetch all bookings across all users
- Filtering by status, lot, date range
- Pagination support

**Frontend Implementation:**
- Admin dashboard feature
- Not needed for user app

---

### 8. **Get All Bookings for User (Legacy)** ❌ NOT STARTED
**Backend Endpoint:** `GET /api/users/{userId}/all-bookings`

**Priority:** LOW (Possibly redundant)

**What it does:**
- Gets ALL user bookings including cancelled
- Different from `/api/users/{userId}/bookings`

**Frontend Implementation:**
- Possibly redundant with current implementation
- App already handles cancelled bookings in COMPLETED tab

---

## 📊 **Updated Summary**

| Feature | Backend | ApiService.kt | Repository | UI | Priority | Status |
|---------|---------|---------------|------------|-----|----------|--------|
| **Check-In** | ✅ | ❌ MISSING | ✅ | ⚠️ Partial | HIGH | **BLOCKED** |
| **Check-Out** | ✅ | ❌ MISSING | ✅ | ⚠️ Partial | HIGH | **BLOCKED** |
| **Validate Check-In QR** | ✅ | ❌ MISSING | ✅ | ⚠️ Partial | HIGH | **BLOCKED** |
| **Validate Check-Out QR** | ✅ | ❌ MISSING | ✅ | ⚠️ Partial | HIGH | **BLOCKED** |
| **Get Penalty Info** | ✅ | ❌ MISSING | ✅ | ❌ | HIGH | **BLOCKED** |
| **Get Booking by ID** | ✅ | ❌ MISSING | ✅ | ⚠️ Partial | HIGH | **BLOCKED** |
| **Cancel Booking** | ✅ | ✅ | ✅ | ❌ Toast only | MEDIUM | **READY** |
| **Extend Booking** | ✅ | ❌ | ❌ | ❌ | MEDIUM | Not Started |
| **Get Breakup** | ✅ | ❌ | ❌ | ❌ | MEDIUM | Not Started |
| **Update Status (Admin)** | ✅ | ❌ | ❌ | ❌ | LOW | Not Started |
| **Delete Booking (Admin)** | ✅ | ❌ | ❌ | ❌ | LOW | Not Started |
| **Get All Bookings (Admin)** | ✅ | ❌ | ❌ | ❌ | LOW | Not Started |
| **Get All User Bookings** | ✅ | ❌ | ❌ | ❌ | LOW | Not Started |

---

## 🚨 **CRITICAL BLOCKER**

### **Missing API Endpoints in ApiService.kt**

**Issue:** The BookingRepository.kt calls these methods but ApiService.kt doesn't have them:
- `validateQrCodeForCheckIn()`
- `checkInBooking()`
- `validateQrCodeForCheckOut()`
- `checkOutBooking()`
- `getPenaltyInfo()`
- `getBookingById()`

**Impact:** QR check-in/checkout **WILL NOT WORK** until these endpoints are added to ApiService.kt

**Solution:** Add all 6 missing endpoint declarations to ApiService.kt (see section 1 above)

---

## 🎯 **Recommended Implementation Order**

### Phase 1: Fix Critical Blocker (URGENT)
1. ✅ Add 6 missing QR endpoints to ApiService.kt
2. ✅ Test QR check-in flow end-to-end
3. ✅ Test QR check-out flow end-to-end

### Phase 2: Complete Medium Priority (Next Sprint)
4. ✅ Implement Cancel Booking UI (currently just toast)
5. ✅ Implement Extend Booking feature
6. ✅ Implement Get Booking Breakup (receipt screen)

### Phase 3: Polish (Future)
7. Add live penalty counter to active bookings
8. Add pull-to-refresh on booking details
9. Improve error handling and UX

### Phase 4: Admin Features (Optional)
10. Update booking status (admin)
11. Delete booking (admin)
12. Get all bookings (admin dashboard)

---

## 📝 **Action Items**

### Immediate (This Sprint):
- [ ] Add 6 QR endpoints to ApiService.kt
- [ ] Add QrCodeRequest and QrValidationResult data models
- [ ] Test QR check-in/checkout flows
- [ ] Implement Cancel Booking UI (remove toast, add real functionality)

### Next Sprint:
- [ ] Implement Extend Booking feature
- [ ] Implement Booking Breakup/Receipt screen
- [ ] Add BookingBreakup data model
- [ ] Add live penalty tracking

### Future:
- [ ] Admin features (if needed)
- [ ] Enhanced error handling
- [ ] Offline support
- [ ] Push notifications for booking reminders

---

## 🔧 **Files to Modify**

### High Priority:
1. ✅ `/app/src/main/java/com/gridee/parking/data/api/ApiService.kt` - Add 6 QR endpoints
2. ✅ `/app/src/main/java/com/gridee/parking/data/model/QrModels.kt` - Add QrCodeRequest, QrValidationResult
3. ✅ `/app/src/main/java/com/gridee/parking/ui/fragments/BookingsFragmentNew.kt` - Line 978: Implement cancel booking

### Medium Priority:
4. `/app/src/main/java/com/gridee/parking/data/model/BookingBreakup.kt` - Create new file
5. ApiService.kt - Add `extendBooking()` and `getBookingBreakup()`
6. BookingRepository.kt - Add extend and breakup methods
7. Create new UI screens/dialogs for extend and breakup

---

## 📚 **Reference**

- Original analysis: `MISSING_BOOKING_FEATURES.md`
- QR implementation plan: `QR_CHECKIN_CHECKOUT_IMPLEMENTATION_PLAN.md`
- Backend controller: `/src/main/java/com/parking/app/controller/BookingController.java`
- Backend service: `/src/main/java/com/parking/app/service/BookingService.java`

---

**Last Updated:** October 25, 2025  
**Status:** QR Repository layer complete, ApiService.kt endpoints MISSING
