# 🎯 EXACT CODE CHANGES NEEDED

This document shows the **exact 4 changes** you need to make to complete the implementation.

---

## Change #1: Update LoginActivity.kt

**File:** `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/auth/LoginActivity.kt`

**Find this code** (around line 130-145):

```kotlin
private fun observeViewModel() {
    viewModel.loginState.observe(this) { state ->
        when (state) {
            is LoginState.Loading -> {
                showLoading(true)
            }
            is LoginState.Success -> {
                showLoading(false)
                Toast.makeText(this, "Welcome back, ${state.user.name}!", Toast.LENGTH_LONG).show()
                
                // Save user data to SharedPreferences
                val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
                sharedPref.edit()
                    .putString("user_id", state.user.id)
                    .putString("user_name", state.user.name)
                    .putString("user_email", state.user.email)
                    .putString("user_phone", state.user.phone)
                    .putBoolean("is_logged_in", true)
                    .apply()
                
                // Navigate to main activity
                val intent = Intent(this, MainContainerActivity::class.java)
                intent.putExtra("USER_NAME", state.user.name)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
```

**Replace with this:**

```kotlin
private fun observeViewModel() {
    viewModel.loginState.observe(this) { state ->
        when (state) {
            is LoginState.Loading -> {
                showLoading(true)
            }
            is LoginState.Success -> {
                showLoading(false)
                Toast.makeText(this, "Welcome back, ${state.user.name}!", Toast.LENGTH_LONG).show()
                
                // Save user data to SharedPreferences
                val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
                sharedPref.edit()
                    .putString("user_id", state.user.id)
                    .putString("user_name", state.user.name)
                    .putString("user_email", state.user.email)
                    .putString("user_phone", state.user.phone)
                    .putString("user_role", state.user.role)  // ✅ ADD THIS LINE
                    .putBoolean("is_logged_in", true)
                    .apply()
                
                // ✅ ADD ROLE-BASED NAVIGATION
                when (state.user.role.uppercase()) {
                    "OPERATOR" -> {
                        // Navigate to operator dashboard
                        val intent = Intent(this, com.gridee.parking.ui.operator.OperatorDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    "USER" -> {
                        // Navigate to main activity (normal user)
                        val intent = Intent(this, MainContainerActivity::class.java)
                        intent.putExtra("USER_NAME", state.user.name)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    else -> {
                        Toast.makeText(this, "Unknown user role: ${state.user.role}", Toast.LENGTH_LONG).show()
                    }
                }
                finish()
            }
```

**Summary:** Added role check to navigate to different screens based on user role.

---

## Change #2: Update ApiService.kt

**File:** `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/api/ApiService.kt`

**Find this section** (around line 50-60, after user endpoints):

```kotlin
interface ApiService {
    
    // ========== Authentication Endpoints ==========
    
    @POST("api/auth/login")
    suspend fun authLogin(@Body request: AuthRequest): Response<AuthResponse>
    
    // ========== User Management Endpoints ==========
    // ... existing endpoints ...
    
    // ========== Booking Endpoints ==========  ← FIND THIS SECTION
```

**Add these operator endpoints** (add after user endpoints, before or after booking endpoints):

```kotlin
    // ========== Operator Check-In/Out Endpoints ==========
    
    /**
     * Operator check-in (no userId/bookingId required)
     * POST /api/bookings/checkin
     * Requires OPERATOR role in JWT token
     */
    @POST("api/bookings/checkin")
    suspend fun operatorCheckIn(@Body request: CheckInRequest): Response<Booking>
    
    /**
     * Operator check-out (no userId/bookingId required)
     * POST /api/bookings/checkout
     * Requires OPERATOR role in JWT token
     */
    @POST("api/bookings/checkout")
    suspend fun operatorCheckOut(@Body request: CheckInRequest): Response<Booking>
```

**Summary:** Added two new API endpoints for operator check-in and check-out operations.

---

## Change #3: Update BookingRepository.kt

**File:** Create or update `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/repository/BookingRepository.kt`

**If file exists, add these methods. If not, create the file:**

