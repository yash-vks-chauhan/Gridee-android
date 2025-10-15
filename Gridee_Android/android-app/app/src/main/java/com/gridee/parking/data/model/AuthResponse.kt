package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for JWT-based authentication
 * Used by POST /api/auth/login endpoint
 */
data class AuthResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("role")
    val role: String
)

/**
 * Request model for JWT-based authentication
 */
data class AuthRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)
