package com.gridee.parking.ui.home

data class HomeParkingSpot(
    val id: String,
    val spotName: String,
    val lotName: String,
    val locationLabel: String,
    val isAvailable: Boolean,
    val availableUnits: Int,
    val capacity: Int,
    val status: String
)
