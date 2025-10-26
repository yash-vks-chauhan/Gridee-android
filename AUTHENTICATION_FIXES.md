# Authentication Fixes - Implementation Guide

## 🚀 Quick Start: Critical Fixes

### Fix 1: Frontend - Update Registration Response Type

**File**: `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/api/ApiService.kt`

**Change Line 34** from:
```kotlin
@POST("api/users/register")
suspend fun registerUser(@Body user: UserRegistration): Response<User>
```

**To**:
```kotlin
@POST("api/users/register")
suspend fun registerUser(@Body user: UserRegistration): Response<AuthResponse>
```

---

### Fix 2: Backend - Implement Social Sign-In

**File**: `src/main/java/com/parking/app/controller/UserController.java`

**Add this method to UserController class**:

```java
@PostMapping("/social-signin")
public ResponseEntity<?> socialSignIn(@RequestBody Map<String, String> credentials) {
    try {
        String provider = credentials.get("provider");
        String email = credentials.get("email");
        String name = credentials.get("name");
        
        // Validate required fields
        if (provider == null || email == null || name == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Missing required fields: provider, email, name"));
        }
        
        // Check if user exists by email
        Users user = userService.getUserByEmail(email);
        
        if (user == null) {
            // Create new user for social sign-in
            user = new Users();
            user.setName(name);
            user.setEmail(email.trim().toLowerCase());
            user.setRole(Users.Role.USER);
            user.setWalletCoins(0);
            user.setFirstUser(true);
            user.setCreatedAt(new java.util.Date());
            
            // Set a special password hash for social sign-in users
            // This prevents them from logging in with password
            user.setPasswordHash("SOCIAL_SIGNIN_" + provider.toUpperCase() + "_" + System.currentTimeMillis());
            
            // For Google Sign-In, we can store additional data
            if ("google".equalsIgnoreCase(provider)) {
                String idToken = credentials.get("idToken");
                String profilePicture = credentials.get("profilePicture");
                // TODO: Verify Google ID token if needed
                // TODO: Store profile picture URL in user profile
            } 
            // For Apple Sign-In
            else if ("apple".equalsIgnoreCase(provider)) {
                String authCode = credentials.get("authorizationCode");
                // TODO: Verify Apple authorization code if needed
            }
            
            // Save the new social user
            user = userService.createSocialUser(user);
        } else {
            // User exists, just log them in
            // Optionally update profile picture or other social data
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        
        // Return response with token
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("token", token);
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("role", user.getRole().name());
        
        return ResponseEntity.ok(response);
        
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Social sign-in failed: " + e.getMessage()));
    }
}
```

---

### Fix 3: Backend - Add UserService Methods

**File**: `src/main/java/com/parking/app/service/UserService.java`

**Add these methods to UserService class**:

```java
/**
 * Create a new user from social sign-in (Google, Apple, etc.)
 * If user already exists by email, return existing user
 */
public Users createSocialUser(Users user) {
    if (user == null) {
        throw new IllegalArgumentException("User data is required");
    }
    
    if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
        throw new IllegalArgumentException("Email is required for social sign-in");
    }
    
    // Check if user already exists by email
    Optional<Users> existingUser = userRepository.findByEmail(user.getEmail());
    if (existingUser.isPresent()) {
        return existingUser.get();
    }
    
    // Validate email format
    if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
        throw new IllegalArgumentException("Invalid email format");
    }
    
    // Set defaults if not set
    if (user.getRole() == null) {
        user.setRole(Users.Role.USER);
    }
    if (user.getCreatedAt() == null) {
        user.setCreatedAt(new java.util.Date());
    }
    if (user.getWalletCoins() == 0) {
        user.setWalletCoins(0);
    }
    
    // Save and return
    return userRepository.save(user);
}

/**
 * Get user by email
 */
public Users getUserByEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
        return null;
    }
    return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
}

/**
 * Get user by email or phone
 */
public Users getUserByEmailOrPhone(String emailOrPhone) {
    if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
        return null;
    }
    
    String normalized = emailOrPhone.trim().toLowerCase();
    
    // Try email first
    Users user = userRepository.findByEmail(normalized).orElse(null);
    
    // If not found and looks like phone, try phone
    if (user == null && PHONE_PATTERN.matcher(emailOrPhone.trim()).matches()) {
        user = userRepository.findByPhone(emailOrPhone.trim()).orElse(null);
    }
    
    return user;
}
```

