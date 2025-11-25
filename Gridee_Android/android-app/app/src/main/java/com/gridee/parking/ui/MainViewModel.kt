package com.gridee.parking.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.ParkingLot
import com.gridee.parking.ui.home.HomeParkingSpot
import com.gridee.parking.data.repository.ParkingRepository
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val parkingRepository = ParkingRepository()

    private val _parkingSpots = MutableLiveData<List<HomeParkingSpot>>()
    val parkingSpots: LiveData<List<HomeParkingSpot>> = _parkingSpots
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refreshParkingSnapshot()
    }
    
    fun refreshParkingSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            emitParkingSnapshot()
        }
    }
    
    fun refreshLocation() {
        refreshParkingSnapshot()
    }
    
    fun refreshNearbyParking() {
        refreshParkingSnapshot()
    }

    private suspend fun emitParkingSnapshot() {
        _isLoading.postValue(true)
        try {
            val spots = fetchHomeParkingSpots()
            _parkingSpots.postValue(spots)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load parking snapshot", e)
            clearSnapshot()
        } finally {
            _isLoading.postValue(false)
        }
    }

    private suspend fun fetchHomeParkingSpots(): List<HomeParkingSpot> {
        val response = parkingRepository.getParkingLots()
        if (!response.isSuccessful) {
            Log.w(TAG, "getParkingLots failed: ${response.code()} ${response.message()}")
            return emptyList()
        }
        val lots = response.body().orEmpty()
        if (lots.isEmpty()) return emptyList()

        val snapshots = mutableListOf<HomeParkingSpot>()
        for (lot in lots) {
            try {
                val spotsResponse = parkingRepository.getParkingSpotsByLot(lot.id)
                if (!spotsResponse.isSuccessful) continue
                val spots = spotsResponse.body().orEmpty()
                val locationLabel = buildLocationLabel(lot)
                for (spot in spots) {
                    val title = spot.name?.takeIf { it.isNotBlank() } ?: spot.zoneName ?: continue
                    val isAvailable = spot.status.equals("available", true) || spot.available > 0
                    snapshots += HomeParkingSpot(
                        id = spot.id,
                        spotName = title,
                        lotName = lot.name,
                        locationLabel = locationLabel,
                        isAvailable = isAvailable,
                        availableUnits = max(spot.available, 0),
                        capacity = spot.capacity,
                        status = spot.status
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load spots for lot ${lot.id}", e)
            }
        }

        return snapshots
            .sortedWith(
                compareByDescending<HomeParkingSpot> { it.isAvailable }
                    .thenByDescending { it.availableUnits }
                    .thenBy { it.spotName.lowercase(Locale.getDefault()) }
            )
            .take(MAX_SPOTS)
    }

    private fun buildLocationLabel(lot: ParkingLot): String {
        val parts = listOfNotNull(
            lot.location?.takeIf { it.isNotBlank() },
            lot.address.takeIf { it.isNotBlank() }
        ).distinct()
        return if (parts.isEmpty()) lot.name else parts.joinToString(" • ")
    }

    private fun clearSnapshot() {
        _parkingSpots.postValue(emptyList())
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_SPOTS = 12
    }
}
