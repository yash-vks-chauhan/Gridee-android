# Gridee Android App

This is the Android frontend for the Gridee parking management system.

## Features

### ✅ Completed
- **User Registration**: Complete user registration with validation
  - Name, email, phone validation
  - Password confirmation
  - Optional vehicle numbers (up to 2)
  - Password hashing (SHA-256)
  - Real-time form validation
  - API integration with backend

- **User Login**: Beautiful login screen matching design specs
  - Email/phone and password authentication
  - Password visibility toggle
  - Form validation and error handling
  - Social login buttons (Apple/Google - UI ready)
  - Forgot password link (placeholder)
  - Seamless navigation between login/registration
  - API integration with backend authentication

### 🚧 In Progress
- Main Dashboard
- Parking Lot Discovery
- Booking Management
- Payment Integration
- User Profile Management

## Architecture

- **MVVM Pattern**: ViewModel + LiveData
- **Repository Pattern**: Data layer abstraction
- **Retrofit**: REST API communication
- **Material Design 3**: Modern UI components
- **View Binding**: Type-safe view references
- **Coroutines**: Asynchronous programming

## API Integration

The app connects to the Spring Boot backend at:
- **Emulator**: `http://10.0.2.2:8080/`
- **Physical Device**: Update `ApiClient.kt` with your local IP

## Setup Instructions

1. **Backend Setup**: Ensure the Spring Boot backend is running on port 8080
2. **MongoDB**: Make sure MongoDB is running with the `parkingdb` database
3. **Network Configuration**: 
   - For emulator: No changes needed
   - For physical device: Update `BASE_URL` in `ApiClient.kt` with your computer's IP address

## Project Structure

```
app/src/main/java/com/gridee/parking/
├── data/
│   ├── api/           # API service and client
│   ├── model/         # Data models
│   └── repository/    # Repository pattern implementation
├── ui/
│   └── auth/          # Authentication screens
└── GrideeApplication.kt
```

## Dependencies

- AndroidX Core KTX
- Material Design Components
- Retrofit & OkHttp
- Gson Converter
- Coroutines
- Navigation Component
- Lifecycle Components

## Next Steps

1. Create Login Activity
2. Implement Main Dashboard
3. Add Parking Lot Browsing
4. Implement Booking System
5. Add Payment Integration
6. Create User Profile Management

## Testing

The registration feature is ready for testing:
1. Start the backend server
2. Run the Android app
3. Fill in the registration form
4. Verify user creation in MongoDB
