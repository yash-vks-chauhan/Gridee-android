package com.gridee.parking.ui.components

import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.view.KeyEvent
import android.view.VelocityTracker
import android.view.animation.PathInterpolator
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
    private val ANIMATION_DURATION_MS = 220L // Slightly snappier, still smooth
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
    private var lastUpdateTime = 0L
    private var velocityTracker: VelocityTracker? = null
    private var lastReleaseVelocityX: Float = 0f
    private var lastPressedSegmentIndex: Int = -1
    private val density = context.resources.displayMetrics.density
    
    init {
        setupTouchListener()
    }
    
    /**
     * (1) Setup gesture detection - Entry point for touch handling
     */
    private fun setupTouchListener() {
        // Enable keyboard focus
        containerView.isFocusableInTouchMode = true
        containerView.requestFocus()

        containerView.setOnTouchListener { _, event ->
            handleTouch(event)
        }

        // Keyboard navigation (DPAD)
        containerView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (currentSelectedIndex > 0) {
                        selectSegment(currentSelectedIndex - 1, true, null)
                        true
                    } else false
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (currentSelectedIndex < segments.size - 1) {
                        selectSegment(currentSelectedIndex + 1, true, null)
                        true
                    } else false
                }
                else -> false
            }
        }
    }
    
    /**
     * Main touch handler supporting both tap and drag gestures
     */
    private fun handleTouch(event: MotionEvent): Boolean {
        android.util.Log.d("SegmentedControl", "Touch event: ${event.action}, x: ${event.x}")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                initialTouchX = event.x
                dragStartX = event.x
                isDragging = false
                hoveredSegmentIndex = currentSelectedIndex
                
                // Cancel any ongoing animations
                currentAnimator?.cancel()
                
                // (Visual affordance) Optional pressed state
                applyPressedState()

                // Press ripple on segment under finger + subtle press scale
                lastPressedSegmentIndex = getSegmentIndexFromX(event.x)
                if (lastPressedSegmentIndex in segments.indices) {
                    val seg = segments[lastPressedSegmentIndex]
                    seg.isPressed = true
                    animateSegmentScale(seg, 0.98f, 80L)
                }
                
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(event.x - initialTouchX)
                velocityTracker?.addMovement(event)
                
                if (!isDragging && deltaX > dragThresholdPx) {
                    // Start dragging
                    isDragging = true
                }
                
                if (isDragging) {
                    // (2) Indicator position updates - Track finger during drag
                    updateIndicatorPosition(event.x)
                    // Update pressed ripple to hovered segment
                    val hover = getSegmentIndexFromX(event.x)
                    if (hover != lastPressedSegmentIndex) {
                        if (lastPressedSegmentIndex in segments.indices) {
                            val prev = segments[lastPressedSegmentIndex]
                            prev.isPressed = false
                            animateSegmentScale(prev, 1.0f, 80L)
                        }
                        lastPressedSegmentIndex = hover
                        if (lastPressedSegmentIndex in segments.indices) {
                            val nowSeg = segments[lastPressedSegmentIndex]
                            nowSeg.isPressed = true
                            animateSegmentScale(nowSeg, 0.98f, 80L)
                        }
                    }
                }
                
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                releasePressedState()
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                lastReleaseVelocityX = velocityTracker?.xVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null
                
                if (!isDragging) {
                    // Handle as tap
                    handleTap(event.x)
                } else {
                    // Handle drag release
                    handleDragRelease(event.x)
                }
                
                // Clear ripples
                if (lastPressedSegmentIndex in segments.indices) {
                    val seg = segments[lastPressedSegmentIndex]
                    seg.isPressed = false
                    animateSegmentScale(seg, 1.0f, 120L)
                }
                lastPressedSegmentIndex = -1

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
            selectSegment(nearestIndex, didChange, lastReleaseVelocityX)
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
        // Throttle to ~60 FPS
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime >= 16) {
            indicatorView.x = clampedX
            // Subtle stretch while dragging for tactile feel
            indicatorView.scaleX = 1.02f
            updateIndicatorCornerRadiusForDrag()
            lastUpdateTime = now
        }
        // Subtle stretch while dragging for tactile feel
        indicatorView.scaleX = 1.02f
        updateIndicatorCornerRadiusForDrag()

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
    private fun selectSegment(index: Int, triggerHaptic: Boolean = true, releaseVelocityX: Float? = null) {
        android.util.Log.d("SegmentedControl", "selectSegment called: index=$index, current=$currentSelectedIndex")
        val oldIndex = currentSelectedIndex
        currentSelectedIndex = index
        hoveredSegmentIndex = index
        
        // Animate indicator to target position
        animateToSegment(index, releaseVelocityX)
        
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
    private fun animateToSegment(index: Int, releaseVelocityX: Float? = null) {
        if (index < 0 || index >= segments.size) return
        
        // Get the target segment view
        val targetSegment = segments[index]
        
        // Calculate target position based on the actual segment position
        val indicatorMargin = 4f * context.resources.displayMetrics.density // 4dp indicator margin
        
        // Target X position should align with the segment's start position
        val targetX = targetSegment.left.toFloat() + indicatorMargin
        
        currentAnimator?.cancel()
        
        // Compute duration factoring distance and release velocity
        val distance = kotlin.math.abs(indicatorView.x - targetX)
        val duration = computeAdaptiveDuration(distance, releaseVelocityX ?: 0f)

        // iOS-like cubic easing for refined settle
        currentAnimator = ValueAnimator.ofFloat(indicatorView.x, targetX).apply {
            this.duration = duration
            // Match iOS segmented control feel: cubic-bezier(0.2, 0.8, 0.2, 1.0)
            interpolator = PathInterpolator(0.2f, 0.8f, 0.2f, 1.0f)
            
            addUpdateListener { animator ->
                indicatorView.x = animator.animatedValue as Float
            }
            
            doOnEnd {
                currentAnimator = null
                // Ensure stretch is reset after animation completes
                indicatorView.scaleX = 1f
                indicatorView.setLayerType(View.LAYER_TYPE_NONE, null)
                // Restore perfect pill radius and add subtle bounce to feel springy
                setIndicatorCornerRadius(baseIndicatorCornerRadius())
                animateIndicatorBounce()
            }
        }
        // Improve perf during animation
        indicatorView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        currentAnimator?.start()
    }

    private fun computeAdaptiveDuration(distancePx: Float, velocityX: Float): Long {
        // px per second
        val v = kotlin.math.abs(velocityX)
        val base = ANIMATION_DURATION_MS
        // Distance factor: shorter distances settle quicker
        val distanceFactor = when {
            distancePx < 10f -> -40L
            distancePx < 30f -> -20L
            else -> 0L
        }
        // Velocity factor: faster swipes settle quicker
        val velocityFactor = when {
            v > 2200f -> -50L
            v > 1400f -> -30L
            v < 200f -> +20L
            else -> 0L
        }
        return (base + distanceFactor + velocityFactor).coerceIn(160L, 260L)
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
            .alpha(0.96f)
            .scaleX(0.99f)
            .scaleY(0.99f)
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
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(120)
            .setInterpolator(OvershootInterpolator(1.0f))
            .withEndAction {
                indicatorView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140)
                    .setInterpolator(OvershootInterpolator(0.9f))
                    .start()
            }
            .start()
    }

    /**
     * Subtle press scale for segment views
     */
    private fun animateSegmentScale(view: View, scale: Float, duration: Long) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .start()
    }

    /**
     * Smoothly adjust indicator corner radius while dragging to keep visual polish
     */
    private fun updateIndicatorCornerRadiusForDrag() {
        val base = baseIndicatorCornerRadius()
        val stretch = (indicatorView.scaleX - 1f).coerceIn(0f, 0.06f)
        // Up to ~2dp reduction at max stretch
        val reduction = (2f * density) * (stretch / 0.06f)
        setIndicatorCornerRadius((base - reduction).coerceAtLeast(0f))
    }

    private fun baseIndicatorCornerRadius(): Float = (indicatorView.height / 2f).coerceAtLeast(0f)

    private fun setIndicatorCornerRadius(radius: Float) {
        val bg = indicatorView.background
        if (bg is LayerDrawable) {
            for (i in 0 until bg.numberOfLayers) {
                val d = bg.getDrawable(i)
                if (d is GradientDrawable) {
                    d.cornerRadius = radius
                }
            }
        } else if (bg is GradientDrawable) {
            bg.cornerRadius = radius
        }
    }
}
