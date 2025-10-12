package com.gridee.parking.config

/**
 * API Configuration for easy switching between environments
 * For UI development: Set USE_LOCALHOST = true
 * For production/AWS: Set USE_LOCALHOST = false
 */
object ApiConfig {
    // Toggle this flag to switch between localhost and AWS
    const val USE_LOCALHOST = false  // Changed back to false for AWS
    
    // AWS Configuration
    const val AWS_BASE_URL = "https://65.0.99.49:8443/"
    const val AWS_USERNAME = "rajeev"
    const val AWS_PASSWORD = "parking"
    
    // Localhost Configuration
    const val LOCALHOST_BASE_URL = "http://localhost:8080/" // Use adb reverse for direct localhost access
    const val LOCALHOST_MACHINE_IP_URL = "http://10.3.79.235:8080/" // Your machine's IP as fallback
    const val LOCALHOST_EMULATOR_URL = "http://10.0.2.2:8080/" // For Android Emulator
    
    // Current configuration based on flag
    val BASE_URL = if (USE_LOCALHOST) LOCALHOST_BASE_URL else AWS_BASE_URL
    val REQUIRES_AUTH = !USE_LOCALHOST // AWS requires auth, localhost doesn't
    
    /**
     * Quick switch methods for easy configuration changes
     */
    fun getAuthHeader(): String? {
        return if (REQUIRES_AUTH) {
            "Basic " + android.util.Base64.encodeToString("$AWS_USERNAME:$AWS_PASSWORD".toByteArray(), android.util.Base64.NO_WRAP)
        } else {
            null
        }
    }
    
    fun isSSLRequired(): Boolean = !USE_LOCALHOST
    
    fun getEnvironmentInfo(): String {
        return if (USE_LOCALHOST) "LOCALHOST" else "AWS"
    }
}
