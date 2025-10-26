package com.gridee.parking.data.repository

import android.content.Context
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.QrCodeRequest
import com.gridee.parking.data.model.QrValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BookingRepository(private val context: Context) {
    
    private val apiService = ApiClient.apiService
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    
    suspend fun getUserBookings(): Result<List<Booking>> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            println("BookingRepository: Loading bookings for userId: '$userId'")
            if (userId.isNullOrEmpty()) {
                println("BookingRepository: User not logged in")
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            // Try preferred endpoint first
            val response = apiService.getUserBookings(userId)
            println("BookingRepository: Get bookings (all-bookings) response code: ${response.code()}")
            if (response.isSuccessful) {
                val bookings = response.body() ?: emptyList()
                println("BookingRepository: Found ${bookings.size} bookings")
                bookings.forEach { booking ->
                    println("BookingRepository: Booking ID: ${booking.id}, Status: ${booking.status}, Spot: ${booking.spotId}")
                }
                Result.success(bookings)
            } else {
                // Fallback to legacy endpoint if available
                val fallback = apiService.getUserBookingsLegacy(userId)
                println("BookingRepository: Fallback (bookings) response code: ${fallback.code()}")
                if (fallback.isSuccessful) {
                    val bookings = fallback.body() ?: emptyList()
                    println("BookingRepository: Found ${bookings.size} bookings via legacy endpoint")
                    Result.success(bookings)
                } else if (fallback.code() == 404) {
                    println("BookingRepository: No bookings found (404), returning empty list")
                    Result.success(emptyList())
                } else {
                    val errorBody = fallback.errorBody()?.string()
                    println("BookingRepository: Get bookings error: $errorBody")
                    Result.failure(Exception("Failed to load bookings: ${fallback.message()}"))
                }
            }
        } catch (e: Exception) {
            println("BookingRepository: Exception loading bookings: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUserBookingHistory(): Result<List<Booking>> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val response = apiService.getUserBookingHistory(userId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                val fallback = apiService.getUserBookingHistoryLegacy(userId)
                if (fallback.isSuccessful) {
                    Result.success(fallback.body() ?: emptyList())
                } else if (fallback.code() == 404) {
                    println("BookingRepository: No booking history found (404), returning empty list")
                    Result.success(emptyList())
                } else {
                    Result.failure(Exception("Failed to load booking history: ${fallback.message()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun startBooking(
        spotId: String,
        lotId: String,
        checkInTime: Date,
        checkOutTime: Date,
        vehicleNumber: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val checkInTimeStr = dateFormatter.format(checkInTime)
            val checkOutTimeStr = dateFormatter.format(checkOutTime)
            
            println("BookingRepository: Creating booking with userId: $userId")
            println("BookingRepository: spotId: $spotId, lotId: $lotId")
            println("BookingRepository: checkInTime: $checkInTimeStr")
            println("BookingRepository: checkOutTime: $checkOutTimeStr")
            println("BookingRepository: vehicleNumber: $vehicleNumber")
            
            val response = apiService.startBooking(
                userId = userId,
                spotId = spotId,
                lotId = lotId,
                checkInTime = checkInTimeStr,
                checkOutTime = checkOutTimeStr,
                vehicleNumber = vehicleNumber
            )
            
            println("BookingRepository: API response code: ${response.code()}")
            println("BookingRepository: API response message: ${response.message()}")
            
            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    println("BookingRepository: Booking created successfully: ${booking.id}")
                    Result.success(booking)
                } else {
                    println("BookingRepository: Empty response from server")
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                println("BookingRepository: API error body: $errorBody")
                
                // Check if it's a wallet error and try to create wallet
                if (errorBody?.contains("Wallet not found") == true) {
                    println("BookingRepository: Wallet not found, attempting to create wallet...")
                    try {
                        // Try to create wallet by topping up with initial amount
                        val walletResponse = apiService.topUpWallet(userId, com.gridee.parking.data.model.TopUpRequest(100.0))
                        if (walletResponse.isSuccessful) {
                            println("BookingRepository: Wallet created successfully, retrying booking...")
                            // Retry the booking
                            val retryResponse = apiService.startBooking(
                                userId = userId,
                                spotId = spotId,
                                lotId = lotId,
                                checkInTime = checkInTimeStr,
                                checkOutTime = checkOutTimeStr,
                                vehicleNumber = vehicleNumber
                            )
                            if (retryResponse.isSuccessful) {
                                val booking = retryResponse.body()
                                if (booking != null) {
                                    println("BookingRepository: Booking created successfully after wallet creation: ${booking.id}")
                                    return@withContext Result.success(booking)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("BookingRepository: Failed to create wallet: ${e.message}")
                    }
                }
                
                Result.failure(Exception("Failed to create booking: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            println("BookingRepository: Exception occurred: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun confirmBooking(bookingId: String): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val response = apiService.confirmBooking(userId, bookingId)
            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Failed to confirm booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelBooking(bookingId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }
            
            val response = apiService.cancelBooking(userId, bookingId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to cancel booking: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== NEW QR METHODS ==========

    /**
     * Validate QR code for check-in and return penalty info/message
     */
    suspend fun validateCheckInQr(
        bookingId: String,
        qrCode: String
    ): Result<QrValidationResult> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val request = QrCodeRequest(qrCode)
            val response = apiService.validateQrCodeForCheckIn(userId, bookingId, request)

            if (response.isSuccessful) {
                val result = response.body()
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Perform actual check-in
     */
    suspend fun checkIn(
        bookingId: String,
        qrCode: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val request = QrCodeRequest(qrCode)
            val response = apiService.checkInBooking(userId, bookingId, request)

            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Check-in failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate QR code for check-out and return final charges
     */
    suspend fun validateCheckOutQr(
        bookingId: String,
        qrCode: String
    ): Result<QrValidationResult> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val request = QrCodeRequest(qrCode)
            val response = apiService.validateQrCodeForCheckOut(userId, bookingId, request)

            if (response.isSuccessful) {
                val result = response.body()
                if (result != null) {
                    Result.success(result)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Validation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Perform actual check-out
     */
    suspend fun checkOut(
        bookingId: String,
        qrCode: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val request = QrCodeRequest(qrCode)
            val response = apiService.checkOutBooking(userId, bookingId, request)

            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else if (response.code() == 402) {
                // Payment required - insufficient funds
                Result.failure(Exception("Insufficient wallet balance to pay penalties"))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Check-out failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time penalty for active booking
     */
    suspend fun getPenaltyInfo(bookingId: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val response = apiService.getPenaltyInfo(userId, bookingId)

            if (response.isSuccessful) {
                val penalty = response.body()
                if (penalty != null) {
                    Result.success(penalty)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to get penalty info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh booking data
     */
    suspend fun refreshBooking(bookingId: String): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val response = apiService.getBookingById(userId, bookingId)

            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else {
                Result.failure(Exception("Failed to refresh booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extend booking checkout time
     */
    suspend fun extendBooking(
        bookingId: String,
        newCheckOutTime: String
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            if (userId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("User not logged in"))
            }

            val request = mapOf("newCheckOutTime" to newCheckOutTime)
            val response = apiService.extendBooking(userId, bookingId, request)

            if (response.isSuccessful) {
                val booking = response.body()
                if (booking != null) {
                    Result.success(booking)
                } else {
                    Result.failure(Exception("Empty response"))
                }
            } else if (response.code() == 402) {
                Result.failure(Exception("Insufficient wallet balance"))
            } else if (response.code() == 409) {
                Result.failure(Exception("Parking spot not available for extended time"))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(errorBody ?: "Failed to extend booking"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getUserId(): String? {
        // Legacy storage
        val sharedPref = context.getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
        val legacyId = sharedPref.getString("user_id", null)
        if (!legacyId.isNullOrBlank()) return legacyId

        // JWT-based storage fallback
        return try {
            com.gridee.parking.utils.JwtTokenManager(context).getUserId()
        } catch (_: Exception) {
            null
        }
    }
    
    // Legacy methods for backward compatibility
    suspend fun getUserBookings(userId: String): List<Booking>? {
        return try {
            val response = apiService.getUserBookings(userId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getUserBookingHistory(userId: String): List<Booking>? {
        return try {
            val response = apiService.getUserBookingHistory(userId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
