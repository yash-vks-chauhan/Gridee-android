# JWT Authentication Testing Guide

## 🧪 How to Test JWT Authentication in Frontend

This guide provides step-by-step instructions to test the JWT authentication implementation in your Android app.

---

## 📋 Prerequisites

Before testing, ensure:
- ✅ Backend server is running (local or production)
- ✅ Backend has the `/api/auth/login` endpoint implemented
- ✅ You have test user credentials (email/phone and password)
- ✅ Android app is built and running

---

## 🚀 Quick Test Methods

### Method 1: Use Existing LoginActivity with JWT (Recommended)

You can quickly test by temporarily modifying your existing `LoginActivity` to use JWT authentication.

#### Step 1: Create a Test Activity

I've created `JwtTestActivity.kt` for you - a standalone activity to test JWT authentication without modifying your existing login.

#### Step 2: Add to AndroidManifest.xml

```xml
<activity
    android:name=".ui.auth.JwtTestActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Note:** Remove the `LAUNCHER` intent filter from your main activity temporarily for testing.

#### Step 3: Build and Run

```bash
cd /Users/yashchauhan/Gridee/Gridee_Android/android-app
./gradlew clean assembleDebug installDebug
```

---

## 🧪 Manual Testing Steps

### Test 1: Basic Login Flow

1. **Launch the app**
2. **Enter credentials:**
   - Email/Phone: `test@example.com` (or your test user)
   - Password: `your_password`
3. **Click "Login with JWT"**
4. **Expected Results:**
   - Loading indicator appears
   - Success message: "Login successful!"
   - Token is automatically saved
   - Navigate to main screen

### Test 2: Verify Token Storage

1. **After successful login, check Logcat:**
   ```bash
   adb logcat | grep "JwtTokenManager"
   ```
2. **Expected output:**
   ```
   JwtTokenManager: Token saved successfully
   JwtTokenManager: User ID: user_123
   JwtTokenManager: User Name: John Doe
   ```

### Test 3: Check Authentication Status

1. **Close and reopen the app**
2. **Expected behavior:**
   - App should detect existing valid token
   - Skip login screen
   - Navigate directly to main screen

### Test 4: Test Invalid Credentials

1. **Enter wrong credentials:**
   - Email: `wrong@example.com`
   - Password: `wrongpass`
2. **Click Login**
3. **Expected result:**
   - Error message: "Invalid email/phone or password"
   - No token saved

### Test 5: Test Token in API Requests

1. **After login, trigger any API call that requires authentication**
2. **Check Logcat:**
   ```bash
   adb logcat | grep "Authorization"
   ```
3. **Expected output:**
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

### Test 6: Test Logout

1. **Navigate to Profile/Settings**
2. **Click Logout**
3. **Expected results:**
   - Token is cleared
   - User data is cleared
   - Redirect to login screen

### Test 7: Test Token Expiry

1. **Manually set old timestamp** (for testing):
   - Use Debug mode to modify timestamp
   - Or wait 24 hours (default expiry)
2. **Reopen app**
3. **Expected behavior:**
   - Token detected as expired
   - Automatically cleared
   - Redirect to login screen

---

## 🔍 Testing with Logcat

### Enable Full Logging

```bash
cd /Users/yashchauhan/Gridee/Gridee_Android/android-app
adb logcat -c  # Clear logs
adb logcat -s "JwtLoginViewModel:D" "JwtTokenManager:D" "JwtAuthInterceptor:D" "ApiClient:D"
```

### What to Look For

**Successful Login:**
```
D/JwtLoginViewModel: Starting JWT login
D/ApiClient: POST /api/auth/login
D/ApiClient: Response: 200 OK
D/JwtTokenManager: Saving JWT token
D/JwtTokenManager: Token: eyJhbGc...
D/JwtLoginViewModel: Login successful
```

**Token Usage in API Call:**
```
D/JwtAuthInterceptor: Adding JWT token to request: /api/users/123/bookings
D/ApiClient: Authorization: Bearer eyJhbGc...
D/ApiClient: Response: 200 OK
```

**Token Expiry:**
```
D/JwtTokenManager: Token expired, clearing
D/JwtTokenManager: Token timestamp: 1697299200000
D/JwtTokenManager: Current time: 1697385600000
D/JwtTokenManager: Difference: 86400000ms (24 hours)
```

---

## 📱 Testing UI Flow

### Visual Indicators to Check

1. **Login Screen:**
   - ✅ Email/Phone input field
   - ✅ Password input field
   - ✅ Login button
   - ✅ Loading indicator (during login)
   - ✅ Error messages (for validation)

2. **Loading State:**
   - ✅ Progress bar visible
   - ✅ Login button disabled
   - ✅ Input fields disabled

3. **Success State:**
   - ✅ Success toast message
   - ✅ Navigation to main screen
   - ✅ User name displayed

4. **Error State:**
   - ✅ Error toast message
   - ✅ Form remains editable
   - ✅ Can retry login

---

## 🧰 Testing Tools

### Tool 1: ADB Commands

**Check if app is running:**
```bash
adb shell ps | grep com.gridee.parking
```

**View current activity:**
```bash
adb shell dumpsys activity activities | grep mResumedActivity
```

**Clear app data (reset state):**
```bash
adb shell pm clear com.gridee.parking
```

**View SharedPreferences (token storage):**
```bash
adb shell run-as com.gridee.parking cat /data/data/com.gridee.parking/shared_prefs/gridee_auth_prefs.xml
```

### Tool 2: Postman/cURL Testing

Test backend endpoint directly:

```bash
# Test login endpoint
curl -X POST http://your-backend-url:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "hashed_password_here"
  }'
