# API Configuration Guide

This guide explains how to switch the Android app between the production backend (Render) and a local development backend.

## Quick Switch Instructions

### Production (Render)
1. Open `app/src/main/java/com/gridee/parking/config/ApiConfig.kt`
2. Set `BASE_URL` to `https://gridee.onrender.com/`

### Local Development
1. Open `app/src/main/java/com/gridee/parking/config/ApiConfig.kt`
2. Set `BASE_URL` to one of the local URLs below

## Configuration Details

### Production Backend (Render)
- **API base**: `https://gridee.onrender.com/api`
- **App `BASE_URL` value**: `https://gridee.onrender.com/` (do not include `/api`)
- **SSL**: Standard HTTPS (no custom SSL required)

### Local Backend URLs
- **Android Emulator**: `http://10.0.2.2:8080/`
- **ADB reverse**: `http://localhost:8080/` (run `adb reverse tcp:8080 tcp:8080`)
- **Physical device**: `http://<your-computer-ip>:8080/`

## Localhost Server Setup

For UI development, you'll need to run a local server on port 8080 that provides the same API endpoints as production:

### Required Endpoints:
- POST `/api/auth/login` - User authentication
- GET `/api/bookings/user/{userId}` - Get user bookings
- GET `/api/parking-lots` - Get parking lots
- POST `/api/bookings` - Create booking
- And other endpoints as needed

### Alternative URLs:
If your local server runs on a different port or IP, update `ApiConfig.kt`:
```kotlin
const val BASE_URL = "http://localhost:3000/" // Change port
const val BASE_URL = "http://192.168.1.100:8080/" // Use your IP
```

## Network Configuration

### For Android Emulator:
Use `http://10.0.2.2:8080/` (already configured in network security config)

### For Physical Device:
Use your computer's IP address: `http://192.168.1.XXX:8080/`

## Debugging

Check logcat for API configuration info:
```
adb logcat -s System.out | grep "ApiClient"
```

You should see:
- `ApiClient: Environment: RENDER`
