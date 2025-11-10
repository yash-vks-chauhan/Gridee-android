package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class User(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("email")
    val email: String = "",
    
    @SerializedName("phone")
    val phone: String = "",
    
    @SerializedName("vehicleNumbers")
    val vehicleNumbers: List<String> = emptyList(),
    
    @SerializedName("defaultVehicle")
    val defaultVehicle: String? = null,
    
    @SerializedName("firstUser")
    val firstUser: Boolean = true,
    
    @SerializedName("walletCoins")
    val walletCoins: Int = 0,
    
    @SerializedName("role")
    val role: String? = null,
    
    @SerializedName("parkingLotId")
    val parkingLotId: String? = null,
    
    @SerializedName("parkingLotName")
    val parkingLotName: String? = null
    
    // Removed createdAt and passwordHash to avoid serialization issues
)

data class UserRegistration(
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val parkingLotName: String,
    val vehicleNumbers: List<String> = emptyList()
)
