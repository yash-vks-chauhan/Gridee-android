# Registration Fix - Multiple Issues

## Problems
When clicking the "Register" button in the Android app, users were getting errors:
1. First: `unauthorized, message: full authentication is required`
2. After fix #1: `Password is required.`

## Root Causes

### Issue 1: Endpoint Mismatch
The Android app was calling the **wrong registration endpoint**:
- **Android app was calling:** `/api/users/register`
- **Backend actually has:** `/api/auth/register`

The backend's `SecurityConfig.java` only permits `/api/auth/register` as a public endpoint. When the app tried to call `/api/users/register`, Spring Security blocked the request.

### Issue 2: Password Field Name Mismatch
After fixing the endpoint, the backend was still rejecting the request with "Password is required." because:
- **Android app was sending:** `passwordHash` field
- **Backend expects:** `password` field

The `UserRequestDto.java` in the backend expects a field named `password`, but the Android `UserRegistration` model was sending it as `passwordHash`.

## Solutions

### Fix 1: Correct the Endpoint
Updated two files to use the correct `/api/auth/register` endpoint:

**ApiService.kt**:
```kotlin
@POST("api/auth/register")  // Changed from api/users/register
suspend fun registerUser(@Body user: UserRegistration): Response<AuthResponse>
```

**JwtAuthInterceptor.kt**:
```kotlin
val publicPaths = listOf(
    "/api/auth/login",
    "/api/auth/register",  // Changed from /api/users/register
    "/api/auth/google",
    ...
)
```

### Fix 2: Map Password Field Correctly
Added `@SerializedName` annotation to map the field name during JSON serialization:

**User.kt**:
```kotlin
data class UserRegistration(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("password")  // Backend expects "password", not "passwordHash"
    val passwordHash: String,
    
    @SerializedName("parkingLotName")
    val parkingLotName: String?,
    
    @SerializedName("vehicleNumbers")
    val vehicleNumbers: List<String> = emptyList()
)
```

## Files Modified
1. `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/api/ApiService.kt`
2. `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/api/JwtAuthInterceptor.kt`
3. `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/model/User.kt`
4. `/Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/auth/RegistrationViewModel.kt` (added logging)

## Testing
To test the fix:
1. Rebuild the Android app
2. Navigate to the registration screen
3. Fill in all required fields (name, email, phone, password, parking lot)
4. Click the "Register" button
5. Registration should now succeed without errors ✅

## Backend Reference
The backend registration endpoint is defined in:
- `/src/main/java/com/parking/app/controller/AuthController.java` (line 54-63)
- Path: `POST /api/auth/register`
- Controller: `AuthController`
- DTO: `UserRequestDto` expects fields: name, email, phone, **password**, vehicleNumbers, parkingLotName
