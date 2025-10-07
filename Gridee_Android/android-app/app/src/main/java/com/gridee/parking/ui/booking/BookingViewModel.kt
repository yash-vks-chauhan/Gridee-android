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
    
    init {
        loadMockBookings()
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
                val spotsResponse = parkingRepository.getParkingSpots()
                if (spotsResponse.isSuccessful) {
                    val spots = spotsResponse.body() ?: emptyList()
                    val spot = spots.find { it.id == spotId }
                    onResult(spot)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    fun loadParkingSpotsForLot(lotId: String, onResult: (List<ParkingSpot>) -> Unit) {
        viewModelScope.launch {
            try {
                val spotsResponse = parkingRepository.getParkingSpots()
                if (spotsResponse.isSuccessful) {
                    val allSpots = spotsResponse.body() ?: emptyList()
                    val lotsSpots = allSpots.filter { it.lotId == lotId }
                    onResult(lotsSpots)
                } else {
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }
    
    fun loadAllParkingSpots(onResult: (List<ParkingSpot>) -> Unit) {
        viewModelScope.launch {
            try {
                val spotsResponse = parkingRepository.getParkingSpots()
                if (spotsResponse.isSuccessful) {
                    val allSpots = spotsResponse.body() ?: emptyList()
                    onResult(allSpots)
                } else {
                    onResult(emptyList())
                }
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
    
    // Remove the mock data method
    private fun generateMockVehicles(): List<Vehicle> {
        return listOf(
            Vehicle(
                id = "1",
                number = "MH 12 AB 1234",
                type = "Car",
                brand = "Maruti",
                model = "Swift",
                isDefault = true
            ),
            Vehicle(
                id = "2", 
                number = "MH 14 CD 5678",
                type = "Car",
                brand = "Honda",
                model = "City",
                isDefault = false
            ),
            Vehicle(
                id = "3",
                number = "MH 01 EF 9012", 
                type = "Bike",
                brand = "Honda",
                model = "Activa",
                isDefault = false
            )
        )
    }
    
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
    
    private fun loadMockBookings() {
        val mockBookings = generateMockBookings()
        _bookings.value = mockBookings
        
        // Separate active and history
        val active = mockBookings.filter { 
            it.status == BookingStatus.ACTIVE || it.status == BookingStatus.CONFIRMED 
        }
        val history = mockBookings.filter { 
            it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED 
        }
        
        _activeBookings.value = active
        _bookingHistory.value = history
    }
    
    private fun generateMockBookings(): List<BookingDetails> {
        val calendar = Calendar.getInstance()
        
        return listOf(
            BookingDetails(
                id = "BK001",
                parkingSpotId = "1",
                parkingSpotName = "Downtown Parking Garage",
                startTime = calendar.apply { add(Calendar.HOUR, 2) }.time,
                endTime = calendar.apply { add(Calendar.HOUR, 2) }.time,
                duration = "4h",
                pricePerHour = 15.0,
                totalPrice = 60.0,
                selectedSpot = "Level 2, Spot A-15",
                status = BookingStatus.CONFIRMED,
                createdAt = calendar.apply { add(Calendar.DAY_OF_MONTH, -1) }.time,
                paymentMethod = "Credit Card",
                transactionId = "TXN123456"
            ),
            BookingDetails(
                id = "BK002",
                parkingSpotId = "2",
                parkingSpotName = "Union Square Parking",
                startTime = calendar.apply { set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) - 3) }.time,
                endTime = calendar.apply { add(Calendar.HOUR, 3) }.time,
                duration = "3h",
                pricePerHour = 20.0,
                totalPrice = 60.0,
                selectedSpot = "Level 1, Spot B-08",
                status = BookingStatus.COMPLETED,
                createdAt = calendar.apply { add(Calendar.DAY_OF_MONTH, -4) }.time,
                paymentMethod = "Digital Wallet",
                transactionId = "TXN123457"
            ),
            BookingDetails(
                id = "BK003",
                parkingSpotId = "3",
                parkingSpotName = "Mission Bay Parking Lot",
                startTime = calendar.apply { set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) - 7) }.time,
                endTime = calendar.apply { add(Calendar.HOUR, 2) }.time,
                duration = "2h",
                pricePerHour = 8.0,
                totalPrice = 16.0,
                selectedSpot = null,
                status = BookingStatus.COMPLETED,
                createdAt = calendar.apply { add(Calendar.DAY_OF_MONTH, -8) }.time,
                paymentMethod = "Credit Card",
                transactionId = "TXN123458"
            )
        )
    }
    
    fun extendBooking(bookingId: String, additionalHours: Int) {
        // TODO: Implement booking extension
    }
    
    fun cancelBooking(bookingId: String) {
        // TODO: Implement booking cancellation
    }
    
    fun modifyBooking(bookingId: String, newStartTime: Date, newEndTime: Date) {
        // TODO: Implement booking modification
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
