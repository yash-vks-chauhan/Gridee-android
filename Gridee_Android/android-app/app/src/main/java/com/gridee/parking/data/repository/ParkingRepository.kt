package com.gridee.parking.data.repository

import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.ParkingLot
import com.gridee.parking.data.model.ParkingSpot
import retrofit2.Response

class ParkingRepository {
    
    private val apiService = ApiClient.apiService
    
    suspend fun getParkingLots(): Response<List<ParkingLot>> {
        return apiService.getParkingLots()
    }
    
    suspend fun getParkingSpots(): Response<List<ParkingSpot>> {
        return apiService.getParkingSpots()
    }
    
    suspend fun getParkingSpotsByLot(lotId: String): Response<List<ParkingSpot>> {
        return apiService.getParkingSpotsByLot(lotId)
    }
}
