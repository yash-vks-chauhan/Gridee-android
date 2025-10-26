# QR Check-In/Check-Out Implementation Plan

## 🎯 Overview
This plan outlines the complete implementation of QR code-based check-in and check-out functionality for the Gridee Android app, fully aligned with the existing backend API.

---

## 📋 Backend Flow Analysis

### Backend Check-In Flow:
1. User scans QR code (bookingId)
2. Backend validates:
   - Booking exists
   - Booking status is "pending"
   - User owns the booking
   - QR code matches bookingId
   - No other active booking exists for user
3. Backend calculates late check-in penalty (10-min grace period)
4. Backend updates:
   - Status: "pending" → "active"
   - actualCheckInTime: current timestamp
   - qrCodeScanned: true
5. Returns updated booking

### Backend Check-Out Flow:
1. User scans QR code (bookingId)
2. Backend validates:
   - Booking exists
   - Booking status is "active"
   - User owns the booking
   - QR code matches bookingId
3. Backend calculates:
   - Late check-in penalty (from actualCheckInTime)
   - Late check-out penalty (from scheduled checkOutTime)
   - Total penalty
4. Backend deducts penalties from wallet
5. Backend updates:
   - Status: "active" → "completed"
   - checkOutTime: current timestamp
   - Releases parking spot (increments availability)
   - Applies refund/breakup
6. Returns updated booking

### Penalty Calculation Rules:
- **Grace Period:** 10 minutes (no penalty)
- **Late Check-In:** After 10 minutes from scheduled check-in time
  - Rate: `spot.checkInPenaltyRate` per minute
- **Late Check-Out:** After 10 minutes from scheduled check-out time
  - Rate: `spot.checkOutPenaltyRate` per minute
- **Formula:** `(minutesLate - 10) * penaltyRate`

---

## 🏗️ Implementation Plan

### **Phase 1: Update Data Models** (1 hour)

#### 1.1 Update Booking.kt
Add missing fields to match backend model:

```kotlin
// File: app/src/main/java/com/gridee/parking/data/model/Booking.kt

data class Booking(
    // Existing fields...
    
    // NEW FIELDS - Add these
    @SerializedName("qrCodeScanned")
    val qrCodeScanned: Boolean = false,
    
    @SerializedName("actualCheckInTime")
    val actualCheckInTime: Date? = null,
    
    @SerializedName("autoCompleted")
    val autoCompleted: Boolean? = false
)
```

#### 1.2 Create QR Validation Models
```kotlin
// File: app/src/main/java/com/gridee/parking/data/model/QrModels.kt

data class QrValidationResult(
    @SerializedName("valid")
    val valid: Boolean,
    
    @SerializedName("penalty")
    val penalty: Double,
    
    @SerializedName("message")
    val message: String
)

data class QrCodeRequest(
    @SerializedName("qrCode")
    val qrCode: String
)
```

---

### **Phase 2: Update API Layer** (2 hours)

#### 2.1 Update ApiService.kt
Add new endpoints:

```kotlin
// File: app/src/main/java/com/gridee/parking/data/api/ApiService.kt

interface ApiService {
    // Existing endpoints...
    
    // ========== NEW QR CHECK-IN/OUT ENDPOINTS ==========
    
    // Validate QR code before check-in
    @POST("api/users/{userId}/bookings/{bookingId}/validate-qr-checkin")
    suspend fun validateQrCodeForCheckIn(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: QrCodeRequest
    ): Response<QrValidationResult>
    
    // Actual check-in
    @POST("api/users/{userId}/bookings/{bookingId}/checkin")
    suspend fun checkInBooking(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: QrCodeRequest
    ): Response<Booking>
    
    // Validate QR code before check-out
    @POST("api/users/{userId}/bookings/{bookingId}/validate-qr-checkout")
    suspend fun validateQrCodeForCheckOut(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: QrCodeRequest
    ): Response<QrValidationResult>
    
    // Actual check-out
    @POST("api/users/{userId}/bookings/{bookingId}/checkout")
    suspend fun checkOutBooking(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String,
        @Body request: QrCodeRequest
    ): Response<Booking>
    
    // Get booking by ID (for refreshing data)
    @GET("api/users/{userId}/bookings/{bookingId}")
    suspend fun getBookingById(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String
    ): Response<Booking>
    
    // Get penalty info (real-time)
    @GET("api/users/{userId}/bookings/{bookingId}/penalty")
    suspend fun getPenaltyInfo(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String
    ): Response<Double>
}
```

