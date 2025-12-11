# JWT Authentication Implementation Guide

## Overview
This document explains the JWT-based authentication implementation for the Gridee Android app, following the backend's `/api/auth/login` endpoint.

---

## 📁 Files Created/Modified

### New Files:
1. **`AuthResponse.kt`** - Model for JWT authentication response
2. **`JwtTokenManager.kt`** - Utility for managing JWT tokens
3. **`JwtAuthInterceptor.kt`** - OkHttp interceptor for automatic JWT handling
4. **`JwtLoginViewModel.kt`** - ViewModel for JWT-based authentication

### Modified Files:
1. **`ApiService.kt`** - Added `authLogin()` endpoint
2. **`UserRepository.kt`** - Added `authLogin()` method

---

## 🔐 How JWT Authentication Works

### Backend Endpoint
```
POST /api/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "hashed_password_sha256"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": "user_id",
  "name": "John Doe",
  "role": "USER"
}
```

---

## 📋 Implementation Details

### 1. Models

#### AuthResponse.kt
```kotlin
data class AuthResponse(
    val token: String,    // JWT token
    val id: String,       // User ID
    val name: String,     // User name
    val role: String      // User role (USER, ADMIN, etc.)
)

data class AuthRequest(
    val email: String,
    val password: String  // SHA-256 hashed
)
```

### 2. API Service

#### ApiService.kt
```kotlin
@POST("api/auth/login")
suspend fun authLogin(@Body request: AuthRequest): Response<AuthResponse>
```

### 3. Repository

#### UserRepository.kt
```kotlin
suspend fun authLogin(email: String, password: String): Response<AuthResponse> {
    val request = AuthRequest(email = email, password = password)
    return apiService.authLogin(request)
}
```

### 4. Token Management

#### JwtTokenManager.kt
Handles JWT token storage, retrieval, and validation:

**Key Methods:**
- `saveAuthToken(token, userId, userName, userRole)` - Save JWT token and user info
- `getAuthToken()` - Get stored JWT token (returns null if expired)
- `getBearerToken()` - Get token in "Bearer {token}" format
- `isAuthenticated()` - Check if user has valid token
- `clearAuthToken()` - Logout user (clear all auth data)
- `getUserId()`, `getUserName()`, `getUserRole()` - Get stored user info

**Token Expiry:**
- Tokens expire after 24 hours
- Expired tokens are automatically cleared

### 5. ViewModel

#### JwtLoginViewModel.kt
```kotlin
class JwtLoginViewModel : ViewModel() {
    fun loginWithJwt(context: Context, emailOrPhone: String, password: String)
    fun checkAuthentication(context: Context): Boolean
    fun logout(context: Context)
}
```

**States:**
- `JwtAuthState.Idle` - Initial state
- `JwtAuthState.Loading` - Login in progress
- `JwtAuthState.Success(authResponse)` - Login successful
- `JwtAuthState.Error(message)` - Login failed
- `JwtAuthState.LoggedOut` - User logged out

---

## 🚀 Usage Examples

### Example 1: Login with JWT Authentication

```kotlin
class LoginActivity : AppCompatActivity() {
    private val jwtViewModel: JwtLoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe authentication state
        jwtViewModel.authState.observe(this) { state ->
            when (state) {
                is JwtAuthState.Loading -> {
                    // Show loading indicator
                    showLoading(true)
                }
                is JwtAuthState.Success -> {
                    // Login successful
                    showLoading(false)
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to main screen
                    navigateToMainScreen()
                }
                is JwtAuthState.Error -> {
                    // Login failed
                    showLoading(false)
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        // Login button click
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            
            jwtViewModel.loginWithJwt(this, email, password)
        }
    }
}
```

### Example 2: Check Authentication Status

```kotlin
class MainActivity : AppCompatActivity() {
    private val jwtViewModel: JwtLoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is already logged in
        if (!jwtViewModel.checkAuthentication(this)) {
            // User not authenticated, redirect to login
            navigateToLogin()
            return
        }
        
        // User is authenticated, continue
        loadUserData()
    }
}
```

### Example 3: Get Current User Info

```kotlin
class ProfileFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val jwtManager = JwtTokenManager(requireContext())
        
        // Get user information
        val userId = jwtManager.getUserId()
        val userName = jwtManager.getUserName()
        val userRole = jwtManager.getUserRole()
        
        // Display user info
        textUserName.text = userName
        textUserId.text = "ID: $userId"
        textUserRole.text = "Role: $userRole"
    }
}
```

### Example 4: Logout

```kotlin
class ProfileActivity : AppCompatActivity() {
    private val jwtViewModel: JwtLoginViewModel by viewModels()
    
    private fun handleLogout() {
        jwtViewModel.logout(this)
        
        // Clear any cached data
        clearUserCache()
        
        // Navigate to login
        navigateToLogin()
        finish()
    }
}
```

### Example 5: Making Authenticated API Calls

#### Option 1: Using JwtAuthInterceptor (Automatic)

Add the interceptor to your OkHttpClient:

```kotlin
object ApiClient {
    private lateinit var applicationContext: Context
    
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(JwtAuthInterceptor(applicationContext))
        .addInterceptor(loggingInterceptor)
        .build()
    
    // ... rest of your ApiClient setup
}
```

Then in your Application class:
```kotlin
class GrideeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
```

#### Option 2: Manual Token Addition

