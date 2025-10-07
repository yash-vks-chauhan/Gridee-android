package com.gridee.parking.data.model

import com.google.gson.annotations.SerializedName

data class WalletTransaction(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("type")
    val type: String? = null, // "CREDIT", "DEBIT"
    
    @SerializedName("amount")
    val amount: Double? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("timestamp")
    val timestamp: String? = null,
    
    @SerializedName("balanceAfter")
    val balanceAfter: Double? = null
)

data class WalletDetails(
    @SerializedName("balance")
    val balance: Double? = null,
    
    @SerializedName("transactions")
    val transactions: List<WalletTransaction>? = null
)

data class TopupRequest(
    @SerializedName("amount")
    val amount: Double
)