#### 2.2 Update BookingRepository.kt
Add repository methods:

```kotlin
// File: app/src/main/java/com/gridee/parking/data/repository/BookingRepository.kt

class BookingRepository(private val context: Context) {
    // Existing methods...
    
    // ========== NEW QR METHODS ==========
    
    /**
     * Validate QR code for check-in
     * Shows penalty warning if applicable
     */
    suspend fun validateCheckInQr(
        bookingId: String,
        qrCode: String
    ): Result<QrValidationResult> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val request = QrCodeRequest(qrCode)
            val response = apiService.validateQrCodeForCheckIn(userId, bookingId, request)
            
            if (response.isSuccessful) {
                val result = response.body()
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform actual check-in
     */
    suspend fun checkIn(
        bookingId: String,
        qrCode: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val request = QrCodeRequest(qrCode)
            val response = apiService.checkInBooking(userId, bookingId, request)
            
            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Check-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate QR code for check-out
     * Shows final charges including penalties
     */
    suspend fun validateCheckOutQr(
        bookingId: String,
        qrCode: String
    ): Result<QrValidationResult> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val request = QrCodeRequest(qrCode)
            val response = apiService.validateQrCodeForCheckOut(userId, bookingId, request)
            
            if (response.isSuccessful) {
                val result = response.body()
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Perform actual check-out
     */
    suspend fun checkOut(
        bookingId: String,
        qrCode: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val request = QrCodeRequest(qrCode)
            val response = apiService.checkOutBooking(userId, bookingId, request)
            
            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else if (response.code() == 402) {
                // Payment required - insufficient funds
                Result.failure(Exception("Insufficient wallet balance to pay penalties"))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Check-out failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get real-time penalty for active booking
     */
    suspend fun getPenaltyInfo(bookingId: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val response = apiService.getPenaltyInfo(userId, bookingId)
            
            if (response.isSuccessful) {
                val penalty = response.body()
                if (penalty != null) {
                    Result.success(penalty)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to get penalty info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh booking data
     */
    suspend fun refreshBooking(bookingId: String): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val response = apiService.getBookingById(userId, bookingId)
            
            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to refresh booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

### **Phase 3: QR Code Scanner Integration** (3 hours)

#### 3.1 Add Dependencies
```gradle
// File: app/build.gradle

dependencies {
    // Existing dependencies...
    
    // ZXing for QR scanning
    implementation 'com.google.zxing:core:3.5.2'
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}
```

#### 3.2 Add Camera Permission
```xml
<!-- File: app/src/main/AndroidManifest.xml -->

<manifest>
    <!-- Existing permissions... -->
    
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
</manifest>
```

#### 3.3 Create QR Scanner Activity
```kotlin
// File: app/src/main/java/com/gridee/parking/ui/qr/QrScannerActivity.kt

package com.gridee.parking.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.gridee.parking.R
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScannerActivity : AppCompatActivity() {
    
    private lateinit var barcodeView: DecoratedBarcodeView
    private var bookingId: String? = null
    private var scanType: ScanType = ScanType.CHECK_IN
    
    enum class ScanType {
        CHECK_IN, CHECK_OUT
    }
    
    companion object {
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_SCAN_TYPE = "scan_type"
        const val EXTRA_QR_CODE = "qr_code"
        const val RESULT_QR_SCANNED = 100
        private const val CAMERA_PERMISSION_REQUEST = 101
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)
        
        bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)
        scanType = ScanType.valueOf(intent.getStringExtra(EXTRA_SCAN_TYPE) ?: "CHECK_IN")
        
        barcodeView = findViewById(R.id.barcode_scanner)
        
        // Check camera permission
        if (checkCameraPermission()) {
            startScanning()
        } else {
            requestCameraPermission()
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
            CAMERA_PERMISSION_REQUEST
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun startScanning() {
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    handleQrCodeScanned(it.text)
                }
            }
            
            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }
    
    private fun handleQrCodeScanned(qrCode: String) {
        barcodeView.pause()
        
        // Return the scanned QR code
        val resultIntent = Intent().apply {
            putExtra(EXTRA_QR_CODE, qrCode)
            putExtra(EXTRA_BOOKING_ID, bookingId)
        }
        setResult(RESULT_QR_SCANNED, resultIntent)
        finish()
    }
    
    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }
    
    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