```kotlin
class BookingRepository {
    suspend fun getUserBookings(userId: String): List<Booking> {
        val jwtManager = JwtTokenManager(context)
        val token = jwtManager.getBearerToken()
        
        // If using manual header addition
        val response = apiService.getUserBookings(userId, token)
        return response.body() ?: emptyList()
    }
}
```

---

## 🔄 Migration from Old Login to JWT Login

### Before (Old Login - `/api/users/login`)
```kotlin
val response = userRepository.loginUser(email, password)
if (response.isSuccessful) {
    val user = response.body()
    // No JWT token returned
    saveUser(user)
}
```

### After (JWT Login - `/api/auth/login`)
```kotlin
val response = userRepository.authLogin(email, password)
if (response.isSuccessful) {
    val authResponse = response.body()
    // JWT token included in response
    val jwtManager = JwtTokenManager(context)
    jwtManager.saveAuthToken(
        token = authResponse.token,
        userId = authResponse.id,
        userName = authResponse.name,
        userRole = authResponse.role
    )
}
```

---

## 🔧 Configuration

### Token Expiry Time
Default: 24 hours

To change, modify `JwtTokenManager.kt`:
```kotlin
companion object {
    // Token expiry time in milliseconds
    private const val TOKEN_EXPIRY_TIME = 24 * 60 * 60 * 1000L  // 24 hours
}
```

### Protected Endpoints
Endpoints that require JWT authentication are configured in `JwtAuthInterceptor.kt`:

```kotlin
private fun shouldAddJwtToken(path: String): Boolean {
    // Public paths (no JWT required)
    val publicPaths = listOf(
        "/api/auth/login",
        "/api/users/register",
        "/api/users/login",
        "/api/users/social-signin",
        "/api/otp/generate",
        "/api/otp/validate"
    )
    
    return !publicPaths.any { path.contains(it) }
}
```

---

## ✅ Testing JWT Authentication

### Test 1: Login with Valid Credentials
```kotlin
@Test
fun testLoginWithJwt() = runBlocking {
    val repository = UserRepository()
    val response = repository.authLogin("test@example.com", "hashed_password")
    
    assertTrue(response.isSuccessful)
    assertNotNull(response.body()?.token)
    assertEquals("test@example.com", response.body()?.email)
}
```

### Test 2: Token Storage and Retrieval
```kotlin
@Test
fun testTokenStorage() {
    val jwtManager = JwtTokenManager(context)
    
    jwtManager.saveAuthToken(
        token = "test_token_123",
        userId = "user_1",
        userName = "Test User",
        userRole = "USER"
    )
    
    assertEquals("test_token_123", jwtManager.getAuthToken())
    assertEquals("Bearer test_token_123", jwtManager.getBearerToken())
    assertTrue(jwtManager.isAuthenticated())
}
```

### Test 3: Token Expiry
```kotlin
@Test
fun testTokenExpiry() {
    val jwtManager = JwtTokenManager(context)
    
    // Save token with timestamp in past
    // (Modify JwtTokenManager to allow this for testing)
    
    assertNull(jwtManager.getAuthToken()) // Should return null for expired token
    assertFalse(jwtManager.isAuthenticated())
}
```

---

## 🐛 Debugging

### Enable Logging
In `ApiClient.kt`:
```kotlin
private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY  // Full request/response logging
}
```

### Check Token Status
```kotlin
val jwtManager = JwtTokenManager(context)
val authData = jwtManager.getAuthData()
Log.d("JwtAuth", "Auth Data: $authData")
```

### Common Issues

1. **Token not being sent with requests**
   - Ensure `JwtAuthInterceptor` is added to OkHttpClient
   - Check if endpoint is in public paths list

2. **Token expired**
   - Check timestamp when token was saved
   - Verify TOKEN_EXPIRY_TIME configuration

3. **401 Unauthorized response**
   - Verify token is valid using backend tools
   - Check if token format is correct ("Bearer {token}")

---

## 📊 Comparison: Old Login vs JWT Login

| Feature | Old Login (`/api/users/login`) | JWT Login (`/api/auth/login`) |
|---------|-------------------------------|------------------------------|
| Endpoint | `POST /api/users/login` | `POST /api/auth/login` |
| Response | User object | AuthResponse with JWT token |
| Token Management | No built-in token | JWT token for authentication |
| Security | Basic authentication | Enhanced security with JWT |
| Stateless | No | Yes |
| Auto-renewal | No | Can be implemented |
| Role-based access | Limited | Full support |

---

## 🎯 Next Steps

1. **Integrate JWT login in your existing login screens**
   - Replace `LoginViewModel` with `JwtLoginViewModel`
   - Update UI to handle JWT authentication states

2. **Add JwtAuthInterceptor to ApiClient**
   - Initialize in Application class
   - Automatic JWT token handling for all requests

3. **Implement token refresh mechanism** (Future)
   - Add refresh token support
   - Auto-renew tokens before expiry

4. **Update all authenticated API calls**
   - Ensure protected endpoints use JWT token
   - Remove manual token handling

5. **Test thoroughly**
   - Login flow
   - Token storage and retrieval
   - Authenticated API calls
   - Logout flow

---

## 📚 References

- Backend AuthController: `/src/main/java/com/parking/app/controller/AuthController.java`
- JWT Utility: `/src/main/java/com/parking/app/config/JwtUtil.java`
- Backend Comparison Doc: `BACKEND_FRONTEND_COMPARISON.md`

---

**Last Updated**: October 14, 2025
**Status**: ✅ IMPLEMENTED
**Priority**: HIGH
**Feature**: JWT-based authentication with token management
