package com.gridee.parking.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.gridee.parking.data.model.User
import com.gridee.parking.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.security.MessageDigest

class LoginViewModel : ViewModel() {
    
    private val userRepository = UserRepository()
    
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    
    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors
    
    fun loginUser(emailOrPhone: String, password: String) {
        // Validate input
        val errors = validateInput(emailOrPhone, password)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val hashedPassword = hashPassword(password)
                val response = userRepository.loginUser(emailOrPhone.trim(), hashedPassword)
                
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        _loginState.value = LoginState.Error("Login successful but no user data received")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Invalid email/phone or password"
                        404 -> "User not found"
                        else -> response.errorBody()?.string() ?: "Login failed"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }
    
    private fun validateInput(emailOrPhone: String, password: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        if (emailOrPhone.isBlank()) {
            errors["emailPhone"] = "Email or phone number is required"
        }
        
        if (password.isBlank()) {
            errors["password"] = "Password is required"
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
    
    fun handleGoogleSignInSuccess(account: GoogleSignInAccount) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                // Send Google account data to your backend for verification
                val response = userRepository.googleSignIn(
                    idToken = account.idToken ?: "",
                    email = account.email ?: "",
                    name = account.displayName ?: "",
                    profilePicture = account.photoUrl?.toString()
                )
                
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        _loginState.value = LoginState.Error("Sign in successful but no user data received")
                    }
                } else {
                    _loginState.value = LoginState.Error("Google Sign In failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun handleAppleSignInSuccess(authorizationCode: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                // Send Apple authorization code to your backend for verification
                val response = userRepository.appleSignIn(authorizationCode)
                
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        _loginState.value = LoginState.Error("Sign in successful but no user data received")
                    }
                } else {
                    _loginState.value = LoginState.Error("Apple Sign In failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun handleSignInError(message: String) {
        _loginState.value = LoginState.Error(message)
    }
    
    @Deprecated("Use handleGoogleSignInSuccess instead")
    fun signInWithApple() {
        // TODO: Implement Apple Sign In
        _loginState.value = LoginState.Error("Apple Sign In not implemented yet")
    }
    
    @Deprecated("Use handleAppleSignInSuccess instead")
    fun signInWithGoogle() {
        // TODO: Implement Google Sign In
        _loginState.value = LoginState.Error("Google Sign In not implemented yet")
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
