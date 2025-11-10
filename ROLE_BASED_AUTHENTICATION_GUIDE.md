# Role-Based Authentication Guide: User vs Operator

## 📋 Overview
This guide explains how to implement a role-based sign-in system where users can authenticate as either:
- **USER**: Normal customers who book parking spots
- **OPERATOR**: Parking lot staff who scan vehicles and manage check-ins/check-outs

---

## 🎯 System Architecture

### Backend (Java Spring Boot)

#### 1. **User Roles** (Already in your backend)
```java
// Your backend already supports roles in UserResponseDto
@SerializedName("role")
val role: String  // Values: "USER" or "OPERATOR"
```

#### 2. **CheckInMode Enum** (Your provided code)
```java
public enum CheckInMode {
    QR_CODE("QR_CODE"),           // Scan QR from booking
    VEHICLE_NUMBER("VEHICLE_NUMBER"), // Scan vehicle plate
    PIN("PIN");                    // Enter PIN code
}
```

#### 3. **API Endpoints** (Based on your code)

**Operator Check-In (No userId required)**
```
POST /api/bookings/checkin
Authorization: Bearer <operator_jwt>
Role Required: OPERATOR

Request Body:
{
  "mode": "VEHICLE_NUMBER",
  "vehicleNumber": "ABC123",  // Scanned vehicle number
  "qrCode": null,
  "pin": null
}
```

**User Check-In (With bookingId)**
```
POST /api/bookings/{userId}/checkin/{bookingId}
Authorization: Bearer <user_jwt>
Role Required: USER

Request Body:
{
  "mode": "QR_CODE",
  "qrCode": "booking_qr_xyz",
  "vehicleNumber": null,
  "pin": null
}
```

---

## 🔧 Android Implementation

### Step 1: Create Kotlin Enum for CheckInMode

```kotlin
// File: CheckInMode.kt
package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName

enum class CheckInMode {
    @SerializedName("QR_CODE")
    QR_CODE,
    
    @SerializedName("VEHICLE_NUMBER")
    VEHICLE_NUMBER,
    
    @SerializedName("PIN")
    PIN
}
```

### Step 2: Create Check-In Request Model

```kotlin
// File: CheckInRequest.kt
package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("mode")
    val mode: CheckInMode,
    
    @SerializedName("qrCode")
    val qrCode: String? = null,
    
    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,
    
    @SerializedName("pin")
    val pin: String? = null
)
```

### Step 3: Update AuthResponse to Include Role

**Your current `AuthResponse.kt` already has role support! ✅**

```kotlin
// Already in your code:
data class UserResponseDto(
    // ... other fields
    @SerializedName("role")
    val role: String  // "USER" or "OPERATOR"
)
```

### Step 4: Create Role-Based Navigation Logic