```

#### 3.4 Create QR Scanner Layout
```xml
<!-- File: app/src/main/res/layout/activity_qr_scanner.xml -->

<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black">

    <com.journeyapps.barcodescanner.DecoratedBarcodeView
        android:id="@+id/barcode_scanner"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:zxing_scanner_layout="@layout/custom_barcode_scanner" />

    <TextView
        android:id="@+id/scan_instructions"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Scan QR code at parking spot"
        android:textColor="@color/white"
        android:textSize="18sp"
        android:padding="16dp"
        android:background="#80000000"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginTop="32dp"/>

    <Button
        android:id="@+id/btn_cancel"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Cancel"
        android:backgroundTint="@color/red"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:layout_marginBottom="32dp"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### **Phase 4: Update Booking Detail Components** (4 hours)

**Note:** Your existing architecture uses:
- `BookingsViewModel` - handles list of bookings
- `BookingsActivity` - displays bookings in tabs (Active, Pending, Completed)
- `BookingAdapter` - RecyclerView adapter for booking list

**We need to add:**
- `BookingDetailViewModel` - NEW - handles individual booking operations
- `BookingDetailBottomSheet` or `BookingDetailActivity` - NEW - shows booking details with QR actions

#### 4.1 Extend BookingsViewModel.kt
First, add QR methods to existing BookingsViewModel:

```kotlin
// File: app/src/main/java/com/gridee/parking/ui/bookings/BookingsViewModel.kt
// ADD these new methods to your existing BookingsViewModel class

class BookingsViewModel(application: Application) : AndroidViewModel(application) {
    // ... existing code ...
    
    // ========== NEW QR CODE METHODS ==========
    
    private val _selectedBooking = MutableLiveData<Booking?>()
    val selectedBooking: LiveData<Booking?> = _selectedBooking
    
    private val _penalty = MutableLiveData<Double>()
    val penalty: LiveData<Double> = _penalty
    
    private val _qrValidation = MutableLiveData<com.gridee.parking.data.model.QrValidationResult?>()
    val qrValidation: LiveData<com.gridee.parking.data.model.QrValidationResult?> = _qrValidation
    
    private val _checkInSuccess = MutableLiveData<Boolean?>()
    val checkInSuccess: LiveData<Boolean?> = _checkInSuccess
    
    private val _checkOutSuccess = MutableLiveData<Boolean?>()
    val checkOutSuccess: LiveData<Boolean?> = _checkOutSuccess
    
    fun selectBooking(booking: Booking) {
        _selectedBooking.value = booking
    }
    
    fun refreshBooking(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.refreshBooking(bookingId).fold(
                    onSuccess = { booking ->
                        _selectedBooking.value = booking
                        _errorMessage.value = null
                        // Also refresh the full list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh booking: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadPenaltyInfo(bookingId: String) {
        viewModelScope.launch {
            try {
                bookingRepository.getPenaltyInfo(bookingId).fold(
                    onSuccess = { penalty ->
                        _penalty.value = penalty
                    },
                    onFailure = {
                        _penalty.value = 0.0
                    }
                )
            } catch (e: Exception) {
                _penalty.value = 0.0
            }
        }
    }
    
    fun validateCheckInQr(bookingId: String, qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.validateCheckInQr(bookingId, qrCode).fold(
                    onSuccess = { validation ->
                        _qrValidation.value = validation
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _qrValidation.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "QR validation failed: ${e.message}"
                _qrValidation.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun checkIn(bookingId: String, qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.checkIn(bookingId, qrCode).fold(
                    onSuccess = { booking ->
                        _selectedBooking.value = booking
                        _checkInSuccess.value = true
                        _errorMessage.value = null
                        // Refresh full list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _checkInSuccess.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Check-in failed: ${e.message}"
                _checkInSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun validateCheckOutQr(bookingId: String, qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.validateCheckOutQr(bookingId, qrCode).fold(
                    onSuccess = { validation ->
                        _qrValidation.value = validation
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _qrValidation.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "QR validation failed: ${e.message}"
                _qrValidation.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun checkOut(bookingId: String, qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.checkOut(bookingId, qrCode).fold(
                    onSuccess = { booking ->
                        _selectedBooking.value = booking
                        _checkOutSuccess.value = true
                        _errorMessage.value = null
                        // Refresh full list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _checkOutSuccess.value = false
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Check-out failed: ${e.message}"
                _checkOutSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearQrValidation() {
        _qrValidation.value = null
    }
    
    fun clearCheckInSuccess() {
        _checkInSuccess.value = null
    }
    
    fun clearCheckOutSuccess() {
        _checkOutSuccess.value = null
    }
}
```

