package com.gridee.parking.ui.main

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityMainContainerBinding
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation
import com.gridee.parking.ui.fragments.BookingsFragmentNew
import com.gridee.parking.ui.fragments.HomeFragment
import com.gridee.parking.ui.fragments.ProfileFragment
import com.gridee.parking.ui.fragments.WalletFragmentNew

class MainContainerActivity : BaseActivityWithBottomNav<ActivityMainContainerBinding>() {

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val EXTRA_SHOW_PENDING = "extra_show_pending"
        const val EXTRA_HIGHLIGHT_BOOKING_ID = "extra_highlight_booking_id"
    }

    private var currentFragment: Fragment? = null
    private var currentTabId = CustomBottomNavigation.TAB_HOME

    // Fragment instances (create once, reuse for better performance)
    private val homeFragment by lazy { HomeFragment() }
    private val bookingsFragment by lazy { BookingsFragmentNew() }
    private val walletFragment by lazy { WalletFragmentNew() }
    private val profileFragment by lazy { ProfileFragment() }

    override fun getViewBinding(): ActivityMainContainerBinding {
        return ActivityMainContainerBinding.inflate(layoutInflater)
    }

    override fun getCurrentTab(): Int {
        return currentTabId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
                // Handle system window insets for proper edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to fragment container to avoid status bar overlap
            binding.fragmentContainer.updatePadding(top = systemBarsInsets.top)
            
            // Don't add bottom padding to bottom navigation to keep original size
            // The bottom nav will handle its own positioning
            
            insets
        }
        
        // Setup bottom navigation manually using binding
        setupBottomNavigationManually(binding.bottomNavigation)
        
        val initialTab = if (savedInstanceState == null) {
            intent?.getIntExtra(EXTRA_TARGET_TAB, CustomBottomNavigation.TAB_HOME)
                ?: CustomBottomNavigation.TAB_HOME
        } else {
            savedInstanceState.getInt("current_tab", CustomBottomNavigation.TAB_HOME)
        }

        if (savedInstanceState == null) {
            bottomNavigation.setActiveTab(initialTab)
            switchToFragment(getFragmentForTab(initialTab), initialTab)
        } else {
            currentTabId = initialTab
            bottomNavigation.setActiveTab(currentTabId)
            currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        }

        handleNavigationIntent(intent, currentTabId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("current_tab", currentTabId)
    }

    override fun setupUI() {
        // Setup any initial UI configuration
    }

    override fun onTabSelected(tabId: Int) {
        if (tabId == currentTabId) {
            // Same tab selected - scroll to top if fragment supports it
            scrollCurrentFragmentToTop()
            return
        }

        val targetFragment = getFragmentForTab(tabId)
        switchToFragment(targetFragment, tabId)
    }

    private fun switchToFragment(fragment: Fragment, tabId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        
        // Add smooth slide animations
        when {
            tabId > currentTabId -> {
                // Sliding right (forward)
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
            tabId < currentTabId -> {
                // Sliding left (backward)
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            }
            else -> {
                // Same position or initial load - use fade
                transaction.setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out
                )
            }
        }

        // Hide current fragment and show target fragment
        currentFragment?.let { transaction.hide(it) }
        
        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.fragment_container, fragment)
        }

        transaction.commitNowAllowingStateLoss()
        
        currentFragment = fragment
        currentTabId = tabId
        
        // Delay scroll behavior setup until fragment view is ready
        binding.fragmentContainer.post {
            setupScrollBehaviorForCurrentFragment()
        }
    }

    private fun scrollCurrentFragmentToTop() {
        currentFragment?.let { fragment ->
            when (fragment) {
                is HomeFragment -> fragment.scrollToTop()
                is BookingsFragmentNew -> fragment.scrollToTop()
                is WalletFragmentNew -> fragment.scrollToTop()
                is ProfileFragment -> fragment.scrollToTop()
                else -> {
                    // Handle unknown fragment types
                }
            }
        }
    }

    private fun setupScrollBehaviorForCurrentFragment() {
        currentFragment?.let { fragment ->
            when (fragment) {
                is HomeFragment -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                is BookingsFragmentNew -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                is WalletFragmentNew -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                is ProfileFragment -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                else -> {
                    // Handle unknown fragment types
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        intent ?: return

        setIntent(intent)

        val targetTab = intent.getIntExtra(EXTRA_TARGET_TAB, currentTabId)
        if (targetTab != currentTabId) {
            bottomNavigation.setActiveTab(targetTab)
            onTabSelected(targetTab)
        }

        handleNavigationIntent(intent, targetTab)
    }

    private fun getFragmentForTab(tabId: Int): Fragment {
        return when (tabId) {
            CustomBottomNavigation.TAB_HOME -> homeFragment
            CustomBottomNavigation.TAB_BOOKINGS -> bookingsFragment
            CustomBottomNavigation.TAB_WALLET -> walletFragment
            CustomBottomNavigation.TAB_PROFILE -> profileFragment
            else -> homeFragment
        }
    }

    private fun handleNavigationIntent(intent: android.content.Intent?, targetTab: Int) {
        if (targetTab != CustomBottomNavigation.TAB_BOOKINGS) return

        val showPending = intent?.getBooleanExtra(EXTRA_SHOW_PENDING, false) ?: false
        val highlightBookingId = intent?.getStringExtra(EXTRA_HIGHLIGHT_BOOKING_ID)
        bookingsFragment.handleExternalNavigation(showPending, highlightBookingId)
    }

    // Handle back button to navigate to home or exit
    override fun onBackPressed() {
        when (currentTabId) {
            CustomBottomNavigation.TAB_HOME -> {
                // Exit app from home
                super.onBackPressed()
            }
            else -> {
                // Navigate to home
                bottomNavigation.setActiveTab(CustomBottomNavigation.TAB_HOME)
                onTabSelected(CustomBottomNavigation.TAB_HOME)
            }
        }
    }
}