```kotlin
// File: LoginViewModel.kt (Update your existing one)
package com.gridee.parking.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.AuthResponse
import com.gridee.parking.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun loginUser(context: android.content.Context, email: String, password: String) {
        if (!validateInput(email, password)) return

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val response = userRepository.authLogin(email, password)
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    
                    // Save authentication data
                    saveAuthData(context, authResponse)
                    
                    // Check user role and navigate accordingly
                    when (authResponse.role.uppercase()) {
                        "OPERATOR" -> _loginState.value = LoginState.OperatorSuccess(authResponse)
                        "USER" -> _loginState.value = LoginState.UserSuccess(authResponse)
                        else -> _loginState.value = LoginState.Error("Unknown user role")
                    }
                } else {
                    _loginState.value = LoginState.Error("Login failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error: ${e.message}")
            }
        }
    }

    private fun saveAuthData(context: android.content.Context, authResponse: AuthResponse) {
        val sharedPref = context.getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        sharedPref.edit()
            .putString("auth_token", authResponse.token)
            .putString("user_id", authResponse.id)
            .putString("user_name", authResponse.name)
            .putString("user_email", authResponse.email)
            .putString("user_phone", authResponse.phone)
            .putString("user_role", authResponse.role)  // ✅ Save role
            .putBoolean("is_logged_in", true)
            .apply()
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Your existing validation
        return true
    }
}

// Update LoginState sealed class
sealed class LoginState {
    object Loading : LoginState()
    data class UserSuccess(val authResponse: AuthResponse) : LoginState()
    data class OperatorSuccess(val authResponse: AuthResponse) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

### Step 5: Update LoginActivity for Role-Based Navigation

```kotlin
// File: LoginActivity.kt (Update observeViewModel method)
private fun observeViewModel() {
    viewModel.loginState.observe(this) { state ->
        when (state) {
            is LoginState.Loading -> {
                showLoading(true)
            }
            is LoginState.UserSuccess -> {
                showLoading(false)
                Toast.makeText(this, "Welcome ${state.authResponse.name}!", Toast.LENGTH_SHORT).show()
                navigateToUserDashboard()
            }
            is LoginState.OperatorSuccess -> {
                showLoading(false)
                Toast.makeText(this, "Operator Mode: ${state.authResponse.name}", Toast.LENGTH_SHORT).show()
                navigateToOperatorScanner()
            }
            is LoginState.Error -> {
                showLoading(false)
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun navigateToUserDashboard() {
    val intent = Intent(this, MainContainerActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}

private fun navigateToOperatorScanner() {
    val intent = Intent(this, OperatorDashboardActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    startActivity(intent)
    finish()
}
```

### Step 6: Create Operator Dashboard with Vehicle Scanner

```kotlin
// File: OperatorDashboardActivity.kt
package com.gridee.parking.ui.operator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gridee.parking.databinding.ActivityOperatorDashboardBinding
import com.gridee.parking.ui.qr.QrScannerActivity

class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding
    private val viewModel: OperatorViewModel by viewModels()

    private val vehicleNumberScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QrScannerActivity.RESULT_QR_SCANNED) {
            val vehicleNumber = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE)
            vehicleNumber?.let { 
                viewModel.checkInByVehicleNumber(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Get operator info from SharedPreferences
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val operatorName = sharedPref.getString("user_name", "Operator")
        
        binding.tvOperatorName.text = "Welcome, $operatorName"

        // Button to scan vehicle number
        binding.btnScanVehicle.setOnClickListener {
            if (checkCameraPermission()) {
                openVehicleScanner()
            } else {
                requestCameraPermission()
            }
        }

        // Button for manual entry
        binding.btnManualEntry.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            if (vehicleNumber.isNotBlank()) {
                viewModel.checkInByVehicleNumber(vehicleNumber)
            } else {
                Toast.makeText(this, "Enter vehicle number", Toast.LENGTH_SHORT).show()
            }
        }

        // Check-out button
        binding.btnCheckOut.setOnClickListener {
            openVehicleScannerForCheckout()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun openVehicleScanner() {
        val intent = Intent(this, QrScannerActivity::class.java).apply {
            putExtra(QrScannerActivity.EXTRA_SCAN_TYPE, "VEHICLE_CHECK_IN")
        }
        vehicleNumberScannerLauncher.launch(intent)
    }

    private fun openVehicleScannerForCheckout() {
        val intent = Intent(this, QrScannerActivity::class.java).apply {
            putExtra(QrScannerActivity.EXTRA_SCAN_TYPE, "VEHICLE_CHECK_OUT")
        }
        vehicleNumberScannerLauncher.launch(intent)
    }

    private fun observeViewModel() {
        viewModel.checkInState.observe(this) { state ->
            when (state) {
                is CheckInState.Loading -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is CheckInState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(
                        this,
                        "Check-in successful for ${state.booking.vehicleNumber}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.etVehicleNumber.text?.clear()
                }
                is CheckInState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            100
        )
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        // Navigate back to login
        val intent = Intent(this, com.gridee.parking.ui.auth.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
```

### Step 7: Create Operator ViewModel

```kotlin
// File: OperatorViewModel.kt
package com.gridee.parking.ui.operator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.CheckInMode
import com.gridee.parking.data.model.CheckInRequest
import com.gridee.parking.data.repository.BookingRepository
import kotlinx.coroutines.launch

class OperatorViewModel(
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _checkInState = MutableLiveData<CheckInState>()
    val checkInState: LiveData<CheckInState> = _checkInState

    /**
     * Check-in using vehicle number (OPERATOR mode)
     */
    fun checkInByVehicleNumber(vehicleNumber: String) {
        _checkInState.value = CheckInState.Loading

        viewModelScope.launch {
            try {
                val request = CheckInRequest(
                    mode = CheckInMode.VEHICLE_NUMBER,
                    vehicleNumber = vehicleNumber
                )

                val response = bookingRepository.operatorCheckIn(request)

                if (response.isSuccessful && response.body() != null) {
                    _checkInState.value = CheckInState.Success(response.body()!!)
                } else {
                    _checkInState.value = CheckInState.Error("Check-in failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _checkInState.value = CheckInState.Error("Error: ${e.message}")
            }
        }
    }

    /**
     * Check-out using vehicle number (OPERATOR mode)
     */
    fun checkOutByVehicleNumber(vehicleNumber: String) {
        _checkInState.value = CheckInState.Loading

        viewModelScope.launch {
            try {
                val request = CheckInRequest(
                    mode = CheckInMode.VEHICLE_NUMBER,
                    vehicleNumber = vehicleNumber
                )

                val response = bookingRepository.operatorCheckOut(request)

                if (response.isSuccessful && response.body() != null) {
                    _checkInState.value = CheckInState.Success(response.body()!!)
                } else {
                    _checkInState.value = CheckInState.Error("Check-out failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _checkInState.value = CheckInState.Error("Error: ${e.message}")
            }
        }
    }
}

sealed class CheckInState {
    object Loading : CheckInState()
    data class Success(val booking: com.gridee.parking.data.model.Booking) : CheckInState()
    data class Error(val message: String) : CheckInState()
}
```

### Step 8: Update BookingRepository with Operator APIs

```kotlin
// File: BookingRepository.kt (Add these methods)
package com.gridee.parking.data.repository

import com.gridee.parking.data.api.RetrofitClient
import com.gridee.parking.data.model.CheckInRequest
import retrofit2.Response

class BookingRepository {
    
    private val apiService = RetrofitClient.apiService

    /**
     * Operator check-in (no userId required)
     * POST /api/bookings/checkin
     */
    suspend fun operatorCheckIn(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckIn(request)
    }

    /**
     * Operator check-out (no userId required)
     * POST /api/bookings/checkout
     */
    suspend fun operatorCheckOut(request: CheckInRequest): Response<Booking> {
        return apiService.operatorCheckOut(request)
    }

    /**
     * User check-in with bookingId
     * POST /api/bookings/{userId}/checkin/{bookingId}
     */
    suspend fun userCheckIn(
        userId: String,
        bookingId: String,
        request: CheckInRequest
    ): Response<Booking> {
        return apiService.userCheckIn(userId, bookingId, request)
    }
}
```

### Step 9: Update ApiService with New Endpoints

```kotlin
// File: ApiService.kt (Add these endpoints)
package com.gridee.parking.data.api

import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.CheckInRequest
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ========== Operator Check-In/Out Endpoints ==========
    
    @POST("api/bookings/checkin")
    suspend fun operatorCheckIn(@Body request: CheckInRequest): Response<Booking>
    
    @POST("api/bookings/checkout")
    suspend fun operatorCheckOut(@Body request: CheckInRequest): Response<Booking>
    
    // ========== User Check-In/Out Endpoints ==========
    
    @POST("api/bookings/{userId}/checkin/{bookingId}")
    suspend fun userCheckIn(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: CheckInRequest
    ): Response<Booking>
    
    @POST("api/bookings/{userId}/checkout/{bookingId}")
    suspend fun userCheckOut(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: CheckInRequest
    ): Response<Booking>
}
```

---

## 📱 UI Layout Files

### Operator Dashboard Layout

```xml
<!-- File: activity_operator_dashboard.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp"
    android:background="@color/background_color">

    <TextView
        android:id="@+id/tvOperatorName"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Welcome, Operator"
        android:textSize="24sp"
        android:textStyle="bold"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        android:layout_marginTop="32dp"/>

    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardScanner"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:cardCornerRadius="16dp"
        app:cardElevation="4dp"
        android:layout_marginTop="32dp"
        app:layout_constraintTop_toBottomOf="@id/tvOperatorName">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="24dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Vehicle Check-In"
                android:textSize="20sp"
                android:textStyle="bold"/>

            <Button
                android:id="@+id/btnScanVehicle"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Scan Vehicle Number"
                android:layout_marginTop="16dp"
                android:drawableStart="@drawable/ic_camera"
                android:drawablePadding="8dp"/>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="OR"
                android:layout_gravity="center"
                android:layout_marginVertical="12dp"/>

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Enter Vehicle Number">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/etVehicleNumber"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="textCapCharacters"/>
            </com.google.android.material.textfield.TextInputLayout>

            <Button
                android:id="@+id/btnManualEntry"
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:text="Manual Check-In"
                android:layout_marginTop="8dp"
                style="@style/Widget.MaterialComponents.Button.OutlinedButton"/>
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>

    <Button
        android:id="@+id/btnCheckOut"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:text="Vehicle Check-Out"
        android:layout_marginTop="16dp"
        android:backgroundTint="@color/orange"
        app:layout_constraintTop_toBottomOf="@id/cardScanner"/>

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

    <Button
        android:id="@+id/btnLogout"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Logout"
        style="@style/Widget.MaterialComponents.Button.TextButton"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="16dp"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 🔐 Testing the Flow

### 1. **Create Test Accounts in Backend**
```sql
-- User Account
INSERT INTO users (email, password, role) 
VALUES ('user@test.com', 'hashed_password', 'USER');

-- Operator Account
INSERT INTO users (email, password, role) 
VALUES ('operator@test.com', 'hashed_password', 'OPERATOR');
```

### 2. **Test User Flow**
1. Login with `user@test.com`
2. Should navigate to MainContainerActivity
3. Can create bookings and use QR code check-in

### 3. **Test Operator Flow**
1. Login with `operator@test.com`
2. Should navigate to OperatorDashboardActivity
3. Can scan vehicle numbers
4. Can check-in/check-out without bookingId

---

## 🎨 Key Differences

| Feature | USER | OPERATOR |
|---------|------|----------|
| **Login Destination** | MainContainerActivity | OperatorDashboardActivity |
| **Check-In Method** | QR Code with bookingId | Vehicle Number scan |
| **Endpoint** | `/api/bookings/{userId}/checkin/{bookingId}` | `/api/bookings/checkin` |
| **Required Data** | bookingId + QR code | vehicleNumber only |
| **JWT Role** | ROLE_USER | ROLE_OPERATOR |

---

## ✅ Implementation Checklist

- [ ] Create `CheckInMode.kt` enum
- [ ] Create `CheckInRequest.kt` model
- [ ] Update `LoginViewModel` for role detection
- [ ] Update `LoginActivity` for role-based navigation
- [ ] Create `OperatorDashboardActivity`
- [ ] Create `OperatorViewModel`
- [ ] Create `activity_operator_dashboard.xml`
- [ ] Update `ApiService` with operator endpoints
- [ ] Update `BookingRepository` with operator methods
- [ ] Test USER login flow
- [ ] Test OPERATOR login flow
- [ ] Test vehicle number scanning
- [ ] Test operator check-in/check-out

---

## 🚀 Next Steps

1. **Implement OCR for Vehicle Plates**: Use ML Kit or Tesseract to automatically read license plates
2. **Add Offline Support**: Cache operator actions when offline
3. **Real-time Notifications**: Alert operators of new bookings
4. **Analytics Dashboard**: Show parking occupancy stats
5. **Multi-language Support**: For international operators

