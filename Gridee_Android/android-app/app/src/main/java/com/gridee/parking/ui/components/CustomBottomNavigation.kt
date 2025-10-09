package com.gridee.parking.ui.components

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.gridee.parking.R

class CustomBottomNavigation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface OnTabSelectedListener {
        fun onTabSelected(tabId: Int)
    }

    companion object {
        const val TAB_HOME = 0
        const val TAB_BOOKINGS = 1
        const val TAB_WALLET = 2
        const val TAB_PROFILE = 3
    }

    private var onTabSelectedListener: OnTabSelectedListener? = null
    private var currentTab = TAB_HOME

    // Views
    private lateinit var tabHome: FrameLayout
    private lateinit var tabBookings: FrameLayout
    private lateinit var tabWallet: FrameLayout
    private lateinit var tabProfile: FrameLayout

    private lateinit var ivHome: ImageView
    private lateinit var ivBookings: ImageView
    private lateinit var ivWallet: ImageView
    private lateinit var ivProfile: ImageView

    private lateinit var tvHome: TextView
    private lateinit var tvBookings: TextView
    private lateinit var tvWallet: TextView
    private lateinit var tvProfile: TextView

    // Active background views
    private lateinit var homeActiveBg: View
    private lateinit var bookingsActiveBg: View
    private lateinit var walletActiveBg: View
    private lateinit var profileActiveBg: View
    
    // Animation properties
    private var currentIndicatorAnimation: SpringAnimation? = null
    private var isHidden = false
    
    // Haptic feedback
    private val vibrator: Vibrator? by lazy { context.getSystemService<Vibrator>() }
    
    // Animation durations and properties
    private val scaleAnimationDuration = 150L
    private val bounceAnimationDuration = 200L

    init {
        LayoutInflater.from(context).inflate(R.layout.custom_bottom_navigation, this, true)
        
        // Enable hardware acceleration for premium performance
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // Handle window insets for edge-to-edge support
        setupWindowInsets()
        
        initViews()
        setupClickListeners()
        setActiveTab(TAB_HOME)
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Force the view to extend to the very bottom edge
            view.setPadding(0, 0, 0, 0)
            
            // Apply padding only to the content container
            val navContainer = findViewById<LinearLayout>(R.id.nav_container)
            navContainer?.let { container ->
                // Reduce top padding and optimize bottom padding
                val bottomPadding = Math.max(navigationBars.bottom, 20) // Minimum 20dp for gesture area
                container.setPadding(
                    container.paddingLeft,
                    4, // Reduced top padding to 4dp
                    container.paddingRight,
                    bottomPadding + 8 // Smaller buffer for bottom
                )
            }
            
            // Reduce minimum height to make navigation bar more compact
            view.minimumHeight = 64 + Math.max(navigationBars.bottom, 32)
            
            // Force layout to extend to bottom
            val layoutParams = view.layoutParams
            if (layoutParams != null) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                view.layoutParams = layoutParams
            }
            
            insets
        }
    }

    private fun initViews() {
        tabHome = findViewById(R.id.tab_home)
        tabBookings = findViewById(R.id.tab_bookings)
        tabWallet = findViewById(R.id.tab_wallet)
        tabProfile = findViewById(R.id.tab_profile)

        ivHome = findViewById(R.id.iv_home)
        ivBookings = findViewById(R.id.iv_bookings)
        ivWallet = findViewById(R.id.iv_wallet)
        ivProfile = findViewById(R.id.iv_profile)

        tvHome = findViewById(R.id.tv_home)
        tvBookings = findViewById(R.id.tv_bookings)
        tvWallet = findViewById(R.id.tv_wallet)
        tvProfile = findViewById(R.id.tv_profile)
        
        // Active background views
        homeActiveBg = findViewById(R.id.home_active_bg)
        bookingsActiveBg = findViewById(R.id.bookings_active_bg)
        walletActiveBg = findViewById(R.id.wallet_active_bg)
        profileActiveBg = findViewById(R.id.profile_active_bg)
    }

    private fun setupClickListeners() {
        tabHome.setOnClickListener { selectTab(TAB_HOME) }
        tabBookings.setOnClickListener { selectTab(TAB_BOOKINGS) }
        tabWallet.setOnClickListener { selectTab(TAB_WALLET) }
        tabProfile.setOnClickListener { selectTab(TAB_PROFILE) }
    }

    private fun performHapticFeedback() {
        // Try modern haptic feedback first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(10)
        }
    }

    private fun selectTab(tabId: Int) {
        if (currentTab != tabId) {
            // Add haptic feedback for tab selection
            performHapticFeedback()
            
            setActiveTab(tabId)
            onTabSelectedListener?.onTabSelected(tabId)
        }
    }

    fun setActiveTab(tabId: Int) {
        currentTab = tabId
        
        // Reset all tabs to inactive state
        resetAllTabs()
        
        // Activate selected tab with elegant transitions
        when (tabId) {
            TAB_HOME -> activateHomeTab()
            TAB_BOOKINGS -> activateBookingsTab()
            TAB_WALLET -> activateWalletTab()
            TAB_PROFILE -> activateProfileTab()
        }
    }

    private fun resetAllTabs() {
        deactivateTab(homeActiveBg, ivHome, tvHome)
        deactivateTab(bookingsActiveBg, ivBookings, tvBookings)
        deactivateTab(walletActiveBg, ivWallet, tvWallet)
        deactivateTab(profileActiveBg, ivProfile, tvProfile)
    }

    private fun deactivateAllTabs() {
        deactivateTab(homeActiveBg, ivHome, tvHome)
        deactivateTab(bookingsActiveBg, ivBookings, tvBookings)
        deactivateTab(walletActiveBg, ivWallet, tvWallet)
        deactivateTab(profileActiveBg, ivProfile, tvProfile)
    }

    private fun activateHomeTab() {
        activateTab(homeActiveBg, ivHome, tvHome)
    }

    private fun activateBookingsTab() {
        activateTab(bookingsActiveBg, ivBookings, tvBookings)
    }

    private fun activateWalletTab() {
        activateTab(walletActiveBg, ivWallet, tvWallet)
    }

    private fun activateProfileTab() {
        activateTab(profileActiveBg, ivProfile, tvProfile)
    }

    private fun activateTab(backgroundView: View, imageView: ImageView, textView: TextView) {
        // Don't show background - keep it hidden
        backgroundView.visibility = View.GONE
        
        // Set active ripple effect for the parent tab
        val parentTab = imageView.parent.parent as FrameLayout
        parentTab.setBackgroundResource(R.drawable.tab_ripple_effect)
        
        // Switch to filled icons for active state and update colors to black
        when (imageView) {
            ivHome -> {
                imageView.setImageResource(R.drawable.ic_home_filled)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black))
            }
            ivBookings -> {
                imageView.setImageResource(R.drawable.ic_bookings_filled)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black))
            }
            ivWallet -> {
                imageView.setImageResource(R.drawable.ic_wallet_filled)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black))
            }
            ivProfile -> {
                imageView.setImageResource(R.drawable.ic_account_filled)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.black))
            }
        }
        
        textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        textView.visibility = View.VISIBLE
        
        // Add smooth bounce animation for active tab
        animateTabActivation(imageView, textView)
    }

    private fun deactivateTab(backgroundView: View, imageView: ImageView, textView: TextView) {
        // Hide background
        backgroundView.visibility = View.GONE
        backgroundView.alpha = 0f
        
        // Set default ripple effect for the parent tab
        val parentTab = imageView.parent.parent as FrameLayout
        parentTab.setBackgroundResource(R.drawable.tab_ripple_effect)
        
        // Switch to outline icons for inactive state and update colors to gray
        when (imageView) {
            ivHome -> {
                imageView.setImageResource(R.drawable.ic_home)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.charcoal_black))
            }
            ivBookings -> {
                imageView.setImageResource(R.drawable.ic_bookings)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.charcoal_black))
            }
            ivWallet -> {
                imageView.setImageResource(R.drawable.ic_wallet)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.charcoal_black))
            }
            ivProfile -> {
                imageView.setImageResource(R.drawable.ic_account)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.charcoal_black))
            }
        }
        
        textView.setTextColor(ContextCompat.getColor(context, R.color.charcoal_black))
        textView.visibility = View.VISIBLE
        
        // Add smooth deactivation animation
        animateTabDeactivation(imageView, textView)
    }

    private fun animateTabActivation(imageView: ImageView, textView: TextView) {
        // Create a subtle bounce animation for the icon
        val scaleUpX = ObjectAnimator.ofFloat(imageView, "scaleX", 1.0f, 1.15f)
        val scaleUpY = ObjectAnimator.ofFloat(imageView, "scaleY", 1.0f, 1.15f)
        val scaleDownX = ObjectAnimator.ofFloat(imageView, "scaleX", 1.15f, 1.0f)
        val scaleDownY = ObjectAnimator.ofFloat(imageView, "scaleY", 1.15f, 1.0f)
        
        // Add alpha animation for smooth appearance
        val alphaUp = ObjectAnimator.ofFloat(imageView, "alpha", 0.7f, 1.0f)
        val alphaDown = ObjectAnimator.ofFloat(imageView, "alpha", 1.0f, 1.0f)
        
        // Animate text with subtle scale
        val textScaleUpX = ObjectAnimator.ofFloat(textView, "scaleX", 1.0f, 1.05f)
        val textScaleUpY = ObjectAnimator.ofFloat(textView, "scaleY", 1.0f, 1.05f)
        val textScaleDownX = ObjectAnimator.ofFloat(textView, "scaleX", 1.05f, 1.0f)
        val textScaleDownY = ObjectAnimator.ofFloat(textView, "scaleY", 1.05f, 1.0f)
        val textAlpha = ObjectAnimator.ofFloat(textView, "alpha", 0.8f, 1.0f)
        
        // Configure scale up animation
        val scaleUpSet = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, textScaleUpX, textScaleUpY, alphaUp, textAlpha)
            duration = scaleAnimationDuration
            interpolator = DecelerateInterpolator()
        }
        
        // Configure scale down animation with overshoot for bounce effect
        val scaleDownSet = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, textScaleDownX, textScaleDownY, alphaDown)
            duration = bounceAnimationDuration
            interpolator = OvershootInterpolator(1.2f)
        }
        
        // Chain animations
        val animationSet = AnimatorSet()
        animationSet.playSequentially(scaleUpSet, scaleDownSet)
        animationSet.start()
    }
    
    private fun animateTabDeactivation(imageView: ImageView, textView: TextView) {
        // Simple scale down animation for deactivated tabs with alpha
        val scaleDownX = ObjectAnimator.ofFloat(imageView, "scaleX", imageView.scaleX, 1.0f)
        val scaleDownY = ObjectAnimator.ofFloat(imageView, "scaleY", imageView.scaleY, 1.0f)
        val alphaFade = ObjectAnimator.ofFloat(imageView, "alpha", imageView.alpha, 0.8f)
        
        val textScaleDownX = ObjectAnimator.ofFloat(textView, "scaleX", textView.scaleX, 1.0f)
        val textScaleDownY = ObjectAnimator.ofFloat(textView, "scaleY", textView.scaleY, 1.0f)
        val textAlphaFade = ObjectAnimator.ofFloat(textView, "alpha", textView.alpha, 0.9f)
        
        val animationSet = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, textScaleDownX, textScaleDownY, alphaFade, textAlphaFade)
            duration = scaleAnimationDuration
            interpolator = DecelerateInterpolator()
        }
        
        animationSet.start()
    }

    fun setOnTabSelectedListener(listener: OnTabSelectedListener) {
        this.onTabSelectedListener = listener
    }

    fun getCurrentTab(): Int = currentTab

    // Professional scroll behavior with refined animations
    fun hideBottomNavigation() {
        if (!isHidden) {
            isHidden = true
            animate()
                .translationY(height.toFloat())
                .alpha(0.0f)
                .setDuration(350)
                .setInterpolator(DecelerateInterpolator(2.0f))
                .start()
        }
    }

    fun showBottomNavigation() {
        if (isHidden) {
            isHidden = false
            animate()
                .translationY(0f)
                .alpha(1.0f)
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
        }
    }

    fun isNavigationHidden(): Boolean = isHidden
}
