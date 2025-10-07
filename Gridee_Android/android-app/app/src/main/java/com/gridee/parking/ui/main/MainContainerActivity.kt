package com.gridee.parking.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityMainContainerBinding
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation
import com.gridee.parking.ui.fragments.BookingsFragment
import com.gridee.parking.ui.fragments.HomeFragment
import com.gridee.parking.ui.fragments.ProfileFragment
import com.gridee.parking.ui.fragments.WalletFragment

class MainContainerActivity : BaseActivityWithBottomNav<ActivityMainContainerBinding>() {

    private var currentFragment: Fragment? = null
    private var currentTabId = CustomBottomNavigation.TAB_HOME

    // Fragment instances (create once, reuse for better performance)
    private val homeFragment by lazy { HomeFragment() }
    private val bookingsFragment by lazy { BookingsFragment() }
    private val walletFragment by lazy { WalletFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    override fun getViewBinding(): ActivityMainContainerBinding {
        return ActivityMainContainerBinding.inflate(layoutInflater)
    }

    override fun getCurrentTab(): Int {
        return currentTabId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup bottom navigation manually using binding
        setupBottomNavigationManually(binding.bottomNavigation)
        
        // Load initial fragment if no saved state
        if (savedInstanceState == null) {
            switchToFragment(homeFragment, CustomBottomNavigation.TAB_HOME)
        } else {
            // Restore current tab from saved state
            currentTabId = savedInstanceState.getInt("current_tab", CustomBottomNavigation.TAB_HOME)
        }
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

        val targetFragment = when (tabId) {
            CustomBottomNavigation.TAB_HOME -> homeFragment
            CustomBottomNavigation.TAB_BOOKINGS -> bookingsFragment
            CustomBottomNavigation.TAB_WALLET -> walletFragment
            CustomBottomNavigation.TAB_PROFILE -> profileFragment
            else -> homeFragment
        }

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
                is BookingsFragment -> fragment.scrollToTop()
                is WalletFragment -> fragment.scrollToTop()
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
                is BookingsFragment -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                is WalletFragment -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                is ProfileFragment -> fragment.getScrollableView()?.let { setupScrollBehaviorForView(it) }
                else -> {
                    // Handle unknown fragment types
                }
            }
        }
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
