# 🚀 QUICK REFERENCE CARD

## What This Feature Does

**Normal User:**
- Login → Book parking → Get QR code → Scan QR to check-in

**Operator:**
- Login → Open scanner → Scan license plate → Auto check-in ANY vehicle

---

## Key Files Created ✅

```
✅ CheckInMode.kt           - Enum (QR_CODE, VEHICLE_NUMBER, PIN)
✅ CheckInRequest.kt        - API request model
✅ OperatorViewModel.kt     - Business logic
✅ OperatorDashboardActivity.kt - Operator UI
✅ activity_operator_dashboard.xml - Layout
```

---

## 4 Quick Changes Needed 🔧

```
1. LoginActivity.kt      → Add: if (role == "OPERATOR") navigate to operator screen
2. ApiService.kt         → Add: operatorCheckIn() and operatorCheckOut() endpoints  
3. BookingRepository.kt  → Add: 2 wrapper methods for operator APIs
4. AndroidManifest.xml   → Add: <activity name="OperatorDashboardActivity" />
```

---

## API Endpoints

### User (needs bookingId)
```
POST /api/bookings/{userId}/checkin/{bookingId}
Body: { "mode": "QR_CODE", "qrCode": "..." }
Auth: JWT with role=USER
```

### Operator (no bookingId needed)
```
POST /api/bookings/checkin
Body: { "mode": "VEHICLE_NUMBER", "vehicleNumber": "ABC123" }
Auth: JWT with role=OPERATOR
```

---

## Decision Flow

```
Login → JWT contains role
  ↓
  ├─→ "USER" → MainContainerActivity (booking dashboard)
  └─→ "OPERATOR" → OperatorDashboardActivity (scanner)
```

---

## Testing

**User Test:**
```
1. Login: user@test.com
2. Should see: Booking dashboard
3. Can: Create bookings, scan QR codes
```

**Operator Test:**
```
1. Login: operator@test.com
2. Should see: Scanner interface
3. Can: Scan plates, check-in ANY vehicle
```

---

## Debug Commands

```kotlin
// Check role after login:
Log.d("AUTH", "Role: ${authResponse.role}")

// Check saved role:
val role = sharedPref.getString("user_role", "")

// Test API call:
viewModel.checkInByVehicleNumber("ABC123")
```

---

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| 401 | No JWT token | Check SharedPreferences has "auth_token" |
| 403 | Wrong role | Verify JWT has correct role |
| 404 | No booking | Vehicle number doesn't match any booking |

---

## Next Steps

1. ✅ Make 4 code changes (see EXACT_CODE_CHANGES.md)
2. 🔨 Build project
3. 🧪 Test user login
4. 🧪 Test operator login
5. 🎉 Done!

---

## Documentation Files

📚 **ROLE_BASED_AUTHENTICATION_GUIDE.md** - Full technical guide
📋 **QUICK_START_ROLE_AUTH.md** - Implementation walkthrough  
🎯 **IMPLEMENTATION_SUMMARY.md** - Complete overview
✏️ **EXACT_CODE_CHANGES.md** - Step-by-step changes (START HERE!)

---

## Contact Backend Team For

- Create test accounts (user@test.com, operator@test.com)
- Verify JWT includes "role" field
- Test operator endpoints are accessible
- Check database has vehicle numbers in bookings

---

**TL;DR:** Your backend is ready. Make 4 small code changes. Test both login flows. You're done! 🚀
