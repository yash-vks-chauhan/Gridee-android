package com.gridee.parking.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.getSystemService
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced Gesture Handler for Bulky Glass Segmented Control
 * 
 * Features:
 * - Tap and drag gestures
 * - Smooth spring animations
 * - One-shot haptic feedback
 * - Accessibility support
 * - Edge handling and debouncing
 */
class EnhancedSegmentedControlGestureHandler(
    private val context: Context,
    private val containerView: View,
    private val indicatorView: View,
    private val segments: List<View>,
    private val onSelectionChanged: (Int) -> Unit,
    private val onAccessibilityAnnouncement: (String) -> Unit
) {
    
    // Configuration constants following the bulky spec
    private val ANIMATION_DURATION_MS = 240L // 220-260ms for weightier feel
    private val LABEL_TRANSITION_DURATION_MS = 140L // 140ms for label color cross-fade
    private val DRAG_THRESHOLD_DP = 8f // Minimum distance to treat as drag vs tap
    private val HAPTIC_DEBOUNCE_MS = 50L // Prevent double haptics
    
    // Gesture state
    private var isDragging = false
    private var initialTouchX = 0f
    private var lastHapticTime = 0L
    private var currentSelectedIndex = 0 // Default to first segment (Active) to match layout
    private var dragStartX = 0f
    private var hoveredSegmentIndex = currentSelectedIndex
    
    // Animation state
    private var currentAnimator: ValueAnimator? = null
    
    // System services
    private val vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val dragThresholdPx = DRAG_THRESHOLD_DP * context.resources.displayMetrics.density
    
    init {
        setupTouchListener()
    }
    
    /**
     * (1) Setup gesture detection - Entry point for touch handling
     */
    private fun setupTouchListener() {
        containerView.setOnTouchListener { _, event ->
            handleTouch(event)
        }
    }
    
    /**
     * Main touch handler supporting both tap and drag gestures
     */
    private fun handleTouch(event: MotionEvent): Boolean {
        android.util.Log.d("SegmentedControl", "Touch event: ${event.action}, x: ${event.x}")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                dragStartX = event.x
                isDragging = false
                hoveredSegmentIndex = currentSelectedIndex
                
                // Cancel any ongoing animations
                currentAnimator?.cancel()
                
                // (Visual affordance) Optional pressed state
                applyPressedState()
                
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(event.x - initialTouchX)
                
                if (!isDragging && deltaX > dragThresholdPx) {
                    // Start dragging
                    isDragging = true
                }
                
                if (isDragging) {
                    // (2) Indicator position updates - Track finger during drag
                    updateIndicatorPosition(event.x)
                }
                
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                releasePressedState()
                
                if (!isDragging) {
                    // Handle as tap
                    handleTap(event.x)
                } else {
                    // Handle drag release
                    handleDragRelease(event.x)
                }
                
                isDragging = false
                return true
            }
        }
        
        return false
    }
    
    /**
     * Handle tap gesture - Immediately select tapped segment
     */
    private fun handleTap(x: Float) {
        val tappedIndex = getSegmentIndexFromX(x)
        if (tappedIndex != -1 && tappedIndex != currentSelectedIndex) {
            selectSegment(tappedIndex, true)
        }
    }
    
    /**
     * Handle drag release - Snap to nearest segment
     */
    private fun handleDragRelease(x: Float) {
        val nearestIndex = getSegmentIndexFromX(x)
        if (nearestIndex != -1) {
            val didChange = nearestIndex != currentSelectedIndex
            selectSegment(nearestIndex, didChange)
        } else {
            // Snap back to current selection if dragged outside bounds
            animateToCurrentSelection()
        }
    }
    
    /**
     * (2) Update indicator position during drag - Clamped within container bounds
     */
    private fun updateIndicatorPosition(x: Float) {
        val indicatorMargin = 4f * context.resources.displayMetrics.density // 4dp indicator margin
        val indicatorWidth = indicatorView.width
        
        // Calculate boundaries based on actual segment positions
        val minX = segments[0].left.toFloat() + indicatorMargin
        val maxX = segments.last().right.toFloat() - indicatorWidth - indicatorMargin
        
        // Center the indicator on the touch point, but clamp to boundaries
        val targetX = x - indicatorWidth / 2
        val clampedX = max(minX, min(maxX, targetX))
        
        indicatorView.x = clampedX

        if (indicatorWidth > 0) {
            val indicatorCenter = clampedX + indicatorWidth / 2f
            val hoverIndex = getSegmentIndexFromX(indicatorCenter)
            if (hoverIndex != hoveredSegmentIndex) {
                hoveredSegmentIndex = hoverIndex
                if (hoverIndex != currentSelectedIndex) {
                    triggerHapticFeedback()
                }
            }
        }
    }
    
    /**
     * (3) Commit selection and trigger callbacks
     */
    private fun selectSegment(index: Int, triggerHaptic: Boolean = true) {
        android.util.Log.d("SegmentedControl", "selectSegment called: index=$index, current=$currentSelectedIndex")
        val oldIndex = currentSelectedIndex
        currentSelectedIndex = index
        hoveredSegmentIndex = index
        
        // Animate indicator to target position
        animateToSegment(index)
        
        // Trigger selection change callback
        android.util.Log.d("SegmentedControl", "Calling onSelectionChanged with index=$index")
        onSelectionChanged(index)
        
        // (3) Trigger haptic feedback on selection change
        if (triggerHaptic && oldIndex != index) {
            triggerHapticFeedback()
        }
        
        // (4) Announce accessibility change
        announceAccessibilityChange(index)
    }
    
    /**
     * Animate indicator to target segment with spring-like motion
     */
    private fun animateToSegment(index: Int) {
        if (index < 0 || index >= segments.size) return
        
        // Get the target segment view
        val targetSegment = segments[index]
        
        // Calculate target position based on the actual segment position
        val indicatorMargin = 4f * context.resources.displayMetrics.density // 4dp indicator margin
        
        // Target X position should align with the segment's start position
        val targetX = targetSegment.left.toFloat() + indicatorMargin
        
        currentAnimator?.cancel()
        
        // Weightier spring animation (damping 0.75-0.80, soft settle)
        currentAnimator = ValueAnimator.ofFloat(indicatorView.x, targetX).apply {
            duration = ANIMATION_DURATION_MS
            // Cubic-bezier(0.20, 0.85, 0.20, 1.00) equivalent for heavier settle
            interpolator = OvershootInterpolator(0.8f)
            
            addUpdateListener { animator ->
                indicatorView.x = animator.animatedValue as Float
            }
            
            doOnEnd {
                currentAnimator = null
                animateIndicatorBounce()
            }
        }
        
        currentAnimator?.start()
    }
    
    /**
     * Animate back to current selection (used when drag is cancelled)
     */
    private fun animateToCurrentSelection() {
        animateToSegment(currentSelectedIndex)
    }
    
    /**
     * Get segment index from X coordinate
     */
    private fun getSegmentIndexFromX(x: Float): Int {
        android.util.Log.d("SegmentedControl", "getSegmentIndexFromX: x=$x")
        // Find which segment contains the x coordinate
        for (i in segments.indices) {
            val segment = segments[i]
            android.util.Log.d("SegmentedControl", "Segment $i: left=${segment.left}, right=${segment.right}")
            if (x >= segment.left && x <= segment.right) {
                android.util.Log.d("SegmentedControl", "Found segment $i for x=$x")
                return i
            }
        }
        
        // If not directly over a segment, find the closest one
        var closestIndex = 0
        var closestDistance = Float.MAX_VALUE
        
        for (i in segments.indices) {
            val segment = segments[i]
            val segmentCenter = segment.left + (segment.right - segment.left) / 2f
            val distance = kotlin.math.abs(x - segmentCenter)
            
            if (distance < closestDistance) {
                closestDistance = distance
                closestIndex = i
            }
        }
        
        return closestIndex
    }
    
    /**
     * (3) Trigger one-shot haptic feedback with debouncing
     */
    private fun triggerHapticFeedback() {
        val currentTime = System.currentTimeMillis()
        
        // Debounce rapid taps
        if (currentTime - lastHapticTime < HAPTIC_DEBOUNCE_MS) {
            return
        }
        
        lastHapticTime = currentTime
        
        // Try modern haptic feedback first
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                containerView.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } else {
                // Fallback to older haptic feedback
                containerView.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        } catch (e: Exception) {
            // If system haptics fail, use vibrator as final fallback
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(10)
                }
            } catch (e2: Exception) {
                // Haptic feedback not available
            }
        }
    }
    
    /**
     * (4) Announce accessibility changes
     */
    private fun announceAccessibilityChange(index: Int) {
        val labels = listOf("Active", "Pending", "Completed")
        if (index in labels.indices) {
            onAccessibilityAnnouncement("Selected: ${labels[index]}")
        }
    }
    
    /**
     * Apply pressed visual state (optional dense press feel)
     */
    private fun applyPressedState() {
        indicatorView.animate()
            .alpha(0.92f) // Reduce alpha by ~0.08
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(50)
            .start()
    }
    
    /**
     * Release pressed visual state
     */
    private fun releasePressedState() {
        indicatorView.animate()
            .alpha(1.0f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(120)
            .start()
    }
    
    /**
     * Public API to set initial selection
     */
    fun setSelectedIndex(index: Int, animated: Boolean = false) {
        android.util.Log.d("SegmentedControl", "setSelectedIndex called: index=$index, animated=$animated")
        if (index in 0 until segments.size) {
            currentSelectedIndex = index
            hoveredSegmentIndex = index
            android.util.Log.d("SegmentedControl", "Setting current index to: $index")
            if (animated) {
                animateToSegment(index)
            } else {
                // Position indicator immediately using actual segment position
                val targetSegment = segments[index]
                val indicatorMargin = 4f * context.resources.displayMetrics.density // 4dp indicator margin
                val targetX = targetSegment.left.toFloat() + indicatorMargin
                android.util.Log.d("SegmentedControl", "Moving indicator to x=$targetX for segment $index")
                indicatorView.x = targetX
            }
        } else {
            android.util.Log.w("SegmentedControl", "Invalid index: $index (segments size: ${segments.size})")
        }
    }
    
    /**
     * Get current selected index
     */
    fun getSelectedIndex(): Int = currentSelectedIndex
    
    /**
     * Cleanup method
     */
    fun cleanup() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    private fun animateIndicatorBounce() {
        indicatorView.animate().cancel()
        indicatorView.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .setDuration(140)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                indicatorView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(OvershootInterpolator(0.9f))
                    .start()
            }
            .start()
    }
}
