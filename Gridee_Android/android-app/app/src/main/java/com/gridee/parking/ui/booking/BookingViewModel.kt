package com.gridee.parking.ui.booking

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.repository.BookingRepository
import com.gridee.parking.data.repository.UserRepository
import com.gridee.parking.data.repository.WalletRepository
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.Vehicle
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import kotlin.math.ceil

data class BookingDetails(
    val id: String,
    val parkingSpotId: String,
    val parkingSpotName: String,
    val startTime: Date,
    val endTime: Date,
    val duration: String,
    val pricePerHour: Double,
    val totalPrice: Double,
    val selectedSpot: String?,
    val status: BookingStatus,
    val createdAt: Date,
    val paymentMethod: String?,
    val transactionId: String?
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    EXPIRED
}

class BookingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bookingRepository = BookingRepository(application)
    private val parkingRepository = com.gridee.parking.data.repository.ParkingRepository()
    private val walletRepository = WalletRepository(application)
    
    private val _startTime = MutableLiveData<Date>()
    val startTime: LiveData<Date> = _startTime
    
    private val _endTime = MutableLiveData<Date>()
    val endTime: LiveData<Date> = _endTime
    
    private val _selectedSpot = MutableLiveData<String?>()
    val selectedSpot: LiveData<String?> = _selectedSpot
    
    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice
    
    private val _duration = MutableLiveData<String>()
    val duration: LiveData<String> = _duration
    
    private val _parkingSpot = MutableLiveData<ParkingSpot>()
    val parkingSpot: LiveData<ParkingSpot> = _parkingSpot
    
    // Backend integration
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _bookingCreated = MutableLiveData<Booking?>()
    val bookingCreated: LiveData<Booking?> = _bookingCreated
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _vehicleNumber = MutableLiveData<String>()
    val vehicleNumber: LiveData<String> = _vehicleNumber
    
    private val _selectedVehicle = MutableLiveData<Vehicle?>()
    val selectedVehicle: LiveData<Vehicle?> = _selectedVehicle
    
    private val _userVehicles = MutableLiveData<List<Vehicle>>()
    val userVehicles: LiveData<List<Vehicle>> = _userVehicles
    
    private val _bookings = MutableLiveData<List<BookingDetails>>()
    val bookings: LiveData<List<BookingDetails>> = _bookings
    
    private val _activeBookings = MutableLiveData<List<BookingDetails>>()
    val activeBookings: LiveData<List<BookingDetails>> = _activeBookings
    
    private val _bookingHistory = MutableLiveData<List<BookingDetails>>()
    val bookingHistory: LiveData<List<BookingDetails>> = _bookingHistory
    
    private val _walletBalance = MutableLiveData<Double>()
    val walletBalance: LiveData<Double> = _walletBalance
    
    init {
        // No dummy data; bookings will be populated from backend when needed
        loadWalletBalance()
    }
    
    fun setStartTime(time: Date) {
        _startTime.value = time
    }
    
    fun setEndTime(time: Date) {
        _endTime.value = time
    }
    
    fun setSelectedSpot(spot: String?) {
        _selectedSpot.value = spot
    }
    
    fun setParkingSpot(spot: ParkingSpot) {
        _parkingSpot.value = spot
    }
    
    fun loadParkingSpotById(spotId: String, onResult: (ParkingSpot?) -> Unit) {
        viewModelScope.launch {
            try {
                val spotResponse = parkingRepository.getParkingSpotById(spotId)
                if (spotResponse.isSuccessful) onResult(spotResponse.body()) else onResult(null)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    fun loadParkingSpotsForLot(lotId: String, lotName: String? = null, onResult: (List<ParkingSpot>) -> Unit) {
        viewModelScope.launch {
            try {
                // Match iOS: prefer lotName, then fall back to lotId
                val attempts = listOf(lotName, lotId).filter { !it.isNullOrBlank() }.distinct()
                var spots: List<ParkingSpot> = emptyList()

                for ((index, key) in attempts.withIndex()) {
                    try {
                        val spotsResponse = parkingRepository.getParkingSpotsByLot(key!!)
                        if (spotsResponse.isSuccessful) {
                            val body = spotsResponse.body() ?: emptyList()
                            println("DEBUG BookingViewModel.loadParkingSpotsForLot: Fetched spots with key='$key', size=${body.size}")
                            spots = body
                            if (body.isNotEmpty() || index == attempts.lastIndex) break
                        }
                    } catch (_: Exception) {
                        // Continue to next attempt
                    }
                }

                println("DEBUG BookingViewModel.loadParkingSpotsForLot: Final result size=${spots.size}")
                onResult(spots)
            } catch (e: Exception) {
                println("DEBUG BookingViewModel.loadParkingSpotsForLot: Exception - ${e.message}")
                onResult(emptyList())
            }
        }
    }
    
    fun loadAllParkingSpots(onResult: (List<ParkingSpot>) -> Unit) {
        viewModelScope.launch {
            try {
                // Aggregate by-lot to avoid ADMIN-only all-spots endpoint
                val lotsResponse = parkingRepository.getParkingLots()
                if (!lotsResponse.isSuccessful) return@launch onResult(emptyList())
                val lots = lotsResponse.body() ?: emptyList()
                val combined = mutableListOf<ParkingSpot>()
                for (lot in lots) {
                    // Try with lotName first (iOS-compatible), then lotId
                    val attempts = listOf(lot.name, lot.id).filter { !it.isNullOrBlank() }.distinct()
                    for ((index, key) in attempts.withIndex()) {
                        try {
                            val resp = parkingRepository.getParkingSpotsByLot(key!!)
                            if (resp.isSuccessful) {
                                val spots = resp.body() ?: emptyList()
                                combined.addAll(spots)
                                if (spots.isNotEmpty() || index == attempts.lastIndex) break
                            }
                        } catch (_: Exception) { /* try next */ }
                    }
                }
                onResult(combined)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    fun setVehicleNumber(vehicleNumber: String) {
        _vehicleNumber.value = vehicleNumber
    }
    
    fun setSelectedVehicle(vehicle: Vehicle) {
        _selectedVehicle.value = vehicle
        _vehicleNumber.value = vehicle.number
    }
    
    fun loadUserVehicles() {
        // Get user ID from SharedPreferences
        val sharedPref = getApplication<Application>().getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        
        if (userId != null) {
            println("BookingViewModel: Loading vehicles for user $userId")
            loadUserVehiclesFromProfile(userId)
        } else {
            println("BookingViewModel: No user ID found")
            // No user logged in, show empty list
            _userVehicles.value = emptyList()
        }
    }
    
    private fun loadUserVehiclesFromProfile(userId: String) {
        viewModelScope.launch {
            try {
                println("BookingViewModel: Fetching user profile from API")
                val userRepository = UserRepository()
                val user = userRepository.getUserById(userId)
                
                if (user != null) {
                    println("BookingViewModel: User found with ${user.vehicleNumbers.size} vehicles: ${user.vehicleNumbers}")
                    
                    if (user.vehicleNumbers.isNotEmpty()) {
                        val vehicles = user.vehicleNumbers.mapIndexed { index, vehicleNumber ->
                            Vehicle(
                                id = "user_vehicle_$index",
                                number = vehicleNumber,
                                type = "Car", // Default type since we only store numbers
                                brand = "User",
                                model = "Vehicle",
                                isDefault = index == 0 // First vehicle is default
                            )
                        }
                        _userVehicles.value = vehicles
                        println("BookingViewModel: Set ${vehicles.size} vehicles in LiveData")
                        
                        // Auto-select first vehicle if available and none selected
                        if (vehicles.isNotEmpty() && _selectedVehicle.value == null) {
                            setSelectedVehicle(vehicles.first())
                            println("BookingViewModel: Auto-selected first vehicle: ${vehicles.first().number}")
                        }
                    } else {
                        println("BookingViewModel: User has no vehicles")
                        // User has no vehicles, show empty list
                        _userVehicles.value = emptyList()
                    }
                } else {
                    println("BookingViewModel: User not found in API response")
                    // User not found, show empty list
                    _userVehicles.value = emptyList()
                }
            } catch (e: Exception) {
                println("BookingViewModel: Exception loading vehicles: ${e.message}")
                e.printStackTrace()
                // On error, show empty list
                _userVehicles.value = emptyList()
            }
        }
    }
    
    // Removed mock vehicle generator
    
    // Backend integration - Create actual booking
    fun createBackendBooking() {
        val start = _startTime.value
        val end = _endTime.value
        val spot = _parkingSpot.value
        val vehicle = _vehicleNumber.value
        
        if (start == null || end == null || spot == null || vehicle.isNullOrEmpty()) {
            _errorMessage.value = "Please fill all required fields"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.startBooking(
                    spotId = spot.id,
                    lotId = spot.lotId, // Use the correct lot ID from the spot
                    checkInTime = start,
                    checkOutTime = end,
                    vehicleNumber = vehicle
                ).fold(
                    onSuccess = { booking ->
                        _bookingCreated.value = booking
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to create booking"
                        _bookingCreated.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error creating booking: ${e.message}"
                _bookingCreated.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearBookingCreated() {
        _bookingCreated.value = null
    }
    
    fun calculatePricing() {
        val start = _startTime.value
        val end = _endTime.value
        val spot = _parkingSpot.value
        
        if (start != null && end != null && spot != null) {
            val durationMillis = end.time - start.time
            val durationHours = durationMillis / (1000.0 * 60 * 60)
            
            // Round up to nearest hour for pricing
            val billingHours = ceil(durationHours)
            // Fixed price: ₹2.5 per hour
            val pricePerHour = 2.5
            val price = billingHours * pricePerHour
            
            _totalPrice.value = price
            _duration.value = formatDuration(durationMillis)
        }
    }
    
    private fun formatDuration(durationMillis: Long): String {
        val hours = (durationMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((durationMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        
        return when {
            hours == 0 -> "${minutes}m"
            minutes == 0 -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }
    
    fun createBooking(): BookingDetails? {
        val start = _startTime.value
        val end = _endTime.value
        val spot = _parkingSpot.value
        val price = _totalPrice.value
        val durationStr = _duration.value
        val selectedSpotStr = _selectedSpot.value
        
        if (start != null && end != null && spot != null && price != null && durationStr != null) {
            return BookingDetails(
                id = generateBookingId(),
                parkingSpotId = spot.id,
                parkingSpotName = spot.name ?: spot.zoneName ?: "Unknown Spot",
                startTime = start,
                endTime = end,
                duration = durationStr,
                pricePerHour = 2.5, // Default price since it's not in API
                totalPrice = price,
                selectedSpot = selectedSpotStr,
                status = BookingStatus.PENDING,
                createdAt = Date(),
                paymentMethod = null,
                transactionId = null
            )
        }
        return null
    }
    
    private fun generateBookingId(): String {
        return "BK${System.currentTimeMillis()}"
    }
    
    // Removed mock bookings; rely on backend data only
    
    fun extendBooking(bookingId: String, additionalHours: Int) {
        // TODO: Implement booking extension
    }
    
    fun cancelBooking(bookingId: String) {
        // TODO: Implement booking cancellation
    }
    
    fun modifyBooking(bookingId: String, newStartTime: Date, newEndTime: Date) {
        // TODO: Implement booking modification
    }
    
    fun loadWalletBalance() {
        viewModelScope.launch {
            try {
                println("BookingViewModel: Loading wallet balance")
                walletRepository.getWalletDetails().fold(
                    onSuccess = { details ->
                        val balance = details.balance ?: 0.0
                        println("BookingViewModel: Wallet balance loaded: $balance")
                        _walletBalance.value = balance
                    },
                    onFailure = { exception ->
                        println("BookingViewModel: Failed to load wallet balance: ${exception.message}")
                        _walletBalance.value = 0.0
                    }
                )
            } catch (e: Exception) {
                println("BookingViewModel: Exception loading wallet balance: ${e.message}")
                _walletBalance.value = 0.0
            }
        }
    }
    
    fun addVehicleToProfile(vehicleNumber: String, onResult: (Boolean) -> Unit) {
        val sharedPref = getApplication<Application>().getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        
        println("BookingViewModel: addVehicleToProfile called with vehicle: $vehicleNumber, userId: $userId")
        
        if (userId == null) {
            println("BookingViewModel: No user ID found")
            onResult(false)
            return
        }
        
        viewModelScope.launch {
            try {
                println("BookingViewModel: Fetching user profile for vehicle addition")
                val userRepository = UserRepository()
                val user = userRepository.getUserById(userId)
                
                if (user != null) {
                    println("BookingViewModel: User found, current vehicles: ${user.vehicleNumbers}")
                    
                    // Check if vehicle already exists
                    if (user.vehicleNumbers.contains(vehicleNumber)) {
                        println("BookingViewModel: Vehicle already exists")
                        onResult(false)
                        return@launch
                    }
                    
                    val updatedVehicles = user.vehicleNumbers.toMutableList()
                    updatedVehicles.add(vehicleNumber)
                    
                    println("BookingViewModel: Updating user with vehicles: $updatedVehicles")
                    val updatedUser = user.copy(vehicleNumbers = updatedVehicles)
                    val result = userRepository.updateUser(updatedUser)
                    
                    println("BookingViewModel: Update result: $result")
                    onResult(result)
                } else {
                    println("BookingViewModel: User not found")
                    onResult(false)
                }
            } catch (e: Exception) {
                println("BookingViewModel: Exception in addVehicleToProfile: ${e.message}")
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}
