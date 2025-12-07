package com.gridee.parking.config

object ApiConfig {
    const val BASE_URL = "https://gridee.onrender.com/api/"
    const val REQUIRES_AUTH = false

    fun getAuthHeader(): String? = null

    fun isSSLRequired(): Boolean = BASE_URL.startsWith("https://")

    fun getEnvironmentInfo(): String = "BACKEND"
}
