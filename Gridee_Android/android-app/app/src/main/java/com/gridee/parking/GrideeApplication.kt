package com.gridee.parking

import android.app.Application
import com.razorpay.Checkout

class GrideeApplication : Application() {
    companion object {
        lateinit var instance: GrideeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            Checkout.preload(applicationContext)
        } catch (_: Exception) {
        }
    }
}
