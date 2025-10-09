package com.gridee.parking.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BookingConfirmationViewModel : ViewModel() {

    private val _bookingDetails = MutableLiveData<BookingConfirmationDetails>()
    val bookingDetails: LiveData<BookingConfirmationDetails> = _bookingDetails

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadBookingDetails(bookingId: String, transactionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Simulate API call to get booking details
                delay(1000)
                
                // Mock booking details - in real app, fetch from API
                val details = BookingConfirmationDetails(
                    bookingId = bookingId,
                    transactionId = transactionId,
                    parkingSpotName = "Downtown Plaza Parking",
                    parkingAddress = "123 Main Street, Downtown",
                    selectedSpot = "A-15",
                    startTime = System.currentTimeMillis() + (30 * 60 * 1000), // 30 minutes from now
                    endTime = System.currentTimeMillis() + (3 * 60 * 60 * 1000), // 3 hours from now
                    totalAmount = 15.00,
                    paymentMethodDisplay = "Credit Card ending in 1234",
                    timestamp = System.currentTimeMillis()
                )
                
                _bookingDetails.value = details
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class BookingConfirmationDetails(
    val bookingId: String,
    val transactionId: String,
    val parkingSpotName: String,
    val parkingAddress: String,
    val selectedSpot: String?,
    val startTime: Long,
    val endTime: Long,
    val totalAmount: Double,
    val paymentMethodDisplay: String,
    val timestamp: Long
)
