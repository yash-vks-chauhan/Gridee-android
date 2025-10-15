package com.gridee.parking.data.api

import com.gridee.parking.data.model.AuthRequest
import com.gridee.parking.data.model.AuthResponse
import com.gridee.parking.data.model.Booking
import com.gridee.parking.data.model.ParkingLot
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.UserRegistration
import com.gridee.parking.data.model.WalletDetails
import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.data.model.PaymentInitiateRequest
import com.gridee.parking.data.model.PaymentInitiateResponse
import com.gridee.parking.data.model.PaymentCallbackRequest
import com.gridee.parking.data.model.PaymentCallbackResponse
import com.gridee.parking.data.model.TopUpRequest
import com.gridee.parking.data.model.TopUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    // ========== Authentication Endpoints ==========
    
    @POST("api/auth/login")
    suspend fun authLogin(@Body request: AuthRequest): Response<AuthResponse>
    
    // ========== User Management Endpoints ==========
    
    @POST("api/users/register")
    suspend fun registerUser(@Body user: UserRegistration): Response<AuthResponse>
    
    @POST("api/users/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<User>
    
    @POST("api/users/social-signin")
    suspend fun socialSignIn(@Body credentials: Map<String, String>): Response<User>

    // OAuth2 user info
    @GET("api/oauth2/user")
    suspend fun getOAuth2User(): Response<Map<String, Any>>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<User>
    
    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") userId: String, @Body user: User): Response<User>
    
    // Parking lots and spots endpoints
    @GET("api/parking-lots")
    suspend fun getParkingLots(): Response<List<ParkingLot>>
    
    @GET("api/parking-spots")
    suspend fun getParkingSpots(): Response<List<ParkingSpot>>
    
    @GET("api/parking-lots/{lotId}/spots")
    suspend fun getParkingSpotsByLot(@Path("lotId") lotId: String): Response<List<ParkingSpot>>
    
    // Preferred: backend exposes "all-bookings" for list/history
    @GET("api/users/{userId}/all-bookings")
    suspend fun getUserBookings(@Path("userId") userId: String): Response<List<Booking>>
    
    @GET("api/users/{userId}/all-bookings/history")
    suspend fun getUserBookingHistory(@Path("userId") userId: String): Response<List<Booking>>

    // Legacy fallback endpoints (older backend versions)
    @GET("api/users/{userId}/bookings")
    suspend fun getUserBookingsLegacy(@Path("userId") userId: String): Response<List<Booking>>
    
    @GET("api/users/{userId}/bookings/history")
    suspend fun getUserBookingHistoryLegacy(@Path("userId") userId: String): Response<List<Booking>>
    
    // Booking creation endpoints
    @POST("api/users/{userId}/bookings/start")
    suspend fun startBooking(
        @Path("userId") userId: String,
        @Query("spotId") spotId: String,
        @Query("lotId") lotId: String,
        @Query("checkInTime") checkInTime: String,
        @Query("checkOutTime") checkOutTime: String,
        @Query("vehicleNumber") vehicleNumber: String
    ): Response<Booking>
    
    @POST("api/users/{userId}/bookings/{bookingId}/confirm")
    suspend fun confirmBooking(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String
    ): Response<Booking>
    
    @POST("api/users/{userId}/bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("userId") userId: String,
        @Path("bookingId") bookingId: String
    ): Response<Void>
    
    // Wallet endpoints
    @GET("api/users/{userId}/wallet")
    suspend fun getWalletDetails(@Path("userId") userId: String): Response<WalletDetails>
    
    @GET("api/users/{userId}/wallet/transactions")
    suspend fun getWalletTransactions(@Path("userId") userId: String): Response<List<WalletTransaction>>
    
    @POST("api/users/{userId}/wallet/topup")
    suspend fun topUpWallet(
        @Path("userId") userId: String,
        @Body request: TopUpRequest
    ): Response<TopUpResponse>

    // Payments (Razorpay)
    @POST("api/payments/initiate")
    suspend fun initiatePayment(@Body request: PaymentInitiateRequest): Response<PaymentInitiateResponse>

    @POST("api/payments/callback")
    suspend fun paymentCallback(@Body payload: PaymentCallbackRequest): Response<PaymentCallbackResponse>
    
    // OTP endpoints
    @POST("api/otp/generate")
    suspend fun generateOtp(@Query("key") phoneNumber: String): Response<String>
    
    @POST("api/otp/validate")
    suspend fun validateOtp(
        @Query("key") phoneNumber: String,
        @Query("otp") otp: String
    ): Response<Boolean>
}
