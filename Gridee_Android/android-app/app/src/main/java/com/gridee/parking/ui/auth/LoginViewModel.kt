package com.gridee.parking.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.gridee.parking.data.model.User
import com.gridee.parking.data.model.AuthResponse
import com.gridee.parking.data.model.ErrorResponse
import com.gridee.parking.data.repository.UserRepository
import com.gridee.parking.utils.JwtTokenManager
import kotlinx.coroutines.launch
import java.security.MessageDigest
import com.google.gson.Gson

import android.content.Context

class LoginViewModel : ViewModel() {
    
    private val userRepository = UserRepository()
    
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    
    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors
    
    fun loginUser(context: Context, emailOrPhone: String, password: String) {
        // Validate input
        val errors = validateInput(emailOrPhone, password)
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                // Prefer JWT-based login to avoid parsing mismatches
                val normalized = emailOrPhone.trim().let { if (it.contains("@")) it.lowercase() else it }
                // Send plain password; backend uses BCrypt on the server side
                val jwtResponse = userRepository.authLogin(normalized, password)

                if (jwtResponse.isSuccessful) {
                    jwtResponse.body()?.let { auth ->
                        // Persist JWT for subsequent authenticated calls
                        val jwtManager = JwtTokenManager(context)
                        jwtManager.saveAuthToken(
                            token = auth.token,
                            userId = auth.id,
                            userName = auth.name,
                            userRole = auth.role
                        )
                        // Build a User object from the response
                        val user = User(
                            id = auth.id,
                            name = auth.name,
                            email = auth.email,
                            phone = auth.phone,
                            vehicleNumbers = auth.user.vehicleNumbers ?: emptyList(),
                            role = auth.role
                        )
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        _loginState.value = LoginState.Error("Login successful but no token received")
                    }
                } else {
                    // Parse error response from backend
                    val errorMessage = try {
                        val errorBody = jwtResponse.errorBody()?.string()
                        if (errorBody != null) {
                            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message ?: getDefaultErrorMessage(jwtResponse.code())
                        } else {
                            getDefaultErrorMessage(jwtResponse.code())
                        }
                    } catch (e: Exception) {
                        getDefaultErrorMessage(jwtResponse.code())
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }
    
    private fun getDefaultErrorMessage(code: Int): String {
        return when (code) {
            400, 401, 404 -> "Invalid email/phone or password"
            500 -> "Server error. Please try again later"
            else -> "Login failed. Please check your connection"
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
    
    fun handleGoogleSignInSuccess(context: Context, account: GoogleSignInAccount) {
        android.util.Log.d("LoginViewModel", ">>> handleGoogleSignInSuccess called")
        android.util.Log.d("LoginViewModel", "  - Email: ${account.email}")
        android.util.Log.d("LoginViewModel", "  - Name: ${account.displayName}")
        android.util.Log.d("LoginViewModel", "  - ID Token length: ${account.idToken?.length}")
        
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                android.util.Log.d("LoginViewModel", "Sending Google sign-in request to backend...")
                android.util.Log.d("LoginViewModel", "  - Endpoint: POST /api/users/social-signin")
                android.util.Log.d("LoginViewModel", "  - idToken: ${account.idToken?.take(30)}...")
                android.util.Log.d("LoginViewModel", "  - email: ${account.email}")
                android.util.Log.d("LoginViewModel", "  - name: ${account.displayName}")
                android.util.Log.d("LoginViewModel", "  - profilePicture: ${account.photoUrl}")
                
                // Send Google account data to your backend for verification
                val response = userRepository.googleSignIn(
                    idToken = account.idToken ?: "",
                    email = account.email ?: "",
                    name = account.displayName ?: "",
                    profilePicture = account.photoUrl?.toString()
                )
                
                android.util.Log.d("LoginViewModel", "Backend response received:")
                android.util.Log.d("LoginViewModel", "  - HTTP Code: ${response.code()}")
                android.util.Log.d("LoginViewModel", "  - is Successful: ${response.isSuccessful}")
                android.util.Log.d("LoginViewModel", "  - Message: ${response.message()}")
                
                if (response.isSuccessful) {
                    android.util.Log.d("LoginViewModel", "✅ Google sign-in backend SUCCESS")
                    response.body()?.let { auth ->
                        android.util.Log.d("LoginViewModel", "Auth response body:")
                        android.util.Log.d("LoginViewModel", "  - token: ${auth.token.take(30)}...")
                        android.util.Log.d("LoginViewModel", "  - userId: ${auth.id}")
                        android.util.Log.d("LoginViewModel", "  - userName: ${auth.name}")
                        android.util.Log.d("LoginViewModel", "  - userRole: ${auth.role}")
                        
                        // Save JWT token and user info
                        val jwtManager = JwtTokenManager(context)
                        jwtManager.saveAuthToken(
                            token = auth.token,
                            userId = auth.id,
                            userName = auth.name,
                            userRole = auth.role
                        )
                        android.util.Log.d("LoginViewModel", "JWT token saved to preferences")
                        
                        // Build a User object from the response
                        val user = User(
                            id = auth.id,
                            name = auth.name,
                            email = auth.email,
                            phone = auth.phone,
                            vehicleNumbers = auth.user.vehicleNumbers ?: emptyList(),
                            role = auth.role
                        )
                        android.util.Log.d("LoginViewModel", "User object created, setting Success state")
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        android.util.Log.e("LoginViewModel", "❌ Response body is NULL")
                        _loginState.value = LoginState.Error("Sign in successful but no token received")
                    }
                } else {
                    android.util.Log.e("LoginViewModel", "❌ Google sign-in backend FAILED")
                    
                    // Parse error response from backend
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("LoginViewModel", "Error body: $errorBody")
                    
                    val errorMessage = try {
                        if (errorBody != null) {
                            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                            android.util.Log.e("LoginViewModel", "Parsed error: ${errorResponse.message}")
                            errorResponse.message ?: "Google Sign In failed. Please try again"
                        } else {
                            "Google Sign In failed. Please try again"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("LoginViewModel", "Failed to parse error: ${e.message}")
                        "Google Sign In failed. Please try again"
                    }
                    android.util.Log.e("LoginViewModel", "Final error message: $errorMessage")
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "❌ EXCEPTION during Google sign-in", e)
                android.util.Log.e("LoginViewModel", "  - Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("LoginViewModel", "  - Message: ${e.message}")
                android.util.Log.e("LoginViewModel", "  - Stack trace: ", e)
                _loginState.value = LoginState.Error("Network error: ${e.message}")
            }
        }
    }
    
    fun handleAppleSignInSuccess(context: Context, authorizationCode: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                // Send Apple authorization code to your backend for verification
                val response = userRepository.appleSignIn(authorizationCode)
                
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
                        
                        // Build a User object from the response
                        val user = User(
                            id = auth.id,
                            name = auth.name,
                            email = auth.email,
                            phone = auth.phone,
                            vehicleNumbers = auth.user.vehicleNumbers ?: emptyList(),
                            role = auth.role
                        )
                        _loginState.value = LoginState.Success(user)
                    } ?: run {
                        _loginState.value = LoginState.Error("Sign in successful but no user data received")
                    }
                } else {
                    // Parse error response from backend
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.message ?: "Apple Sign In failed. Please try again"
                        } else {
                            "Apple Sign In failed. Please try again"
                        }
                    } catch (e: Exception) {
                        "Apple Sign In failed. Please try again"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
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
