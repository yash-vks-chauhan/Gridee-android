# 🎯 Quick Start: Implementing Role-Based Authentication

## What You're Building

A parking app where:
- **Normal Users** → Book parking spots, check-in with QR codes
- **Operators** → Scan vehicle license plates, check-in/out any vehicle

---

## 🔄 How It Works

### Login Flow

```
┌─────────────┐
│ Login Screen│
└──────┬──────┘
       │
       ├──→ Backend checks user role in JWT token
       │
       ├──→ Role = "USER"
       │    └──→ Navigate to MainContainerActivity
       │         (Normal user dashboard)
       │
       └──→ Role = "OPERATOR"
            └──→ Navigate to OperatorDashboardActivity
                 (Scanner interface)
```

### Backend API Flow

**Your backend already supports this!** Look at your code:

```java
@PostMapping("checkin")
@PreAuthorize("hasRole('OPERATOR')")  // ← Only operators can access
public ResponseEntity<?> checkIn(@RequestBody CheckInRequestDto request) {
    // Operator checks in ANY vehicle by scanning plate number
    bookingService.checkIn(
        null,  // No bookingId needed!
        request.getMode(),  // VEHICLE_NUMBER
        request.getVehicleNumber(),  // ABC123 (scanned)
        ...
    );
}
```

**vs User endpoint:**

```java
@PostMapping("/{userId}/checkin/{bookingId}")
@PreAuthorize("hasRole('USER')")  // ← Only users
public ResponseEntity<?> checkInWithBookingId(...) {
    // User needs their specific bookingId
    bookingService.checkIn(
        bookingId,  // Must provide!
        request.getMode(),  // QR_CODE
        request.getQrCode(),  // QR from booking
        ...
    );
}
```

---

## 📱 What You Need to Build (Android)

### 1. **Already Done** ✅
- Your `AuthResponse.kt` already has `role: String`
- Your JWT token already includes the role
- Your backend endpoints are ready

### 2. **Files I Created for You** ✅

#### `CheckInMode.kt`
```kotlin
enum class CheckInMode {
    QR_CODE,           // For users
    VEHICLE_NUMBER,    // For operators (scan license plate)
    PIN               // Alternative method
}
```

#### `CheckInRequest.kt`
```kotlin
data class CheckInRequest(
    val mode: CheckInMode,
    val vehicleNumber: String? = null,  // ← Operator uses this
    val qrCode: String? = null,         // ← User uses this
    val pin: String? = null
)
```

#### `OperatorViewModel.kt`
```kotlin
fun checkInByVehicleNumber(vehicleNumber: String) {
    // Calls: POST /api/bookings/checkin
    // Body: { "mode": "VEHICLE_NUMBER", "vehicleNumber": "ABC123" }
}
```

### 3. **Files You Need to Create** 🔨

#### A. `OperatorDashboardActivity.kt`

**Layout:** Big "Scan Vehicle" button → Opens camera → OCR reads plate → Auto check-in

```kotlin
class OperatorDashboardActivity : AppCompatActivity() {
    private val viewModel: OperatorViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show scanner UI
        
        binding.btnScanVehicle.setOnClickListener {
            // Open QrScannerActivity (you already have this!)
            // But configure it to scan license plates instead of QR
            openVehicleScanner()
        }
    }
    
    private fun openVehicleScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        vehicleScannerLauncher.launch(intent)
    }
    
    // When scanner returns a result:
    private val vehicleScannerLauncher = registerForActivityResult(...) { result ->
        val vehicleNumber = result.data?.getStringExtra("qr_code")  // Reuse QR field
        viewModel.checkInByVehicleNumber(vehicleNumber!!)
    }
}
```

#### B. Update `LoginActivity.kt`

Add this to your existing `observeViewModel()`:

```kotlin
private fun observeViewModel() {
    viewModel.loginState.observe(this) { state ->
        when (state) {
            is LoginState.Success -> {
                // ✅ CHECK THE ROLE!
                when (state.authResponse.role.uppercase()) {
                    "OPERATOR" -> {
                        // Go to operator dashboard
                        startActivity(Intent(this, OperatorDashboardActivity::class.java))
                    }
                    "USER" -> {
                        // Go to normal user dashboard
                        startActivity(Intent(this, MainContainerActivity::class.java))
                    }
                }
                finish()
            }
            // ... other states
        }
    }
}
```

#### C. Update `ApiService.kt`

Add these two endpoints:

