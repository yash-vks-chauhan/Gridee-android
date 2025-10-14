package com.gridee.parking.data.repository

import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.UserRegistration
import retrofit2.Response

class UserRepository {
    
    private val apiService = ApiClient.apiService
    
    suspend fun registerUser(userRegistration: UserRegistration): Response<User> {
        return apiService.registerUser(userRegistration)
    }
    
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
    ): Response<User> {
        val googleData = mapOf(
            "idToken" to idToken,
            "email" to email,
            "name" to name,
            "profilePicture" to (profilePicture ?: ""),
            "provider" to "google"
        )
        return apiService.socialSignIn(googleData)
    }
    
    suspend fun appleSignIn(authorizationCode: String): Response<User> {
        val appleData = mapOf(
            "authorizationCode" to authorizationCode,
            "provider" to "apple"
        )
        return apiService.socialSignIn(appleData)
    }
}
