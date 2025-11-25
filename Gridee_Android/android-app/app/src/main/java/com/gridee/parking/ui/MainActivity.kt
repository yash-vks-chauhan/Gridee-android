package com.gridee.parking.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityMainBinding
import com.gridee.parking.ui.auth.LoginActivity
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation

class MainActivity : BaseActivityWithBottomNav<ActivityMainBinding>() {

    private lateinit var viewModel: MainViewModel

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun getCurrentTab(): Int {
        return CustomBottomNavigation.TAB_HOME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupObservers()
        setupClickListeners()
    }

    override fun setupUI() {
        // Get user name from SharedPreferences or Intent
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val userName = intent.getStringExtra("USER_NAME") 
            ?: sharedPref.getString("user_name", "User") 
            ?: "User"
        
        binding.tvWelcome.text = "Welcome back, $userName!"
        
        // Setup scroll behavior for the main scroll view
        val scrollView = findViewById<ScrollView>(R.id.scroll_content)
        if (scrollView != null) {
            println("MainActivity: Setting up scroll behavior for ScrollView")
            setupScrollBehaviorForView(scrollView)
        } else {
            println("MainActivity: ScrollView not found!")
        }
    }

    private fun setupObservers() {
        // TODO: Observe ViewModel data when location and parking features are added
    }

    private fun setupClickListeners() {
        println("MainActivity: Setting up click listeners")
        
        binding.cardSearch.setOnClickListener {
            // Navigate to Find Parking screen
            val intent = Intent(this, com.gridee.parking.ui.discovery.ParkingDiscoveryActivity::class.java)
            startActivity(intent)
        }

        println("MainActivity: All click listeners set up successfully")
    }

    private fun logout() {
        // Clear any stored user data
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        // Navigate back to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
