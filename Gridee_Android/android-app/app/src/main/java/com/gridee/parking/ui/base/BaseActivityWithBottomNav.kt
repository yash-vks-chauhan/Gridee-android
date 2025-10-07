package com.gridee.parking.ui.base

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.gridee.parking.R
import com.gridee.parking.ui.MainActivity
import com.gridee.parking.ui.components.CustomBottomNavigation

abstract class BaseActivityWithBottomNav<T : ViewBinding> : AppCompatActivity(), 
    CustomBottomNavigation.OnTabSelectedListener {
    
    private var _binding: T? = null
    protected val binding get() = _binding!!
    
    protected lateinit var bottomNavigation: CustomBottomNavigation
    
    // Scroll behavior variables
    private var lastScrollY = 0
    private var scrollThreshold = 3  // More sensitive threshold like Twitter
    
    abstract fun getViewBinding(): T
    abstract fun getCurrentTab(): Int
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        setupEdgeToEdge()
        
        _binding = getViewBinding()
        setContentView(binding.root)
        
        // Apply window insets to the root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            // Don't apply any padding to the root - let child views handle it
            insets
        }
        
        setupBottomNavigation()
        setupScrollBehavior()
        setupUI()
    }
    
    private fun setupEdgeToEdge() {
        // Enable edge-to-edge for modern Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Disable navigation bar contrast enforcement to prevent gray areas
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
            
            // Set transparent navigation bar
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Disable navigation bar contrast enforcement (API 29+)
            window.isNavigationBarContrastEnforced = false
            
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
        } else {
            // For older versions, use deprecated approach
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
            
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        // Force the navigation bar to be completely transparent
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Configure light icons for status and navigation bars
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
    
    private fun setupBottomNavigation() {
        try {
            bottomNavigation = findViewById(R.id.bottom_navigation)
            bottomNavigation.setOnTabSelectedListener(this)
            bottomNavigation.setActiveTab(getCurrentTab())
        } catch (e: Exception) {
            // Will be handled by subclass if findViewById fails
            // This allows subclasses to override navigation setup
        }
    }
    
    protected fun setupBottomNavigationManually(navigation: CustomBottomNavigation) {
        bottomNavigation = navigation
        bottomNavigation.setOnTabSelectedListener(this)
        bottomNavigation.setActiveTab(getCurrentTab())
    }
    
    private fun setupScrollBehavior() {
        // This method can be overridden by subclasses to implement scroll behavior
        // For now, we'll implement a simple version that activities can use
    }
    
    // Method that activities can call to setup scroll behavior for specific views
    protected fun setupScrollBehaviorForView(scrollableView: View) {
        when (scrollableView) {
            is NestedScrollView -> {
                scrollableView.setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
                    val deltaY = scrollY - oldScrollY
                    handleScrollDelta(deltaY)
                }
            }
            is ScrollView -> {
                // For regular ScrollView, use a custom scroll detection approach
                scrollableView.viewTreeObserver.addOnScrollChangedListener {
                    val currentScrollY = scrollableView.scrollY
                    val deltaY = currentScrollY - lastScrollY
                    
                    println("ScrollView: currentScrollY=$currentScrollY, lastScrollY=$lastScrollY, deltaY=$deltaY, threshold=$scrollThreshold")
                    
                    if (kotlin.math.abs(deltaY) > scrollThreshold) {
                        handleScrollDelta(deltaY)
                        lastScrollY = currentScrollY
                    }
                }
                
                // Also try using touch listener as backup
                var startY = 0f
                scrollableView.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startY = event.y
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val currentY = event.y
                            val deltaY = startY - currentY
                            if (kotlin.math.abs(deltaY) > 50) { // Touch threshold
                                handleScrollDelta(deltaY.toInt())
                                startY = currentY
                            }
                        }
                    }
                    false // Don't consume the touch event
                }
            }
            is RecyclerView -> {
                scrollableView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        handleScrollDelta(dy)
                    }
                })
            }
        }
    }
    
    private fun handleScrollDelta(deltaY: Int) {
        // Disabled scroll behavior to prevent content bleeding through
        // Add debug logging
        println("ScrollDelta: $deltaY, threshold: $scrollThreshold")
        
        // Commenting out the hide/show behavior
        /*
        if (deltaY > scrollThreshold) {
            // Scrolling down - hide navigation
            println("Hiding navigation - scrolling down")
            bottomNavigation.hideBottomNavigation()
        } else if (deltaY < -scrollThreshold) {
            // Scrolling up - show navigation
            println("Showing navigation - scrolling up")
            bottomNavigation.showBottomNavigation()
        }
        */
    }
    
    @Deprecated("Use handleScrollDelta instead")
    private fun handleScroll(scrollY: Int) {
        val deltaY = scrollY - lastScrollY
        handleScrollDelta(deltaY)
        lastScrollY = scrollY
    }
    
    override fun onTabSelected(tabId: Int) {
        if (tabId == getCurrentTab()) {
            return // Already on this tab
        }
        
        when (tabId) {
            CustomBottomNavigation.TAB_HOME -> {
                if (this !is MainActivity) {
                    navigateToActivity(MainActivity::class.java)
                }
            }
            CustomBottomNavigation.TAB_BOOKINGS -> {
                if (this::class.java.simpleName != "BookingsActivity") {
                    navigateToActivity(Class.forName("com.gridee.parking.ui.bookings.BookingsActivity"))
                }
            }
            CustomBottomNavigation.TAB_WALLET -> {
                if (this::class.java.simpleName != "WalletActivity") {
                    navigateToActivity(Class.forName("com.gridee.parking.ui.wallet.WalletActivity"))
                }
            }
            CustomBottomNavigation.TAB_PROFILE -> {
                if (this::class.java.simpleName != "ProfileActivity") {
                    navigateToActivity(Class.forName("com.gridee.parking.ui.profile.ProfileActivity"))
                }
            }
        }
    }
    
    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        // Preserve user data if needed
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "User")
        intent.putExtra("USER_NAME", userName)
        
        startActivity(intent)
        overridePendingTransition(0, 0) // No animation for tab navigation
        finish()
    }
    
    protected fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    // Method to manually control navigation visibility
    protected fun hideBottomNavigation() {
        bottomNavigation.hideBottomNavigation()
    }
    
    protected fun showBottomNavigation() {
        bottomNavigation.showBottomNavigation()
    }
    
    abstract fun setupUI()
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
