package com.gridee.parking.ui.bookings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.repository.BookingRepository
import kotlinx.coroutines.launch
import java.util.*

class BookingsViewModel(application: Application) : AndroidViewModel(application) {

    private val bookingRepository = BookingRepository(application)

    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // For booking creation
    private val _bookingCreated = MutableLiveData<Booking?>()
    val bookingCreated: LiveData<Booking?> = _bookingCreated

    fun loadUserBookings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.getUserBookings().fold(
                    onSuccess = { bookingList ->
                        println("BookingsViewModel: Received ${bookingList.size} bookings from repository")
                        for (i in bookingList.indices) {
                            println("BookingsViewModel: Booking $i - ID: ${bookingList[i].id}, Status: ${bookingList[i].status}")
                        }
                        _bookings.value = bookingList
                        _errorMessage.value = null
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _bookings.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load bookings: ${e.message}"
                _bookings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBooking(
        spotId: String,
        lotId: String,
        checkInTime: Date,
        checkOutTime: Date,
        vehicleNumber: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.startBooking(
                    spotId = spotId,
                    lotId = lotId,
                    checkInTime = checkInTime,
                    checkOutTime = checkOutTime,
                    vehicleNumber = vehicleNumber
                ).fold(
                    onSuccess = { booking ->
                        _bookingCreated.value = booking
                        _errorMessage.value = null
                        // Refresh bookings list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                        _bookingCreated.value = null
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create booking: ${e.message}"
                _bookingCreated.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmBooking(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.confirmBooking(bookingId).fold(
                    onSuccess = { booking ->
                        _errorMessage.value = null
                        // Refresh bookings list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to confirm booking: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                bookingRepository.cancelBooking(bookingId).fold(
                    onSuccess = {
                        _errorMessage.value = null
                        // Refresh bookings list
                        loadUserBookings()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to cancel booking: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearBookingCreated() {
        _bookingCreated.value = null
    }

    // Legacy method for backward compatibility
    fun loadUserBookings(userId: String) {
        loadUserBookings()
    }
}