```kotlin
interface ApiService {
    
    // Operator check-in (no userId/bookingId required)
    @POST("api/bookings/checkin")
    suspend fun operatorCheckIn(@Body request: CheckInRequest): Response<Booking>
    
    // Operator check-out
    @POST("api/bookings/checkout")
    suspend fun operatorCheckOut(@Body request: CheckInRequest): Response<Booking>
}
```

#### D. Update `BookingRepository.kt`

```kotlin
class BookingRepository {
    
    suspend fun operatorCheckIn(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckIn(request)
    }
    
    suspend fun operatorCheckOut(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckOut(request)
    }
}
```

---

## 🧪 Testing Steps

### 1. **Create Test Accounts**

Ask your backend team to create:
```
User:     user@test.com      → role: "USER"
Operator: operator@test.com  → role: "OPERATOR"
```

### 2. **Test User Login**
1. Login as `user@test.com`
2. Should see: Normal booking dashboard
3. Can create bookings and scan QR codes

### 3. **Test Operator Login**
1. Login as `operator@test.com`
2. Should see: Vehicle scanner interface
3. Can scan any vehicle plate and check them in

---

## 🎨 Visual Comparison

### User Screen
```
┌─────────────────────────┐
│  My Bookings            │
├─────────────────────────┤
│  [Booking #123]         │
│  Vehicle: ABC123        │
│  Slot: A12              │
│  [Scan QR to Check-In]  │
└─────────────────────────┘
```

### Operator Screen
```
┌─────────────────────────┐
│  Operator Dashboard     │
├─────────────────────────┤
│  ┌───────────────────┐  │
│  │   📷 SCAN VEHICLE │  │
│  │                   │  │
│  │  [Big Camera Btn] │  │
│  └───────────────────┘  │
│                         │
│  OR Manual Entry:       │
│  [ABC123_________]      │
│  [Check In]  [Check Out]│
└─────────────────────────┘
```

---

## 🚀 Deployment Checklist

- [ ] ✅ Backend endpoints ready (`/api/bookings/checkin`)
- [ ] ✅ JWT includes role field
- [ ] ✅ Create `CheckInMode.kt`
- [ ] ✅ Update `CheckInRequest.kt`
- [ ] ✅ Create `OperatorViewModel.kt`
- [ ] 🔨 Create `OperatorDashboardActivity.kt`
- [ ] 🔨 Update `LoginActivity.kt` (add role check)
- [ ] 🔨 Update `ApiService.kt` (add operator endpoints)
- [ ] 🔨 Update `BookingRepository.kt`
- [ ] 🔨 Create operator layout XML
- [ ] 🧪 Test user login
- [ ] 🧪 Test operator login
- [ ] 🧪 Test vehicle scanning

---

## 💡 Key Insights

### Why This Design?

1. **Operators don't need bookingId**: They scan plates, backend finds the booking
2. **Users need bookingId**: They only check-in their own bookings
3. **Same scanner**: Reuse `QrScannerActivity` for both QR codes and license plates
4. **JWT handles security**: Backend checks role before allowing operations

### Backend Logic (already implemented)

```java
// When operator scans "ABC123":
1. Find booking where vehicleNumber = "ABC123" AND status = "CONFIRMED"
2. Set checkInTime = now
3. Set checkInOperatorId = operator.id
4. Return updated booking

// When user scans QR:
1. Find booking by bookingId (from URL parameter)
2. Verify user owns this booking
3. Set checkInTime = now
4. Return updated booking
```

---

## 🎯 Next Steps

1. **Create `OperatorDashboardActivity`** (30 min)
   - Copy structure from existing activities
   - Add big "Scan Vehicle" button
   - Add manual entry field

2. **Update Login Flow** (10 min)
   - Add role check in `LoginActivity`
   - Navigate based on role

3. **Test with Backend** (15 min)
   - Create operator account
   - Test check-in flow
   - Verify JWT authorization

4. **Polish UI** (optional)
   - Add operator branding
   - Show recent check-ins
   - Add statistics

---

## 📞 Need Help?

**Common Issues:**

1. **"401 Unauthorized"** → JWT token not included in request
2. **"403 Forbidden"** → User has wrong role (USER trying to access operator endpoint)
3. **"404 Not Found"** → No booking found for that vehicle number
4. **Scanner not working** → Check camera permissions

**Debug Tips:**

```kotlin
// Add logging to see role:
Log.d("Login", "User role: ${authResponse.role}")

// Check SharedPreferences:
val role = sharedPref.getString("user_role", "")
Log.d("Role", "Saved role: $role")
```

---

## 🎉 You're Ready!

Your backend is already set up perfectly. Now you just need to:
1. Create the operator UI
2. Wire up the navigation based on role
3. Test both flows

The guide above has all the code you need. Good luck! 🚀
