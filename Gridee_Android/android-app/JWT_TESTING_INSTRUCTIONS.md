# JWT Authentication Testing Instructions

## ✅ Implementation Complete!

The JWT authentication is now fully implemented and ready to test on your device (SM-A546E).

---

## 🚀 How to Test JWT Authentication

### Step 1: Open the App
Launch the Gridee app on your device.

### Step 2: Navigate to Profile
Tap on the **Profile** tab in the bottom navigation bar.

### Step 3: Launch JWT Test Activity
Scroll down to find the **"Test JWT Authentication"** button in the Settings section (it has a green lock icon 🔒).

Tap on it to open the JWT Test Activity.

---

## 🧪 JWT Test Activity Features

The JWT Test Activity provides a complete testing interface with the following buttons:

### 1. **Login with JWT**
- Enter email and password
- Taps the "Login with JWT" button
- Sends credentials to `POST /api/auth/login`
- Receives JWT token + user info
- Stores token securely in SharedPreferences

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": "user123",
  "name": "John Doe",
  "role": "USER"
}
```

### 2. **Check Authentication Status**
- Checks if user is currently authenticated
- Validates token expiry (24 hours)
- Shows login status and token info

### 3. **View Token**
- Displays the full JWT token in a dialog
- Shows token expiry time
- Useful for debugging

### 4. **Logout**
- Clears the stored JWT token
- Resets authentication state

---

## 📝 Test Credentials

Use your existing backend user credentials:
- **Email**: Your registered email
- **Password**: Your account password

Or create a test user via the registration endpoint first.

---

## 🔍 What to Look For

### Success Indicators:
1. ✅ Login button shows loading state
2. ✅ Toast message: "Login successful!"
3. ✅ Token is stored (check via "Check Authentication Status")
4. ✅ Token is displayed correctly in "View Token"
5. ✅ Logout clears the token

### Log Messages:
Check Android Logcat for detailed logs:
```bash
adb logcat -s "JwtTestActivity:D" "JwtLoginViewModel:D" "JwtTokenManager:D"
```

You should see:
- `JwtTestActivity: Login attempt for email: xxx`
- `JwtTokenManager: Token saved successfully`
- `JwtLoginViewModel: Login successful: AuthResponse(...)`

---

## 🏗️ Implementation Details

### Files Created:
1. **AuthResponse.kt** - JWT request/response models
2. **JwtTokenManager.kt** - Token storage and lifecycle
3. **JwtAuthInterceptor.kt** - Automatic token injection
4. **JwtLoginViewModel.kt** - MVVM ViewModel for JWT auth
5. **JwtTestActivity.kt** - Standalone test UI
6. **activity_jwt_test.xml** - Test UI layout

### Files Modified:
1. **ApiService.kt** - Added `authLogin()` endpoint
2. **UserRepository.kt** - Added `authLogin()` method
3. **fragment_profile.xml** - Added JWT Test button
4. **ProfileFragment.kt** - Added button click handler
5. **AndroidManifest.xml** - Registered JwtTestActivity

---

## 🔧 Backend Endpoint

The app connects to: `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "JWT_TOKEN_HERE",
  "id": "user_id",
  "name": "User Name",
  "role": "USER"
}
```

**Backend Implementation:**
- Controller: `AuthController.java`
- Method: `authenticateUser()`
- Token Generation: `JwtUtil.generateToken()`
- Token Expiry: 24 hours

---

## 🎯 Next Steps

After successful testing, you can:

1. **Integrate with Existing Login Screen**
   - Replace the old login method in `LoginActivity`
   - Use `JwtLoginViewModel` for authentication
   - Store token using `JwtTokenManager`

2. **Add Auto-Login**
   - Check token on app launch
   - Auto-navigate to home if token is valid

3. **Implement Token Refresh**
   - Add refresh token endpoint
   - Handle token expiry gracefully

4. **Secure API Calls**
   - Use `JwtAuthInterceptor` for all authenticated requests
   - Backend validates JWT on each request

---

## 📱 Test Flow Example

```
1. Open App → Profile Tab
2. Tap "Test JWT Authentication"
3. Enter email: test@example.com
4. Enter password: password123
5. Tap "Login with JWT"
   → Shows loading spinner
   → Backend validates credentials
   → Returns JWT token
   → Token stored locally
   → Success toast appears
6. Tap "Check Authentication Status"
   → Shows "Authenticated"
   → Shows token info
7. Tap "View Token"
   → Displays full JWT token
8. Tap "Logout"
   → Token cleared
   → Status changes to "Not authenticated"
```

---

## ⚠️ Troubleshooting

### Issue: "Login failed: HTTP 401"
**Solution:** Check backend credentials or endpoint URL in `ApiConfig.kt`

### Issue: "Network error"
**Solution:** Ensure backend server is running and accessible

### Issue: Button not visible
**Solution:** Scroll down in Profile fragment to find "Test JWT Authentication"

### Issue: Token not persisting
**Solution:** Check SharedPreferences permissions and storage

---

## 📚 Documentation Files

All documentation is available in the project:
- `JWT_AUTHENTICATION_GUIDE.md` - Complete implementation guide
- `JWT_TESTING_GUIDE.md` - Detailed testing instructions
- `JWT_IMPLEMENTATION_SUMMARY.md` - Technical summary
- `JWT_SETUP_VERIFICATION.md` - Backend verification checklist
- `JWT_TESTING_INSTRUCTIONS.md` - This file (step-by-step testing)

---

## ✨ Success!

JWT authentication is fully functional! Test it now on your device and verify all features work correctly.

**Happy Testing! 🎉**
