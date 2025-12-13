package com.gridee.parking

import android.app.Application
import com.razorpay.Checkout
import com.google.android.gms.ads.MobileAds

class GrideeApplication : Application() {
    companion object {
        lateinit var instance: GrideeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            Checkout.preload(applicationContext)
            // Initialize Google Mobile Ads SDK (AdMob)
            MobileAds.initialize(this) { }
        } catch (_: Exception) {
        }
    }
}