#### 4.2 Create BookingDetailBottomSheet.kt
Create a BottomSheet to show booking details with QR actions:

```kotlin
// File: app/src/main/java/com/gridee/parking/ui/bookings/BookingDetailBottomSheet.kt

package com.gridee.parking.ui.bookings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.data.model.Booking
import com.gridee.parking.databinding.BottomSheetBookingDetailBinding
import com.gridee.parking.ui.qr.QrScannerActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetBookingDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: BookingsViewModel
    private var booking: Booking? = null
    private var penaltyUpdateJob: Job? = null
    
    companion object {
        private const val ARG_BOOKING_ID = "booking_id"
        private const val QR_SCAN_REQUEST = 200
        
        fun newInstance(bookingId: String): BookingDetailBottomSheet {
            return BookingDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_BOOKING_ID, bookingId)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[BookingsViewModel::class.java]
        
        val bookingId = arguments?.getString(ARG_BOOKING_ID) ?: return
        
        setupObservers()
        setupClickListeners(bookingId)
        
        // Load booking details
        viewModel.refreshBooking(bookingId)
    }
    
    private fun setupObservers() {
        viewModel.selectedBooking.observe(viewLifecycleOwner) { booking ->
            this.booking = booking
            booking?.let { updateUI(it) }
            
            // Start penalty tracking for active bookings
            if (booking?.status == "active") {
                startPenaltyTracking(booking.id ?: "")
            } else {
                stopPenaltyTracking()
            }
        }
        
        viewModel.penalty.observe(viewLifecycleOwner) { penalty ->
            if (penalty > 0) {
                binding.tvPenaltyWarning.visibility = View.VISIBLE
                binding.tvPenaltyWarning.text = "⚠️ Overtime Penalty: ₹$penalty"
            } else {
                binding.tvPenaltyWarning.visibility = View.GONE
            }
        }
        
        viewModel.qrValidation.observe(viewLifecycleOwner) { validation ->
            validation?.let {
                showValidationDialog(it)
                viewModel.clearQrValidation()
            }
        }
        
        viewModel.checkInSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(context, "✅ Check-in successful!", Toast.LENGTH_SHORT).show()
                    booking?.id?.let { id -> viewModel.refreshBooking(id) }
                }
                viewModel.clearCheckInSuccess()
            }
        }
        
        viewModel.checkOutSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(context, "✅ Check-out successful!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                viewModel.clearCheckOutSuccess()
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }
    
    private fun setupClickListeners(bookingId: String) {
        binding.btnCheckIn.setOnClickListener {
            launchQrScanner(QrScannerActivity.ScanType.CHECK_IN, bookingId)
        }
        
        binding.btnCheckOut.setOnClickListener {
            launchQrScanner(QrScannerActivity.ScanType.CHECK_OUT, bookingId)
        }
        
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshBooking(bookingId)
        }
        
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }
    
    private fun launchQrScanner(scanType: QrScannerActivity.ScanType, bookingId: String) {
        val intent = Intent(requireContext(), QrScannerActivity::class.java).apply {
            putExtra(QrScannerActivity.EXTRA_BOOKING_ID, bookingId)
            putExtra(QrScannerActivity.EXTRA_SCAN_TYPE, scanType.name)
        }
        startActivityForResult(intent, QR_SCAN_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == QR_SCAN_REQUEST && resultCode == Activity.RESULT_OK) {
            val qrCode = data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE) ?: return
            val bookingId = data.getStringExtra(QrScannerActivity.EXTRA_BOOKING_ID) ?: return
            
            handleQrScanned(bookingId, qrCode)
        }
    }
    
    private fun handleQrScanned(bookingId: String, qrCode: String) {
        val currentBooking = booking ?: return
        
        when (currentBooking.status) {
            "pending" -> {
                viewModel.validateCheckInQr(bookingId, qrCode)
            }
            "active" -> {
                viewModel.validateCheckOutQr(bookingId, qrCode)
            }
            else -> {
                Toast.makeText(context, "Invalid booking status", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showValidationDialog(validation: com.gridee.parking.data.model.QrValidationResult) {
        if (!validation.valid) {
            AlertDialog.Builder(requireContext())
                .setTitle("❌ Invalid QR Code")
                .setMessage(validation.message)
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        val currentBooking = booking ?: return
        val bookingId = currentBooking.id ?: return
        
        when (currentBooking.status) {
            "pending" -> showCheckInConfirmDialog(bookingId, validation)
            "active" -> showCheckOutConfirmDialog(bookingId, validation)
        }
    }
    
    private fun showCheckInConfirmDialog(
        bookingId: String, 
        validation: com.gridee.parking.data.model.QrValidationResult
    ) {
        val message = if (validation.penalty > 0) {
            "⚠️ You are checking in late.\n\nLate Check-in Penalty: ₹${validation.penalty}\n\nDo you want to proceed?"
        } else {
            "✅ Check in to activate your parking booking?"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Check-In")
            .setMessage(message)
            .setPositiveButton("Check In") { _, _ ->
                viewModel.checkIn(bookingId, bookingId) // QR code is bookingId
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCheckOutConfirmDialog(
        bookingId: String, 
        validation: com.gridee.parking.data.model.QrValidationResult
    ) {
        val message = if (validation.penalty > 0) {
            "⚠️ Final Charges:\n\nLate Check-out Penalty: ₹${validation.penalty}\n\nDo you want to complete check-out?"
        } else {
            "✅ Complete your parking session?"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Check-Out")
            .setMessage(message)
            .setPositiveButton("Check Out") { _, _ ->
                viewModel.checkOut(bookingId, bookingId) // QR code is bookingId
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateUI(booking: Booking) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        
        binding.apply {
            // Basic info
            tvSpotName.text = "Spot: ${booking.spotId}"
            tvLotId.text = "Lot ID: ${booking.lotId}"
            tvVehicleNumber.text = "Vehicle: ${booking.vehicleNumber ?: "N/A"}"
            tvAmount.text = "₹${booking.amount}"
            
            // Status
            tvStatus.text = booking.status.uppercase()
            tvStatus.setTextColor(
                when (booking.status) {
                    "pending" -> android.graphics.Color.parseColor("#E65100")
                    "active" -> android.graphics.Color.parseColor("#2E7D32")
                    "completed" -> android.graphics.Color.parseColor("#616161")
                    else -> android.graphics.Color.GRAY
                }
            )
            
            // Times
            booking.checkInTime?.let {
                tvCheckInTime.text = "Check-in: ${dateFormat.format(it)}"
            }
            booking.checkOutTime?.let {
                tvCheckOutTime.text = "Check-out: ${dateFormat.format(it)}"
            }
            
            // Show/hide buttons based on status
            when (booking.status) {
                "pending" -> {
                    btnCheckIn.visibility = View.VISIBLE
                    btnCheckOut.visibility = View.GONE
                }
                "active" -> {
                    btnCheckIn.visibility = View.GONE
                    btnCheckOut.visibility = View.VISIBLE
                }
                else -> {
                    btnCheckIn.visibility = View.GONE
                    btnCheckOut.visibility = View.GONE
                }
            }
        }
    }
    
    private fun startPenaltyTracking(bookingId: String) {
        stopPenaltyTracking()
        
        penaltyUpdateJob = lifecycleScope.launch {
            while (isActive) {
                viewModel.loadPenaltyInfo(bookingId)
                delay(30000) // Update every 30 seconds
            }
        }
    }
    
    private fun stopPenaltyTracking() {
        penaltyUpdateJob?.cancel()
        penaltyUpdateJob = null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopPenaltyTracking()
        _binding = null
    }
}
```

