package com.gridee.parking.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.UserRegistration
import com.gridee.parking.data.repository.ParkingRepository
import com.gridee.parking.data.repository.UserRepository
import com.gridee.parking.utils.JwtTokenManager
import kotlinx.coroutines.launch
import java.security.MessageDigest

class RegistrationViewModel : ViewModel() {
    
    private val userRepository = UserRepository()
    private val parkingRepository = ParkingRepository()
    
    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState
    
    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors

    private val _parkingLotNames = MutableLiveData<List<String>>()
    val parkingLotNames: LiveData<List<String>> = _parkingLotNames

    private val _parkingLotLoading = MutableLiveData<Boolean>()
    val parkingLotLoading: LiveData<Boolean> = _parkingLotLoading

    private val _parkingLotError = MutableLiveData<String?>()
    val parkingLotError: LiveData<String?> = _parkingLotError
    
    fun registerUser(
        context: Context,
        name: String,
        email: String,
        phone: String,
        password: String,
        parkingLotName: String
    ) {
        val normalizedParkingLot = parkingLotName.trim().ifBlank { null }
        // Validate input
        val errors = validateInput(name, email, phone, password, normalizedParkingLot)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _registrationState.value = RegistrationState.Loading
        
        viewModelScope.launch {
            try {
                val userRegistration = UserRegistration(
                    name = name.trim(),
                    email = email.trim().lowercase(),
                    phone = phone.trim(),
                    // Send plain password; backend will BCrypt-hash it
                    passwordHash = password,
                    parkingLotName = normalizedParkingLot,
                    vehicleNumbers = emptyList()
                )
                
                val response = userRepository.registerUser(userRegistration)
                
                if (response.isSuccessful) {
                    response.body()?.let { auth ->
                        // Save JWT token and user info
                        val jwtManager = JwtTokenManager(context)
                        jwtManager.saveAuthToken(
                            token = auth.token,
                            userId = auth.id,
                            userName = auth.name,
                            userRole = auth.role
                        )

                        // Build a minimal User object for UI using entered details
                        val registeredUser = User(
                            id = auth.id,
                            name = auth.name,
                            email = email.trim(),
                            phone = phone.trim(),
                            parkingLotName = normalizedParkingLot
                        )
                        _registrationState.value = RegistrationState.Success(registeredUser)
                    } ?: run {
                        _registrationState.value = RegistrationState.Error("Registration successful but empty response")
                    }
                } else {
                    val errorMessage = runCatching { response.errorBody()?.string() }.getOrNull() ?: "Registration failed"
                    _registrationState.value = RegistrationState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("Network error: ${e.message}")
            }
        }
    }
    
    private fun validateInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        parkingLotName: String?
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (name.isBlank()) {
            errors["name"] = "Name is required"
        }
        
        if (email.isBlank()) {
            errors["email"] = "Email is required"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Invalid email format"
        }
        
        if (phone.isBlank()) {
            errors["phone"] = "Phone number is required"
        } else if (phone.length < 10) {
            errors["phone"] = "Phone number must be at least 10 digits"
        }
        
        if (password.isBlank()) {
            errors["password"] = "Password is required"
        } else if (password.length < 6) {
            errors["password"] = "Password must be at least 6 characters"
        }

        return errors
    }

    fun loadParkingLotNames(forceRefresh: Boolean = false) {
        if (!forceRefresh && !_parkingLotNames.value.isNullOrEmpty()) {
            return
        }

        _parkingLotLoading.value = true

        viewModelScope.launch {
            try {
                val response = parkingRepository.getParkingLotNames()
                if (response.isSuccessful) {
                    val names = response.body().orEmpty()
                    _parkingLotNames.value = names
                    _parkingLotError.value = if (names.isEmpty()) "No parking lots available yet" else null
                } else {
                    val error = runCatching { response.errorBody()?.string() }.getOrNull()
                    _parkingLotError.value = error ?: "Unable to load parking lots"
                }
            } catch (e: Exception) {
                _parkingLotError.value = "Unable to load parking lots: ${e.message}"
            } finally {
                _parkingLotLoading.value = false
            }
        }
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }
    
    fun clearErrors() {
        _validationErrors.value = emptyMap()
    }
}

sealed class RegistrationState {
    object Loading : RegistrationState()
    data class Success(val user: User) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}
