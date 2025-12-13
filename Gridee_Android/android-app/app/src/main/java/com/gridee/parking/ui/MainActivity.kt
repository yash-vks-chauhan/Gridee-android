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
        

        
        // Verify FAB exists and set up click listener
        val fab = binding.fabBookParking
        println("MainActivity: FAB found: ${fab != null}")
        println("MainActivity: FAB isClickable: ${fab.isClickable}")
        println("MainActivity: FAB isEnabled: ${fab.isEnabled}")
        println("MainActivity: FAB visibility: ${fab.visibility}")
        
        // FAB click listener - parking lot selection
        binding.fabBookParking.setOnClickListener {
            println("MainActivity: FAB clicked - navigating to ParkingLotSelectionActivity")
            try {
                // Navigate to parking lot selection first
                val intent = Intent(this, com.gridee.parking.ui.booking.ParkingLotSelectionActivity::class.java)
                println("MainActivity: Intent created, starting activity")
                startActivity(intent)
                println("MainActivity: Activity started successfully")
            } catch (e: Exception) {
                println("MainActivity: Error starting ParkingLotSelectionActivity: ${e.message}")
                e.printStackTrace()
                // Fallback to ParkingDiscoveryActivity if ParkingLotSelectionActivity fails
                try {
                    val fallbackIntent = Intent(this, com.gridee.parking.ui.discovery.ParkingDiscoveryActivity::class.java)
                    startActivity(fallbackIntent)
                    println("MainActivity: Fallback to ParkingDiscoveryActivity successful")
                } catch (fallbackException: Exception) {
                    println("MainActivity: Fallback also failed: ${fallbackException.message}")
                    showToast("Unable to open parking selection")
                }
            }
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
