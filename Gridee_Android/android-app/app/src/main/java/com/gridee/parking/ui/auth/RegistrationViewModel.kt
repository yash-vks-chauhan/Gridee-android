package com.gridee.parking.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.AuthResponse
import com.gridee.parking.data.model.UserRegistration
import com.gridee.parking.data.repository.UserRepository
import com.gridee.parking.utils.JwtTokenManager
import kotlinx.coroutines.launch
import java.security.MessageDigest

class RegistrationViewModel : ViewModel() {
    
    private val userRepository = UserRepository()
    
    private val _registrationState = MutableLiveData<RegistrationState>()
    val registrationState: LiveData<RegistrationState> = _registrationState
    
    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors
    
    fun registerUser(
        context: Context,
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        vehicleNumbers: List<String>
    ) {
        // Validate input
        val errors = validateInput(name, email, phone, password, vehicleNumbers)
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
                    vehicleNumbers = vehicleNumbers.filter { it.isNotBlank() }
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
                            vehicleNumbers = vehicleNumbers.filter { it.isNotBlank() }
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
        vehicleNumbers: List<String>
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
        
        // Validate vehicle numbers
        val nonEmptyVehicles = vehicleNumbers.filter { it.isNotBlank() }
        if (nonEmptyVehicles.isEmpty()) {
            errors["vehicle"] = "At least one vehicle number is required"
        } else {
            // Validate vehicle number format (basic Indian vehicle number pattern)
            val vehiclePattern = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,2}[0-9]{4}$")
            val invalidVehicles = nonEmptyVehicles.filter { !vehiclePattern.matches(it.uppercase()) }
            if (invalidVehicles.isNotEmpty()) {
                errors["vehicle"] = "Invalid vehicle number format (e.g., MH12AB1234)"
            }
            
            // Check for duplicates
            if (nonEmptyVehicles.size != nonEmptyVehicles.toSet().size) {
                errors["vehicle"] = "Duplicate vehicle numbers not allowed"
            }
        }
        
        return errors
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
