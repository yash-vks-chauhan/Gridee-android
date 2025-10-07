package com.gridee.parking.data.api

import com.gridee.parking.config.ApiConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiClient {
    // Dynamic BASE_URL from ApiConfig
    private val BASE_URL = ApiConfig.BASE_URL
    
    // AWS Configuration (commented for localhost development)
    // private const val BASE_URL = "https://65.2.80.78:8443/"
    
    // Localhost Configuration for UI development
    // private const val BASE_URL = "http://localhost:8080/"
    // Alternative localhost URLs (uncomment as needed):
    // private const val BASE_URL = "http://10.0.2.2:8080/" // For Android Emulator
    // private const val BASE_URL = "http://192.168.1.100:8080/" // For physical device (replace with your IP)
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Create a trust manager that accepts all certificates (for AWS HTTPS)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            
            // Add authentication header if required (AWS)
            ApiConfig.getAuthHeader()?.let { authHeader ->
                requestBuilder.addHeader("Authorization", authHeader)
            }
            
            val request = requestBuilder.build()
            
            println("ApiClient: Environment: ${ApiConfig.getEnvironmentInfo()}")
            println("ApiClient: Making request to: ${request.url}")
            println("ApiClient: Request headers: ${request.headers}")
            
            try {
                val response = chain.proceed(request)
                println("ApiClient: Response code: ${response.code}")
                println("ApiClient: Response message: ${response.message}")
                response
            } catch (e: Exception) {
                println("ApiClient: Network error: ${e.message}")
                throw e
            }
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .apply {
            // SSL Configuration for AWS HTTPS (only applied when needed)
            if (ApiConfig.isSSLRequired()) {
                try {
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    hostnameVerifier { _, _ -> true }
                    println("ApiClient: SSL configuration applied for AWS")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                println("ApiClient: Using HTTP for localhost - no SSL configuration needed")
            }
        }
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