#### 4.3 Update BookingsActivity.kt
Modify the click handler to show the BottomSheet:

```kotlin
// File: app/src/main/java/com/gridee/parking/ui/bookings/BookingsActivity.kt
// UPDATE the showBookingDetails method:

private fun showBookingDetails(booking: Booking) {
    val bookingId = booking.id
    if (bookingId.isNotBlank()) {
        val bottomSheet = BookingDetailBottomSheet.newInstance(bookingId)
        bottomSheet.show(supportFragmentManager, "BookingDetailBottomSheet")
    } else {
        Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
    }
}
```

#### 4.4 Create BottomSheet Layout
```xml
<!-- File: app/src/main/res/layout/bottom_sheet_booking_detail.xml -->

<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bottom_sheet_background">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <!-- Header -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Booking Details"
                android:textSize="20sp"
                android:textStyle="bold"
                android:textColor="@android:color/black"/>

            <ImageButton
                android:id="@+id/btn_close"
                android:layout_width="32dp"
                android:layout_height="32dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_close"
                android:contentDescription="Close"/>
        </LinearLayout>

        <!-- Progress Bar -->
        <ProgressBar
            android:id="@+id/progress_bar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:visibility="gone"/>

        <!-- Status Badge -->
        <TextView
            android:id="@+id/tv_status"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:padding="8dp"
            android:textSize="14sp"
            android:textStyle="bold"/>

        <!-- Penalty Warning -->
        <TextView
            android:id="@+id/tv_penalty_warning"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="#FFEBEE"
            android:padding="12dp"
            android:textColor="#C62828"
            android:textSize="14sp"
            android:visibility="gone"/>

        <!-- Booking Info Card -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            app:cardCornerRadius="8dp"
            app:cardElevation="2dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:id="@+id/tv_spot_name"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Spot: A-15"
                    android:textSize="16sp"
                    android:textStyle="bold"/>

                <TextView
                    android:id="@+id/tv_lot_id"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Lot ID: 101"
                    android:textSize="14sp"/>

                <TextView
                    android:id="@+id/tv_vehicle_number"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Vehicle: DL01AB1234"
                    android:textSize="14sp"/>

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginTop="12dp"
                    android:layout_marginBottom="12dp"
                    android:background="#E0E0E0"/>

                <TextView
                    android:id="@+id/tv_check_in_time"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Check-in: Oct 24, 2025 at 10:00 AM"
                    android:textSize="14sp"/>

                <TextView
                    android:id="@+id/tv_check_out_time"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Check-out: Oct 24, 2025 at 06:00 PM"
                    android:textSize="14sp"/>

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginTop="12dp"
                    android:layout_marginBottom="12dp"
                    android:background="#E0E0E0"/>

                <TextView
                    android:id="@+id/tv_amount"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="₹200.00"
                    android:textSize="24sp"
                    android:textStyle="bold"
                    android:textColor="@android:color/black"
                    android:gravity="center"/>
            </LinearLayout>
        </androidx.cardview.widget.CardView>

        <!-- Action Buttons -->
        <Button
            android:id="@+id/btn_check_in"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:layout_marginTop="16dp"
            android:text="📷 Scan QR to Check In"
            android:textSize="16sp"
            android:backgroundTint="@color/primary"
            android:visibility="gone"/>

        <Button
            android:id="@+id/btn_check_out"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:layout_marginTop="16dp"
            android:text="📷 Scan QR to Check Out"
            android:textSize="16sp"
            android:backgroundTint="@color/primary"
            android:visibility="gone"/>

        <Button
            android:id="@+id/btn_refresh"
            android:layout_width="match_parent"
            android:layout_height="48dp"
            android:layout_marginTop="8dp"
            android:text="🔄 Refresh"
            android:textSize="14sp"
            style="@style/Widget.MaterialComponents.Button.OutlinedButton"/>

    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```