---

## 🔧 Optional But Recommended Fixes

### Fix 4: Backend - Implement Forgot Password Flow

**File**: `src/main/java/com/parking/app/controller/UserController.java`

**Add these endpoints**:

```java
/**
 * Request password reset - sends OTP to user's email/phone
 * POST /api/users/forgot-password
 */
@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
    try {
        String emailOrPhone = request.get("emailOrPhone");
        
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Email or phone number is required"));
        }
        
        // Check if user exists
        Users user = userService.getUserByEmailOrPhone(emailOrPhone);
        if (user == null) {
            // For security, don't reveal if user exists or not
            return ResponseEntity.ok(Map.of(
                "message", "If this account exists, an OTP has been sent",
                "success", true
            ));
        }
        
        // Generate OTP
        String otp = otpService.generateOtp(emailOrPhone);
        
        // TODO: Send OTP via email or SMS service
        // For now, just log it (REMOVE IN PRODUCTION!)
        System.out.println("Password Reset OTP for " + emailOrPhone + ": " + otp);
        
        return ResponseEntity.ok(Map.of(
            "message", "OTP sent successfully",
            "success", true,
            "otpKey", emailOrPhone // Frontend needs this to verify OTP
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to process password reset request"));
    }
}

/**
 * Reset password with OTP verification
 * POST /api/users/reset-password
 */
@PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
    try {
        String emailOrPhone = request.get("emailOrPhone");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        
        // Validate input
        if (emailOrPhone == null || otp == null || newPassword == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Missing required fields"));
        }
        
        if (newPassword.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Password must be at least 6 characters"));
        }
        
        // Validate OTP
        if (!otpService.validateOtp(emailOrPhone, otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid or expired OTP"));
        }
        
        // Get user
        Users user = userService.getUserByEmailOrPhone(emailOrPhone);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        }
        
        // Update password (will be hashed in service)
        Users updateData = new Users();
        updateData.setPasswordHash(newPassword);
        userService.updateUser(user.getId(), updateData);
        
        return ResponseEntity.ok(Map.of(
            "message", "Password reset successfully",
            "success", true
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to reset password"));
    }
}
```

**Don't forget to add OtpService autowiring** at the top of UserController:
```java
@Autowired
private OtpService otpService;
```

---

### Fix 5: Backend - Add Vehicle Number Validation

**File**: `src/main/java/com/parking/app/service/UserService.java`

**Add this pattern constant** (near the top with other patterns):
```java
// Indian vehicle number pattern: XX00XX0000
private static final Pattern VEHICLE_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,2}[0-9]{4}$");
```

**Update the `addUserVehicles` method**:
```java
public Users addUserVehicles(String userId, List<String> vehicleNumbers) {
    Users existingUser = userRepository.findById(userId).orElse(null);
    if (existingUser == null) return null;
    
    if (vehicleNumbers == null || vehicleNumbers.isEmpty()) {
        throw new IllegalArgumentException("Vehicle numbers list cannot be empty.");
    }
    
    // Validate all vehicle numbers first
    for (String v : vehicleNumbers) {
        if (v != null && !v.trim().isEmpty()) {
            String normalized = v.trim().toUpperCase();
            if (!VEHICLE_PATTERN.matcher(normalized).matches()) {
                throw new IllegalArgumentException(
                    "Invalid vehicle number format: " + v + 
                    " (Expected format: MH12AB1234)"
                );
            }
        }
    }
    
    List<String> currentVehicles = existingUser.getVehicleNumbers();
    if (currentVehicles == null) {
        currentVehicles = new java.util.ArrayList<>();
    }
    
    for (String v : vehicleNumbers) {
        if (v != null && !v.trim().isEmpty()) {
            String normalized = v.trim().toUpperCase();
            if (!currentVehicles.contains(normalized)) {
                currentVehicles.add(normalized);
            }
        }
    }
    
    existingUser.setVehicleNumbers(currentVehicles);
    return userRepository.save(existingUser);
}
```

---

### Fix 6: Frontend - Implement Forgot Password UI

**File**: `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/auth/LoginActivity.kt`

