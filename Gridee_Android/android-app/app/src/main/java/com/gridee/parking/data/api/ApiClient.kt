package com.gridee.parking.data.api

import com.gridee.parking.config.ApiConfig

/**
 * Shared API client configuration.
 * BASE_URL keeps the trailing slash to satisfy Retrofit requirements.
 */
object ApiClient {
    const val BASE_URL: String = ApiConfig.BASE_URL
}
