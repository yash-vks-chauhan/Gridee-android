package com.gridee.parking.data.repository

import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.AuthRequest
import com.gridee.parking.data.model.AuthResponse
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.UserRegistration
import retrofit2.Response

class UserRepository {
    
    private val apiService = ApiClient.apiService
    
    suspend fun registerUser(userRegistration: UserRegistration): Response<AuthResponse> {
        return apiService.registerUser(userRegistration)
    }
    
    /**
     * JWT-based authentication using /api/auth/login endpoint
     * Returns AuthResponse with JWT token and user info
     */
    suspend fun authLogin(email: String, password: String): Response<AuthResponse> {
        val request = AuthRequest(email = email, password = password)
        return apiService.authLogin(request)
    }
    
    /**
     * Legacy login using /api/users/login endpoint
     * Returns User object without JWT token
     */
    suspend fun loginUser(email: String, password: String): Response<User> {
        val credentials = mapOf(
            "email" to email,
            "password" to password
        )
        return apiService.loginUser(credentials)
    }
    
    suspend fun getUserById(userId: String): User? {
        return try {
            val response = apiService.getUserById(userId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateUser(user: User): Boolean {
        return try {
            println("UserRepository: Updating user ${user.id}")
            println("UserRepository: User data - name: ${user.name}, email: ${user.email}, phone: ${user.phone}")
            println("UserRepository: Vehicle numbers: ${user.vehicleNumbers}")
            println("UserRepository: firstUser: ${user.firstUser}, walletCoins: ${user.walletCoins}")
            
            val response = apiService.updateUser(user.id ?: "", user)
            println("UserRepository: Update response - success: ${response.isSuccessful}, code: ${response.code()}")
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                println("UserRepository: Update failed - error body: $errorBody")
            } else {
                println("UserRepository: Update successful - response: ${response.body()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            println("UserRepository: Update exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    suspend fun googleSignIn(
        idToken: String,
        email: String,
        name: String,
        profilePicture: String?
    ): Response<AuthResponse> {
        val googleData = mapOf(
            "idToken" to idToken,
            "email" to email,
            "name" to name,
            "profilePicture" to (profilePicture ?: ""),
            "provider" to "google"
        )
        return apiService.socialSignIn(googleData)
    }
    
    suspend fun appleSignIn(authorizationCode: String): Response<AuthResponse> {
        val appleData = mapOf(
            "authorizationCode" to authorizationCode,
            "provider" to "apple"
        )
        return apiService.socialSignIn(appleData)
    }

    /**
     * Fetch current authenticated user info from OAuth2 (or Basic) context
     */
    suspend fun getOAuth2User(): Response<Map<String, Any>> {
        return apiService.getOAuth2User()
    }
}