**Update the forgot password click handler**:
```kotlin
binding.tvForgotPassword.setOnClickListener {
    // Navigate to forgot password activity
    startActivity(Intent(this, ForgotPasswordActivity::class.java))
}
```

**Then create** `ForgotPasswordActivity.kt` (new file):
```kotlin
package com.gridee.parking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gridee.parking.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.btnSendOtp.setOnClickListener {
            val emailOrPhone = binding.etEmailPhone.text.toString()
            viewModel.requestPasswordReset(emailOrPhone)
        }
        
        binding.btnVerifyAndReset.setOnClickListener {
            val emailOrPhone = binding.etEmailPhone.text.toString()
            val otp = binding.etOtp.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            viewModel.resetPassword(emailOrPhone, otp, newPassword)
        }
        
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
    
    private fun observeViewModel() {
        viewModel.resetState.observe(this) { state ->
            when (state) {
                is ForgotPasswordState.Loading -> showLoading(true)
                is ForgotPasswordState.OtpSent -> {
                    showLoading(false)
                    Toast.makeText(this, "OTP sent!", Toast.LENGTH_SHORT).show()
                    showResetSection()
                }
                is ForgotPasswordState.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Password reset successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is ForgotPasswordState.Error -> {
                    showLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showResetSection() {
        binding.llOtpSection.visibility = View.VISIBLE
        binding.btnSendOtp.visibility = View.GONE
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}

sealed class ForgotPasswordState {
    object Loading : ForgotPasswordState()
    object OtpSent : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}
```

---

## 🎯 Testing Checklist

After implementing fixes, test these scenarios:

### Registration Flow:
- [ ] Register with email, phone, password, vehicles
- [ ] Verify JWT token is returned and stored
- [ ] Verify user is logged in automatically after registration
- [ ] Try registering with existing email (should fail)
- [ ] Try registering with invalid vehicle number (should fail)

### Login Flow:
- [ ] Login with email + password
- [ ] Login with phone + password
- [ ] Try wrong password (should fail)
- [ ] Verify JWT token is stored
- [ ] Verify token is sent in subsequent API calls

### Social Sign-In:
- [ ] Sign in with Google
- [ ] Sign in with Apple
- [ ] Verify new user is created on first sign-in
- [ ] Verify existing user is logged in on subsequent sign-ins
- [ ] Verify JWT token is returned and stored

### Forgot Password:
- [ ] Request OTP with email
- [ ] Request OTP with phone
- [ ] Verify OTP in console (or email/SMS if configured)
- [ ] Reset password with valid OTP
- [ ] Try resetting with invalid OTP (should fail)
- [ ] Login with new password

### OTP:
- [ ] Generate OTP
- [ ] Validate correct OTP
- [ ] Try invalid OTP (should fail)
- [ ] Try expired OTP (should fail after timeout)

---

## 📝 Security Notes

1. **Never log OTPs in production** - Remove all `System.out.println(otp)` statements
2. **Implement proper OTP delivery** - Use SMS/Email service providers
3. **Add rate limiting** - Prevent OTP spam and brute force attacks
4. **Token expiry** - Ensure JWT tokens have reasonable expiry (currently set in JwtUtil)
5. **HTTPS only** - Always use HTTPS in production
6. **Validate social tokens** - Verify Google ID tokens and Apple auth codes server-side
7. **CORS configuration** - Ensure CORS is properly configured for your frontend

---

## 🚀 Deployment Steps

1. **Backend Changes**:
   - Add social-signin endpoint
   - Add forgot-password endpoints  
   - Add helper methods to UserService
   - Test locally
   - Deploy to server
   - Update SecurityConfig if needed

2. **Frontend Changes**:
   - Update ApiService registration response type
   - Add ForgotPasswordActivity (optional)
   - Test locally
   - Build release APK
   - Deploy to Play Store/TestFlight

3. **Post-Deployment**:
   - Monitor error logs
   - Test all authentication flows
   - Update documentation
   - Train support team

---

## 📞 Need Help?

If you encounter issues:
1. Check backend logs for detailed error messages
2. Check Android Logcat for frontend errors
3. Verify API endpoints in Postman/curl
4. Ensure JWT tokens are being sent correctly
5. Check MongoDB for user data consistency

Good luck! 🎉
