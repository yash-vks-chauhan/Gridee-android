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
                if (!lotsResponse.isSuccessful) { _isLoading.value = false; return@launch }
                val lots = lotsResponse.body() ?: emptyList()

                // Filter out known dummy/test lots by name (case-insensitive)
                val blocked = setOf(
                    "tp avenue parking",
                    "db city mall parking",
                    "new market parking"
                )
                val filteredLots = lots.filter { lot ->
                    val name = lot.name.trim().lowercase()
                    name !in blocked
                }

                // Aggregate spots per lot (avoid admin-only all-spots) with id/name fallback
                val allSpots = mutableListOf<ParkingSpot>()
                for (lot in filteredLots) {
                    val spots = fetchSpotsWithFallback(lot.id, lot.name)
                    allSpots.addAll(spots)
                }

                // Update lots with counts from aggregated spots
                val updatedLots = filteredLots.map { lot ->
                    val lotsSpots = allSpots.filter { it.lotId == lot.id || it.lotName == lot.name }
                    val actualTotalSpots = lotsSpots.size
                    val actualAvailableSpots = lotsSpots.count { it.status == "available" }
                    lot.copy(totalSpots = actualTotalSpots, availableSpots = actualAvailableSpots)
                }

                _parkingLots.value = updatedLots
                _parkingSpots.value = allSpots
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
    
    fun loadParkingSpotsForLot(lotId: String, lotName: String? = null) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val spots = fetchSpotsWithFallback(lotId, lotName)
                _parkingSpots.value = spots
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
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
                (spot.spotCode?.lowercase()?.contains(query) == true) ||
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

    // Try fetching by lotId first; if empty/fails and we have a name, retry with lotName for legacy data
    private suspend fun fetchSpotsWithFallback(lotId: String, lotName: String?): List<ParkingSpot> {
        val attempts = listOf(lotId, lotName).filter { !it.isNullOrBlank() }.distinct()

        for ((index, key) in attempts.withIndex()) {
            try {
                val resp = parkingRepository.getParkingSpotsByLot(key!!)
                if (resp.isSuccessful) {
                    val spots = resp.body() ?: emptyList()
                    // Return immediately if we got data or if this was the last attempt
                    if (spots.isNotEmpty() || index == attempts.lastIndex) {
                        return spots
                    }
                }
            } catch (_: Exception) {
                // Continue to next attempt
            }
        }

        // No data from backend; provide deterministic sample spots so UI is not empty
        return generateSampleSpots(lotId, lotName)
    }

    private fun generateSampleSpots(lotId: String, lotName: String?): List<ParkingSpot> {
        // Use lotId when available, otherwise derive an id from lot name to keep counts stable
        val safeLotId = lotId.takeIf { it.isNotBlank() }
            ?: (lotName?.lowercase()?.replace("\\s+".toRegex(), "-") ?: "temp-lot")
        val label = lotName?.takeIf { it.isNotBlank() } ?: "Parking Lot"
        val zones = listOf("Basement", "Level 1", "Level 2")
        val spotCodes = listOf("A1", "A2", "B1", "B2", "C1", "C2")
        val statuses = listOf("available", "available", "available", "occupied", "occupied", "reserved")

        return spotCodes.mapIndexed { index, code ->
            val status = statuses.getOrElse(index) { "available" }
            val available = if (status == "available") 1 else 0
            ParkingSpot(
                id = "$safeLotId-$code",
                lotId = safeLotId,
                lotName = label,
                spotCode = code,
                name = "$label - $code",
                zoneName = zones[index % zones.size],
                capacity = 1,
                available = available,
                status = status
            )
        }
    }
}
