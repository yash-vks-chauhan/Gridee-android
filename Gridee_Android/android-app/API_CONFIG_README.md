# API Configuration Guide

This guide explains how to switch between AWS production server and localhost development server.

## Quick Switch Instructions

### For UI Development (Localhost)
1. Open `app/src/main/java/com/gridee/parking/config/ApiConfig.kt`
2. Set `USE_LOCALHOST = true`
3. Build and install the app

### For Production (AWS)
1. Open `app/src/main/java/com/gridee/parking/config/ApiConfig.kt`
2. Set `USE_LOCALHOST = false`
3. Build and install the app

## Configuration Details

### Current Configuration
- **Localhost Mode**: `USE_LOCALHOST = true`
- **Base URL**: `http://localhost:8080/`
- **Authentication**: None required
- **SSL**: Disabled

### AWS Configuration (Commented/Preserved)
- **AWS URL**: `https://15.206.211.142:8443/`
- **Authentication**: Basic Auth (username: rajeev, password: parking)
- **SSL**: Custom SSL with trust-all certificates

## Files Modified for Localhost

### 1. ApiConfig.kt (NEW)
- Central configuration management
- Easy toggle between environments
- Environment-specific settings

### 2. ApiClient.kt
- AWS configuration commented but preserved
- Dynamic URL and authentication based on ApiConfig
- SSL configuration only applied when needed

### 3. network_security_config.xml
- AWS domain configuration commented
- Localhost domains added (localhost, 10.0.2.2, 127.0.0.1, 192.168.1.1)
- Cleartext traffic enabled for localhost

## Localhost Server Setup

For UI development, you'll need to run a local server on port 8080 that provides the same API endpoints as AWS:

### Required Endpoints:
- POST `/api/auth/login` - User authentication
- GET `/api/bookings/user/{userId}` - Get user bookings
- GET `/api/parking-lots` - Get parking lots
- POST `/api/bookings` - Create booking
- And other endpoints as needed

### Alternative URLs:
If your local server runs on a different port or IP, update `ApiConfig.kt`:
```kotlin
const val LOCALHOST_BASE_URL = "http://localhost:3000/" // Change port
const val LOCALHOST_BASE_URL = "http://192.168.1.100:8080/" // Use your IP
```

## Network Configuration

### For Android Emulator:
Use `http://10.0.2.2:8080/` (already configured in network security config)

### For Physical Device:
Use your computer's IP address: `http://192.168.1.XXX:8080/`

## Switching Back to AWS

1. Set `USE_LOCALHOST = false` in `ApiConfig.kt`
2. Uncomment AWS domain in `network_security_config.xml` if needed
3. Build and install

All AWS-related code is preserved and ready to use!

## Debugging

Check logcat for API configuration info:
```
adb logcat -s System.out | grep "ApiClient"
```

You should see:
- `ApiClient: Environment: LOCALHOST` (for localhost)
- `ApiClient: Environment: AWS` (for AWS)
