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
    
    @SerializedName("firstUser")
    val firstUser: Boolean = true,
    
    @SerializedName("walletCoins")
    val walletCoins: Int = 0
    
    // Removed createdAt and passwordHash to avoid serialization issues
)

data class UserRegistration(
    val name: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val vehicleNumbers: List<String> = emptyList()
)
