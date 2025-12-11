# JWT Authentication Testing - Quick Start Guide

## ✅ What's Been Implemented

### 1. **JWT Authentication System**
- Full JWT-based authentication via `POST /api/auth/login`
- Token storage with 24-hour expiry
- Automatic token injection for API calls
- Complete MVVM architecture

### 2. **Test UI (JwtTestActivity)**
- Accessible from Profile → "Test JWT Login" button
- Features:
  - Login with email/password
  - Check authentication status
  - View stored token
  - Logout functionality
  - Real-time status updates

## 🚀 How to Test

### Step 1: Open the App
The app is now installed on your device (SM-A546E).

### Step 2: Navigate to JWT Test Screen
1. Open the Gridee app
2. Go to **Profile** tab (bottom navigation)
3. Scroll to **Settings** section
4. Tap on **"Test JWT Login"** button
5. JWT Test Activity will open

### Step 3: Test Login
1. **Enter credentials:**
   - Email: `test@example.com` (or any registered user)
   - Password: Your password
   
2. **Tap "Login with JWT"**

3. **Expected Result:**
   - Status changes to "Loading..."
   - If successful: "✅ Login Successful!"
   - Token is saved to SharedPreferences
   - User info displayed in logs

### Step 4: Check Authentication
1. **Tap "Check Authentication Status"**

2. **Expected Results:**
   - If logged in: "✅ Authenticated"
   - Shows token expiry time
   - If not logged in: "❌ Not Authenticated"

### Step 5: View Token
1. **Tap "View Token"**

2. **Expected Result:**
   - Dialog shows the JWT token
   - Format: "Bearer eyJhbGciOiJIUzI1Ni..."
   - Copy button available

### Step 6: Test Logout
1. **Tap "Logout"**

2. **Expected Result:**
   - Token cleared from storage
   - Status changes to "Logged Out"
   - "Check Authentication" now shows "❌ Not Authenticated"

## 📱 Testing Scenarios

### Scenario 1: Valid Login
```
Email: test@example.com
Password: CorrectPassword
Expected: ✅ Success, token stored
```

### Scenario 2: Invalid Credentials
```
Email: wrong@example.com
Password: WrongPassword
Expected: ❌ Error message shown
```

### Scenario 3: Token Persistence
```
1. Login successfully
2. Close the app completely
3. Reopen the app
4. Go to JWT Test
5. Tap "Check Authentication"
Expected: ✅ Still authenticated (if < 24 hours)
```

### Scenario 4: Token Expiry
```
1. Login successfully
2. Wait 24 hours (or manually change system time)
3. Tap "Check Authentication"
Expected: ❌ Not authenticated (token expired)
```

## 🔍 Monitoring Logs

To see detailed logs, run:
```bash
adb logcat -s "JwtTestActivity:D" "JwtTokenManager:D" "JwtLoginViewModel:D" "ApiClient:D"
```

### What to Look For:
- `JwtTestActivity: Login button clicked`
- `JwtLoginViewModel: Login attempt for: <email>`
- `JwtTokenManager: Saving JWT token`
- `JwtTokenManager: Token is valid, expires at: <timestamp>`
- `ApiClient: Request headers: Authorization: Bearer <token>`

## 🎯 Success Indicators

✅ **Login Success:**
- Status shows "✅ Login Successful!"
- Token is visible when tapping "View Token"
- "Check Authentication" shows "✅ Authenticated"

✅ **Token Storage:**
- Token persists after app restart
- Token automatically added to API requests

✅ **Logout Success:**
- Status shows "Logged Out"
- Token is cleared
- "Check Authentication" shows "❌ Not Authenticated"

## 🔧 Troubleshooting

### Issue: "Activity not found"
**Solution:** ✅ Already fixed! The import is now correct.

### Issue: "Network error"
**Possible causes:**
1. Backend not running
2. Wrong BASE_URL in ApiConfig
3. Network connectivity issue

**Check:**
```bash
# Verify backend is running
curl http://localhost:8080/api/auth/login

# Check ApiConfig.BASE_URL
# Should point to your backend server
```

### Issue: "Invalid credentials"
**Solution:**
1. Ensure user exists in database
2. Verify password is correct
3. Check backend logs for authentication errors

### Issue: Token not persisting
**Solution:**
1. Check SharedPreferences permissions
2. Verify token expiry time (24 hours)
3. Look for JwtTokenManager logs

## 📝 Key Files

| File | Purpose |
|------|---------|
| `JwtTestActivity.kt` | Test UI for JWT authentication |
| `JwtLoginViewModel.kt` | Handles login logic and state |
| `JwtTokenManager.kt` | Token storage and lifecycle |
| `JwtAuthInterceptor.kt` | Auto-injects tokens in API calls |
| `ApiService.kt` | Contains `authLogin()` endpoint |
| `UserRepository.kt` | Repository layer for auth calls |

## 🎉 Next Steps

1. **Test the basic flow** (login → check auth → logout)
2. **Verify token persistence** (restart app)
3. **Test with real backend** (ensure backend is running)
4. **Monitor logs** for any errors
5. **Integrate into main login** (optional - replace legacy login)

## 💡 Tips

- **Use Logcat** to see detailed authentication flow
- **Test token expiry** by changing system time
- **Copy token** to verify it's valid JWT format
- **Test offline mode** to see token persistence

---

**Status:** ✅ Ready to Test  
**Last Updated:** October 14, 2025  
**Device:** SM-A546E (Android 15)