---

### **Phase 5: Testing** (2 hours)

#### 5.1 Test Scenarios

**Check-In Tests:**
1. ✅ Scan valid QR for pending booking
2. ✅ On-time check-in (no penalty)
3. ✅ Late check-in (with penalty warning)
4. ✅ Invalid QR code
5. ✅ Wrong booking status
6. ✅ Already has active booking

**Check-Out Tests:**
1. ✅ Scan valid QR for active booking
2. ✅ On-time check-out (no penalty)
3. ✅ Late check-out (with penalty)
4. ✅ Insufficient wallet balance for penalty
5. ✅ Invalid QR code
6. ✅ Wrong booking status

**Penalty Tracking Tests:**
1. ✅ Real-time penalty updates
2. ✅ Grace period (10 minutes)
3. ✅ Penalty calculation accuracy

---

## 📅 Implementation Timeline

### Day 1 (6 hours)
- ✅ **Morning (3 hours):** Phase 1 & 2
  - Update data models
  - Update API layer
  - Update repository

- ✅ **Afternoon (3 hours):** Phase 3
  - Add dependencies
  - Implement QR scanner
  - Test camera integration

### Day 2 (6 hours)
- ✅ **Morning (4 hours):** Phase 4
  - Update ViewModel
  - Update UI screens
  - Implement validation dialogs