```

Expected response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": "user_123",
  "name": "John Doe",
  "role": "USER"
}
```

### Tool 3: Android Studio Debugger

1. **Set breakpoint** in `JwtLoginViewModel.loginWithJwt()`
2. **Run app in debug mode**
3. **Step through code**
4. **Inspect variables:**
   - Request data
   - Response data
   - Token value
   - Error messages

---

## ✅ Test Checklist

Use this checklist to ensure complete testing:

### Basic Functionality
- [ ] Login with valid credentials succeeds
- [ ] Login with invalid credentials fails
- [ ] Token is saved after successful login
- [ ] User info is saved after successful login
- [ ] Loading indicator shows during login
- [ ] Error messages display correctly
- [ ] Validation errors show for empty fields

### Token Management
- [ ] Token is retrieved correctly after save
- [ ] Bearer token format is correct
- [ ] Token expiry is checked correctly
- [ ] Expired tokens are cleared automatically
- [ ] isAuthenticated() returns true when logged in
- [ ] isAuthenticated() returns false when logged out

### API Integration
- [ ] JWT token is added to authenticated requests
- [ ] Public endpoints don't get JWT token
- [ ] Backend accepts the JWT token
- [ ] 401 error handled properly for invalid token
- [ ] Token format matches backend expectations

### UI/UX
- [ ] Login screen displays correctly
- [ ] Input fields are editable
- [ ] Buttons are clickable
- [ ] Loading state disables interactions
- [ ] Success navigation works
- [ ] Error messages are user-friendly
- [ ] Keyboard dismisses after submit

### Persistence
- [ ] Token persists after app restart
- [ ] Auto-login works with valid token
- [ ] Login required when no token present
- [ ] Logout clears all data
- [ ] App state is consistent

### Edge Cases
- [ ] Handle network errors gracefully
- [ ] Handle backend downtime
- [ ] Handle malformed responses
- [ ] Handle very long passwords
- [ ] Handle special characters in input
- [ ] Handle rapid button clicks (debouncing)

---

## 🐛 Common Issues & Solutions

### Issue 1: "Token not saved"

**Check:**
- Logcat for errors
- Token value in response
- SharedPreferences permissions

**Solution:**
```kotlin
// Add logging in JwtTokenManager
println("Attempting to save token: ${token.substring(0, 10)}...")
```

### Issue 2: "Token not added to requests"

**Check:**
- JwtAuthInterceptor is added to OkHttpClient
- Endpoint is not in public paths list
- Token format is correct

