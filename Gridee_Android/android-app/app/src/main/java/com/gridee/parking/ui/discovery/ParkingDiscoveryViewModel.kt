package com.gridee.parking.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.ParkingLot
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.repository.ParkingRepository
import kotlinx.coroutines.launch

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

class ParkingDiscoveryViewModel : ViewModel() {
    
    private val parkingRepository = ParkingRepository()
    
    private val _parkingSpots = MutableLiveData<List<ParkingSpot>>()
    val parkingSpots: LiveData<List<ParkingSpot>> = _parkingSpots
    
    private val _parkingLots = MutableLiveData<List<ParkingLot>>()
    val parkingLots: LiveData<List<ParkingLot>> = _parkingLots
    
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery
    
    // Filter options
    private val _maxPrice = MutableLiveData<Double>()
    private val _maxDistance = MutableLiveData<Double>()
    private val _selectedAmenities = MutableLiveData<List<String>>()
    private val _availableOnly = MutableLiveData<Boolean>()
    
    init {
        loadParkingData()
    }
    
    fun searchParking(query: String) {
        _searchQuery.value = query
        _isLoading.value = true
        
        // Filter existing data based on search query
        filterParkingSpots()
    }
    
    fun loadParkingData() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // Load parking lots
                val lotsResponse = parkingRepository.getParkingLots()
                
                // Load parking spots
                val spotsResponse = parkingRepository.getParkingSpots()
                
                if (lotsResponse.isSuccessful && spotsResponse.isSuccessful) {
                    val lots = lotsResponse.body() ?: emptyList()
                    val spots = spotsResponse.body() ?: emptyList()
                    
                    // Update lot data with actual spot counts from parking spots API
                    val updatedLots = lots.map { lot ->
                        val lotsSpots = spots.filter { it.lotId == lot.id }
                        val actualTotalSpots = lotsSpots.size
                        val actualAvailableSpots = lotsSpots.count { it.status == "available" }
                        
                        lot.copy(
                            totalSpots = actualTotalSpots,
                            availableSpots = actualAvailableSpots
                        )
                    }
                    
                    _parkingLots.value = updatedLots
                    _parkingSpots.value = spots
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                // Handle error - could emit error state
            }
        }
    }
    
    fun loadParkingSpotsForLot(lotId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // Load all parking spots and filter by lot ID
                val spotsResponse = parkingRepository.getParkingSpots()
                if (spotsResponse.isSuccessful) {
                    val allSpots = spotsResponse.body() ?: emptyList()
                    val filteredSpots = allSpots.filter { it.lotId == lotId }
                    _parkingSpots.value = filteredSpots
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                // Handle error - could emit error state
            }
        }
    }
    
    fun getCurrentLocation() {
        _isLoading.value = true
        
        // TODO: Implement actual location detection
        // Mock current location (Chennai, near SRM University)
        _currentLocation.value = Location(
            latitude = 12.8231,
            longitude = 80.0414,
            address = "Kattankulathur, SRM Nagar, Chennai"
        )
        
        loadParkingData()
    }
    
    private fun filterParkingSpots() {
        val allSpots = _parkingSpots.value ?: emptyList()
        val query = _searchQuery.value?.lowercase() ?: ""
        
        val filteredSpots = allSpots.filter { spot ->
            val matchesQuery = query.isEmpty() || 
                (spot.name?.lowercase()?.contains(query) == true) ||
                (spot.zoneName?.lowercase()?.contains(query) == true) ||
                spot.status.lowercase().contains(query)
            
            val matchesAvailability = _availableOnly.value?.let { availableOnly -> 
                if (availableOnly) spot.available > 0 else true 
            } ?: true
            
            matchesQuery && matchesAvailability
        }
        
        _parkingSpots.value = filteredSpots
        _isLoading.value = false
    }
    
    fun applyFilters(
        maxDistance: Double?,
        selectedAmenities: List<String>,
        availableOnly: Boolean
    ) {
        _maxDistance.value = maxDistance
        _selectedAmenities.value = selectedAmenities
        _availableOnly.value = availableOnly
        
        filterParkingSpots()
    }
}