```kotlin
package com.gridee.parking.data.repository

import com.gridee.parking.data.api.RetrofitClient
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.CheckInRequest
import retrofit2.Response

class BookingRepository {
    
    private val apiService = RetrofitClient.apiService

    /**
     * Operator check-in (no userId required)
     * Finds booking by vehicle number automatically
     * POST /api/bookings/checkin
     */
    suspend fun operatorCheckIn(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckIn(request)
    }

    /**
     * Operator check-out (no userId required)
     * Finds active booking by vehicle number automatically
     * POST /api/bookings/checkout
     */
    suspend fun operatorCheckOut(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckOut(request)
    }

    /**
     * User check-in with bookingId (existing functionality)
     * POST /api/bookings/{userId}/checkin/{bookingId}
     */
    suspend fun userCheckIn(
        userId: String,
        bookingId: String,
        request: CheckInRequest
    ): Response<Booking> {
        return apiService.userCheckIn(userId, bookingId, request)
    }

    /**
     * User check-out with bookingId (existing functionality)
     * POST /api/bookings/{userId}/checkout/{bookingId}
     */
    suspend fun userCheckOut(
        userId: String,
        bookingId: String,
        request: CheckInRequest
    ): Response<Booking> {
        return apiService.userCheckOut(userId, bookingId, request)
    }
}
```

**Summary:** Created repository methods to call the operator API endpoints.

---

## Change #4: Update AndroidManifest.xml

**File:** `/Gridee_Android/android-app/app/src/main/AndroidManifest.xml`

**Find the activities section** (where other activities like `LoginActivity` are declared):

```xml
<application>
    <!-- Existing activities -->
    <activity
        android:name=".ui.auth.LoginActivity"
        android:exported="false" />
    
    <activity
        android:name=".ui.main.MainContainerActivity"
        android:exported="false" />
    
    <!-- ✅ ADD THIS NEW ACTIVITY -->
    <activity
        android:name=".ui.operator.OperatorDashboardActivity"
        android:screenOrientation="portrait"
        android:exported="false"
        android:label="Operator Dashboard" />
    
    <!-- Rest of your activities -->
</application>
```

**Summary:** Registered the new OperatorDashboardActivity in the manifest.

---

## 🎯 Verification Steps

After making these 4 changes:

### Step 1: Build the project
```bash
./gradlew clean build
```

### Step 2: Check for compilation errors
Look for:
- Missing imports (add them if needed)
- Type mismatches (ensure `state.user` has a `role` field)

### Step 3: Test User Login
1. Run app
2. Login with user credentials
3. Should navigate to `MainContainerActivity`
4. Verify normal booking flow works

### Step 4: Test Operator Login
1. Run app
2. Login with operator credentials
3. Should navigate to `OperatorDashboardActivity`
4. Verify scanner interface appears

---

## 🔍 Common Issues & Fixes

### Issue 1: "Unresolved reference: OperatorDashboardActivity"

**In LoginActivity.kt, add import:**
```kotlin
import com.gridee.parking.ui.operator.OperatorDashboardActivity
```

### Issue 2: "Unresolved reference: CheckInRequest"

**In ApiService.kt, add import:**
```kotlin
import com.gridee.parking.data.model.CheckInRequest
```

### Issue 3: "state.user doesn't have role property"

**Check your LoginViewModel.kt** - make sure it's using `AuthResponse` which has the role field:
```kotlin
sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: AuthResponse) : LoginState()  // ← Must be AuthResponse, not User
    data class Error(val message: String) : LoginState()
}
```

### Issue 4: API calls return 403 Forbidden

**Check JWT token includes role:**
```kotlin
// In LoginActivity after login:
val token = sharedPref.getString("auth_token", "")
Log.d("AUTH", "Token: $token")

// Decode JWT (use jwt.io) to verify it contains:
// { "role": "OPERATOR", ... }
```

---

## 📝 Import Statements Needed

Add these imports where needed:

**LoginActivity.kt:**
```kotlin
import com.gridee.parking.ui.operator.OperatorDashboardActivity
```

**ApiService.kt:**
```kotlin
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.CheckInRequest
```

**BookingRepository.kt:**
```kotlin
import com.gridee.parking.data.api.RetrofitClient
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.CheckInRequest
import retrofit2.Response
```

---

## ✅ Final Checklist

Before testing:

- [ ] Updated `LoginActivity.kt` (added role-based navigation)
- [ ] Updated `ApiService.kt` (added 2 operator endpoints)
- [ ] Updated/Created `BookingRepository.kt` (added 2 methods)
- [ ] Updated `AndroidManifest.xml` (registered OperatorDashboardActivity)
- [ ] Added all necessary imports
- [ ] Project builds successfully
- [ ] No compilation errors

Ready to test? Follow the verification steps above! 🚀

---

## 🎊 Success!

Once all 4 changes are complete and tested:

✅ Users login → See booking dashboard
✅ Operators login → See scanner interface
✅ Operators can check-in vehicles by scanning plates
✅ Backend validates JWT roles correctly

**You're done!** 🎉