- ✅ **Afternoon (2 hours):** Phase 5
  - Testing
  - Bug fixes
  - Polish UI

---

## 🎨 UI/UX Considerations

### Check-In Flow:
1. User taps "Check In" button
2. Camera opens with QR scanner
3. User scans QR at parking spot
4. App validates QR:
   - If valid + no penalty → Show confirmation
   - If valid + penalty → Show penalty warning
   - If invalid → Show error
5. User confirms → Check-in completes
6. Status changes to "ACTIVE"

### Check-Out Flow:
1. User taps "Check Out" button
2. Camera opens with QR scanner
3. User scans QR at parking spot
4. App validates QR:
   - If valid + no penalty → Show confirmation
   - If valid + penalty → Show final charges
   - If invalid → Show error
5. User confirms → Check-out completes
6. Status changes to "COMPLETED"
7. Navigate to receipt screen

### Active Booking Screen:
- Show live penalty counter (updates every 30s)
- Red warning when overtime
- Countdown to scheduled end time
- Prominent "Check Out" button

---

## 🔒 Error Handling

### Backend Errors to Handle:
1. **Invalid QR Code** → "QR code doesn't match booking"
2. **Wrong Status** → "Booking is not in correct status"
3. **Already Active** → "You already have an active booking"
4. **Insufficient Funds** → "Not enough wallet balance for penalties"
5. **Network Error** → "Connection failed, please retry"
6. **Not Found** → "Booking not found"

### User-Friendly Messages:
```kotlin
when (errorCode) {
    "INVALID_QR" -> "This QR code doesn't match your booking"
    "WRONG_STATUS" -> "This booking cannot be checked in/out"
    "ALREADY_ACTIVE" -> "Please check out from your current booking first"
    "INSUFFICIENT_FUNDS" -> "Please top up your wallet to pay penalties"
    else -> "Something went wrong. Please try again."
}
```

---

## ✅ Checklist

### Phase 1: Data Models
- [ ] Update Booking.kt with missing fields
- [ ] Create QrValidationResult model
- [ ] Create QrCodeRequest model
- [ ] Test serialization/deserialization

### Phase 2: API Layer
- [ ] Add QR endpoints to ApiService
- [ ] Add repository methods
- [ ] Test API calls with backend
- [ ] Handle all error cases

### Phase 3: QR Scanner
- [ ] Add ZXing dependencies
- [ ] Add camera permission
- [ ] Create QrScannerActivity
- [ ] Create scanner layout
- [ ] Test camera on device

### Phase 4: UI Integration
- [ ] Update BookingDetailViewModel
- [ ] Update BookingDetailActivity
- [ ] Add check-in button
- [ ] Add check-out button
- [ ] Add validation dialogs
- [ ] Add penalty tracking
- [ ] Add loading states

### Phase 5: Testing
- [ ] Test all check-in scenarios
- [ ] Test all check-out scenarios
- [ ] Test penalty calculation
- [ ] Test error handling
- [ ] Test with real backend
- [ ] UI/UX polish

---

## 🚀 Deployment

### Prerequisites:
1. Backend running on http://192.168.x.x:8080
2. MongoDB with dummy booking data
3. Android device with camera
4. Wallet with sufficient balance

### Testing Flow:
1. Create booking via app
2. Wait for scheduled check-in time
3. Scan QR code to check in
4. Verify status changes to "active"
5. Wait past scheduled check-out time
6. Scan QR code to check out
7. Verify penalties applied
8. Check wallet deduction
9. Verify status changes to "completed"

---

## 📚 References

- Backend Controller: `/src/main/java/com/parking/app/controller/BookingController.java`
- Backend Service: `/src/main/java/com/parking/app/service/BookingService.java`
- ZXing Documentation: https://github.com/journeyapps/zxing-android-embedded
- QR Code Best Practices: https://developer.android.com/training/camera

---

## 🎯 Success Criteria

- ✅ User can check in using QR code
- ✅ User can check out using QR code
- ✅ Penalties calculated correctly (10-min grace)
- ✅ Penalty warnings shown before action
- ✅ Real-time penalty tracking for active bookings
- ✅ All error cases handled gracefully
- ✅ Smooth user experience
- ✅ Backend integration works correctly

---

**Ready to start implementation? Let's begin with Phase 1! 🚀**
