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

    suspend fun getParkingLotNames(): Response<List<String>> {
        return apiService.getParkingLotNames()
    }
    
    suspend fun getParkingSpots(): Response<List<ParkingSpot>> {
        return apiService.getParkingSpots()
    }
    
    suspend fun getParkingSpotsByLot(lotId: String): Response<List<ParkingSpot>> {
        return apiService.getParkingSpotsByLot(lotId)
    }

    suspend fun getParkingSpotById(id: String): Response<ParkingSpot> {
        return apiService.getParkingSpotById(id)
    }

    suspend fun getAvailableSpots(lotId: String, startTime: String, endTime: String): Response<List<ParkingSpot>> {
        return apiService.getAvailableSpots(lotId, startTime, endTime)
    }
}
