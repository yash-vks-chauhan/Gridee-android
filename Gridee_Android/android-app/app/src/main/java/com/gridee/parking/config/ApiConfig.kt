package com.gridee.parking.config

/**
 * API Configuration for backend server
 * Configure your backend server URL here
 */
object ApiConfig {
    // Backend Server Configuration
    // Note: Base URL must NOT include "/api" because paths in ApiService already start with "api/..."
    
    // ✅ FOR PHYSICAL DEVICE: Use your computer's local IP (make sure device is on same WiFi)
    const val BASE_URL = "http://10.58.156.227:8080/"  // Current computer IP (Hotspot)
    
    // Alternative backend URLs (uncomment to use)
    // const val BASE_URL = "http://localhost:8080/"  // Via ADB reverse (needs: adb reverse tcp:8080 tcp:8080)
    // const val BASE_URL = "http://10.0.2.2:8080/"  // For Android emulator
    // const val BASE_URL = "http://192.168.42.227:8080/"  // Phone hotspot network
    // const val BASE_URL = "http://192.168.146.227:8080/"  // Previous network
    // const val BASE_URL = "http://192.168.1.103:8080/"  // For regular WiFi network
    
    // Authentication is handled via JWT tokens (no basic auth required)
    val REQUIRES_AUTH = false
    
    /**
     * Quick switch methods for easy configuration changes
     */
    fun getAuthHeader(): String? {
        return null  // JWT tokens are handled by JwtAuthInterceptor
    }
    
    // Apply SSL trust configuration whenever using https
    fun isSSLRequired(): Boolean = BASE_URL.startsWith("https://")
    
    fun getEnvironmentInfo(): String = "BACKEND"
}
