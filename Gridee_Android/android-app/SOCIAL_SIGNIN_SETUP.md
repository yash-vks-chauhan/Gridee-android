# Google and Apple Sign-In Integration Setup Guide

This guide will help you set up Google Sign-In and Apple Sign-In for the Gridee Android app.

## Table of Contents
1. [Google Sign-In Setup](#google-sign-in-setup)
2. [Apple Sign-In Setup](#apple-sign-in-setup)
3. [Backend Integration](#backend-integration)
4. [Testing](#testing)

---

## Google Sign-In Setup

### Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the **Google Sign-In API**

### Step 2: Configure OAuth Consent Screen

1. Navigate to **APIs & Services** > **OAuth consent screen**
2. Choose **External** user type
3. Fill in the required information:
   - App name: `Gridee`
   - User support email: Your email
   - Developer contact email: Your email
4. Add scopes: `email` and `profile`
5. Save and continue

### Step 3: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. Create **two** OAuth client IDs:

#### Web Client ID (Required for Android)
   - Application type: **Web application**
   - Name: `Gridee Web Client`
   - This generates your Web Client ID (looks like: `123456789-abc.apps.googleusercontent.com`)

#### Android Client ID
   - Application type: **Android**
   - Name: `Gridee Android`
   - Package name: `com.gridee.parking`
   - SHA-1 certificate fingerprint: Get it by running:
     ```bash
     # For debug builds
     keytool -keystore ~/.android/debug.keystore -list -v -alias androiddebugkey
     # Password is: android
     
     # For release builds
     keytool -keystore path/to/your/release.keystore -list -v
     ```

### Step 4: Download google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Add your project or link existing Google Cloud project
3. Add an Android app with package name: `com.gridee.parking`
4. Download `google-services.json`
5. Place it in: `/app/google-services.json` (replace the template file)

### Step 5: Update Your App

1. Open `/app/src/main/res/values/strings.xml`
2. Replace `YOUR_WEB_CLIENT_ID` with your actual Web Client ID:
   ```xml
   <string name="default_web_client_id">YOUR_ACTUAL_WEB_CLIENT_ID.apps.googleusercontent.com</string>
   ```

---

## Apple Sign-In Setup

### Step 1: Apple Developer Account

1. You need an [Apple Developer Account](https://developer.apple.com/) ($99/year)
2. Sign in to your Apple Developer account

### Step 2: Configure App ID

1. Go to **Certificates, Identifiers & Profiles**
2. Select **Identifiers** > **App IDs**
3. Click the **+** button to create a new App ID (or edit existing)
4. Enable **Sign in with Apple** capability
5. Configure Sign in with Apple:
   - Enable as a primary App ID
   - Save your changes

### Step 3: Create a Service ID

1. Go to **Identifiers** and click **+**
2. Select **Services IDs** and click **Continue**
3. Fill in:
   - Description: `Gridee Sign In`
   - Identifier: `com.gridee.parking.signin` (or your preferred ID)
4. Enable **Sign in with Apple**
5. Click **Configure** next to Sign in with Apple
6. Add your domain and return URLs:
   - Domains: `your-domain.com`
   - Return URLs: `https://your-domain.com/auth/apple/callback`
7. Save and continue

### Step 4: Create a Key

1. Go to **Keys** and click **+**
2. Enter a name: `Gridee Apple Sign In Key`
3. Enable **Sign in with Apple**
4. Click **Configure** and select your primary App ID
5. Download the key file (`.p8`) - **Save it securely!**
6. Note your **Key ID** and **Team ID**

### Step 5: Update Your App

1. Open `/app/src/main/java/com/gridee/parking/utils/AppleSignInManager.kt`
2. Update the constants:
   ```kotlin
   private const val CLIENT_ID = "com.gridee.parking.signin" // Your Service ID
   private const val REDIRECT_URI = "https://your-domain.com/auth/apple/callback"
   ```

---

## Backend Integration

Your backend needs to handle authentication tokens from Google and Apple. Here's what you need to implement:

### Google Sign-In Backend Endpoint

**Endpoint:** `POST /api/users/social-signin`

**Request Body:**
```json
{
  "idToken": "google_id_token_here",
  "email": "user@example.com",
  "name": "John Doe",
  "profilePicture": "https://...",
  "provider": "google"
}
```

**Backend Verification:**
```java
// Example using Google API Client Library for Java
GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
    .setAudience(Collections.singletonList(CLIENT_ID))
    .build();

GoogleIdToken idToken = verifier.verify(idTokenString);
if (idToken != null) {
    GoogleIdToken.Payload payload = idToken.getPayload();
    String userId = payload.getSubject();
    String email = payload.getEmail();
    // Create or login user in your database
}
```

### Apple Sign-In Backend Endpoint

**Endpoint:** `POST /api/users/social-signin`

**Request Body:**
```json
{
  "authorizationCode": "apple_authorization_code_here",
  "provider": "apple"
}
```

**Backend Verification:**
```java
// You need to verify the authorization code with Apple's servers
// Exchange authorization code for tokens at:
// POST https://appleid.apple.com/auth/token

// Required parameters:
// - client_id: Your Service ID
// - client_secret: JWT signed with your private key
// - code: The authorization code
// - grant_type: authorization_code
```

**Response Format (for both):**
```json
{
  "id": "user_id",
  "name": "John Doe",
  "email": "user@example.com",
  "phone": "+1234567890",
  "vehicleNumbers": [],
  "walletCoins": 0,
  "firstUser": true
}
```

---

## Testing

### Test Google Sign-In

1. Build and install the app:
   ```bash
   cd /Users/yashchauhan/Gridee/Gridee_Android/android-app
   ./gradlew clean assembleDebug installDebug
   ```

2. Open the app and navigate to the login screen
3. Click "Sign in with Google"
4. Select a Google account
5. Check logs for any errors:
   ```bash
   adb logcat -s "GoogleSignInManager:D" "LoginViewModel:D" "ApiClient:D"
   ```

### Test Apple Sign-In

1. Apple Sign-In on Android has limited functionality
2. It opens a web view for authentication
3. Test on a physical device with internet connection
4. Check logs:
   ```bash
   adb logcat -s "AppleSignInManager:D" "LoginViewModel:D" "ApiClient:D"
   ```

### Common Issues

#### Google Sign-In Issues

1. **"Sign in failed: 10"** - SHA-1 fingerprint mismatch
   - Regenerate SHA-1 and update in Google Cloud Console

2. **"Sign in failed: 12501"** - User cancelled or network issue
   - Check internet connection
   - Ensure google-services.json is correct

3. **"No Web Client ID"** - Missing configuration
   - Verify strings.xml has the correct Web Client ID

#### Apple Sign-In Issues

1. **Web view not opening** - Configuration error
   - Verify CLIENT_ID and REDIRECT_URI
   - Check internet connection

2. **"Sign in failed"** - Invalid credentials
   - Verify Service ID is correct
   - Ensure domain is verified in Apple Developer Console

---

## Security Notes

1. **Never commit sensitive files:**
   - Add to `.gitignore`:
     ```
     google-services.json
     *.keystore
     *.p8
     ```

2. **Backend Security:**
   - Always verify tokens on the backend
   - Never trust client-sent user data without verification
   - Use HTTPS for all API calls

3. **Production Keys:**
   - Use different OAuth credentials for debug and release builds
   - Store release keystore securely
   - Keep Apple private key (`.p8`) secure

---

## Troubleshooting

### Check Build Configuration

Ensure your `app/build.gradle` has:
```gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation 'com.google.android.gms:play-services-auth:20.7.0'
    implementation 'com.willowtreeapps.signinwithapplebutton:signinwithapplebutton:0.3'
}
```

### Check Project-level build.gradle

Ensure your project `build.gradle` has:
```gradle
plugins {
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

### Enable Logging

Add to `LoginViewModel.kt` for debugging:
```kotlin
import android.util.Log

companion object {
    private const val TAG = "LoginViewModel"
}

// In functions:
Log.d(TAG, "Google sign in started")
```

---

## Additional Resources

- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Apple Sign-In Documentation](https://developer.apple.com/sign-in-with-apple/)
- [Firebase Authentication](https://firebase.google.com/docs/auth/android/start)

---

## Support

If you encounter issues:
1. Check the logs using `adb logcat`
2. Verify all credentials are correct
3. Ensure google-services.json matches your package name
4. Test on a physical device (not emulator) for best results
