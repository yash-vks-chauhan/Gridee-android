package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName

data class CheckInRequest(
    @SerializedName("mode") val mode: String,
    @SerializedName("qrCode") val qrCode: String? = null,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
    @SerializedName("pin") val pin: String? = null
)