**Solution:**
```kotlin
// Add logging in JwtAuthInterceptor
println("Should add JWT: ${shouldAddJwtToken(path)}")
println("Token: ${jwtTokenManager.getBearerToken()}")
```

### Issue 3: "Backend returns 401 Unauthorized"

**Check:**
- Token format (should be "Bearer {token}")
- Token expiry on backend
- Backend JWT secret key configuration

**Solution:**
- Test with Postman using same token
- Check backend logs
- Verify token signature

### Issue 4: "App crashes on login"

**Check:**
- Logcat for stack trace
- Context is not null
- All dependencies are initialized

**Solution:**
```bash
adb logcat -s "AndroidRuntime:E"
```

### Issue 5: "Network timeout"

**Check:**
- Backend is running
- Correct BASE_URL in ApiConfig
- Timeout settings in OkHttpClient

**Solution:**
```kotlin
// Increase timeout
.connectTimeout(60, TimeUnit.SECONDS)
.readTimeout(60, TimeUnit.SECONDS)
```

---

## 📊 Expected vs Actual Results

### Scenario: Successful Login

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Enter valid credentials | Fields accept input | | |
| Click Login | Loading starts | | |
| API Call | POST /api/auth/login | | |
| Response | 200 OK with token | | |
| Token Save | Success | | |
| Navigation | Go to main screen | | |
| Toast | "Welcome back, {name}" | | |

### Scenario: Failed Login

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Enter invalid credentials | Fields accept input | | |
| Click Login | Loading starts | | |
| API Call | POST /api/auth/login | | |
| Response | 401 Unauthorized | | |
| Error Display | "Invalid credentials" | | |
| UI State | Form still editable | | |
| Token Save | Not saved | | |

---

## 🎯 Performance Testing

### Test Network Latency

```bash
# Simulate slow network
adb shell settings put global wifi_sleep_policy 0
adb shell svc wifi disable
adb shell svc wifi enable
```

### Test Battery Impact

- Monitor battery usage during login
- Check wake locks
- Verify background services

### Test Memory Usage

```bash
adb shell dumpsys meminfo com.gridee.parking
```

---

## 🔐 Security Testing

### Test 1: Token in Logs
- Verify token is not fully logged in production
- Check only first few characters are logged

### Test 2: Token Storage
- Verify SharedPreferences is in MODE_PRIVATE
- Check token is not accessible to other apps

### Test 3: Network Sniffing
- Use proxy (Charles, Fiddler) to inspect traffic
- Verify token is sent over HTTPS only
- Check token is in Authorization header

---

## 📝 Test Report Template

```
Test Date: ___________
Tester: ___________
App Version: ___________
Backend URL: ___________

Test Results:
✅ Login with valid credentials: PASS
✅ Login with invalid credentials: PASS
✅ Token storage: PASS
✅ Token retrieval: PASS
✅ Auto-login: PASS
✅ Logout: PASS
✅ API authentication: PASS

Issues Found:
1. ___________
2. ___________

Notes:
___________
```

---

## 🚀 Automated Testing (Future)

### Unit Tests

```kotlin
@Test
fun testJwtLogin() = runTest {
    val viewModel = JwtLoginViewModel()
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    viewModel.loginWithJwt(context, "test@example.com", "password")
    
    val state = viewModel.authState.value
    assertTrue(state is JwtAuthState.Success)
}
```

### Integration Tests

```kotlin
@Test
fun testTokenPersistence() {
    val jwtManager = JwtTokenManager(context)
    jwtManager.saveAuthToken("token", "id", "name", "role")
    
    // Simulate app restart
    val newManager = JwtTokenManager(context)
    assertEquals("token", newManager.getAuthToken())
}
```

---

## 📞 Need Help?

If you encounter issues:
1. Check this guide first
2. Review `JWT_AUTHENTICATION_GUIDE.md`
3. Check Logcat logs
4. Test backend endpoint separately
5. Review example code in `JwtTestActivity.kt`

---

**Happy Testing!** 🎉

Last Updated: October 14, 2025
