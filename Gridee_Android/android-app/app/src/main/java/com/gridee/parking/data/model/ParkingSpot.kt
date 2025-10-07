package com.gridee.parking.data.model

data class ParkingSpot(
    val id: String,
    val lotId: String,
    val name: String? = null,  // The actual spot name like "TP Avenue", "Medical College"
    val zoneName: String? = null,  // Keep for backwards compatibility
    val capacity: Int = 0,
    val available: Int = 0,
    val status: String
)
