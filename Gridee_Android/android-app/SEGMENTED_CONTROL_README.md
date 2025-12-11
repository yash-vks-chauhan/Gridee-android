# iOS-Style Segmented Control for Android

A professional, fully-featured segmented control implementation that matches iOS design specifications and behavior patterns.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [File Structure](#file-structure)
- [Implementation Details](#implementation-details)
- [Usage Guide](#usage-guide)
- [Customization](#customization)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)

## 🎯 Overview

This segmented control provides a native iOS-like experience on Android with:
- **Exact iOS specifications**: 52dp height, 26dp corner radius, 330dp width
- **Professional glass morphism styling** with translucent backgrounds
- **Advanced gesture handling** supporting both tap and drag interactions
- **Smooth spring animations** with 240ms duration for weighty feel
- **Haptic feedback** and accessibility support
- **Responsive indicator** that follows touch gestures

## ✨ Features

### Visual Design
- ✅ **iOS-compliant dimensions**: Height 52dp, radius 26dp, width 330dp
- ✅ **44dp indicator height** (container height minus 8dp margin)
- ✅ **Glass morphism effects** with translucent fills and proper shadows
- ✅ **Professional corner radius hierarchy** for nested elements
- ✅ **Consistent 4dp margins** for indicator positioning

### Interaction
- ✅ **Tap gesture support** - immediate segment selection
- ✅ **Drag gesture support** - fluid indicator movement
- ✅ **Touch threshold detection** (8dp minimum for drag vs tap)
- ✅ **Haptic feedback** with 50ms debouncing
- ✅ **Visual pressed states** (0.8f alpha during interaction)

### Animation
- ✅ **Spring-like motion** with 240ms duration
- ✅ **ValueAnimator-based transitions** for smooth movement
- ✅ **Real-time indicator tracking** during drag operations
- ✅ **Snap-to-segment behavior** with proper bounds checking

### Accessibility
- ✅ **Screen reader announcements** for segment changes
- ✅ **Semantic content descriptions** for each segment
- ✅ **Keyboard navigation support** (future enhancement ready)

## 🏗 Architecture

### Core Components

```
EnhancedSegmentedControlGestureHandler (Main Logic)
├── Touch Event Processing
├── Gesture Recognition (Tap vs Drag)
├── Animation Management
├── Haptic Feedback
└── Accessibility Support

segment_bookings_ios.xml (Layout)
├── Container (ConstraintLayout)
├── Background Drawable
├── Three Segments (LinearLayout)
└── Sliding Indicator (View)

Glass Morphism Drawables
├── glass_segment_container.xml
├── glass_segment_indicator.xml
└── ultra_status_background.xml
```

### Data Flow

```
User Touch → EnhancedSegmentedControlGestureHandler → Animation → Callback → Fragment → Data Filtering
```

## 📁 File Structure

### Primary Implementation Files

#### Core Logic
- **`EnhancedSegmentedControlGestureHandler.kt`** - Main gesture handling and animation logic
- **`BookingsFragmentNew.kt`** - Integration example with fragment lifecycle

#### Layout Files
- **`segment_bookings_ios.xml`** - iOS-style segmented control layout
- **`fragment_bookings_new.xml`** - Host fragment layout

#### Drawable Resources
- **`glass_segment_container.xml`** - Container background with glass morphism
- **`glass_segment_indicator.xml`** - Sliding indicator styling  
- **`ultra_status_background.xml`** - Status container backgrounds

#### Data Models
- **`BookingStatus.kt`** - Enum for segment states
- **`UIBooking.kt`** - UI data model for bookings

## 🔧 Implementation Details

### 1. Gesture Handler Core Logic

```kotlin
class EnhancedSegmentedControlGestureHandler(
    private val context: Context,
    private val containerView: View,
    private val indicatorView: View,
    private val segments: List<View>,
    private val onSelectionChanged: (Int) -> Unit,
    private val onAccessibilityAnnouncement: (String) -> Unit
)
```

**Key Constants:**
```kotlin
private val ANIMATION_DURATION_MS = 240L        // Weighty animation feel
private val DRAG_THRESHOLD_DP = 8f              // Tap vs drag detection
private val HAPTIC_DEBOUNCE_MS = 50L            // Prevent double haptics
```

### 2. Touch Event Processing

```kotlin
private fun handleTouch(event: MotionEvent): Boolean {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> { /* Initialize touch tracking */ }
        MotionEvent.ACTION_MOVE -> { /* Handle drag gestures */ }
        MotionEvent.ACTION_UP -> { /* Complete gesture, trigger animations */ }
    }
}
```

### 3. Animation System

**Indicator Positioning:**
```kotlin
private fun animateToSegment(targetIndex: Int) {
    val targetSegment = segments[targetIndex]
    val indicatorMargin = 4f * context.resources.displayMetrics.density
    val targetX = targetSegment.left.toFloat() + indicatorMargin
    
    ValueAnimator.ofFloat(startX, targetX).apply {
        duration = ANIMATION_DURATION_MS
        addUpdateListener { animator ->
            indicatorView.x = animator.animatedValue as Float
        }
        doOnEnd {
            onSelectionChanged(targetIndex)
            triggerHapticFeedback()
        }
        start()
    }
}
```

### 4. Layout Specifications

**Container Constraints:**
```xml
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="330dp"
    android:layout_height="52dp"
    android:background="@drawable/glass_segment_container">
```

**Indicator Specifications:**
```xml
<View
    android:id="@+id/segment_indicator"
    android:layout_width="102dp"
    android:layout_height="44dp"
    android:layout_margin="4dp"
    android:background="@drawable/glass_segment_indicator" />
```

**Segment Layout:**
```xml
<LinearLayout
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:gravity="center"
    android:orientation="horizontal">
```

### 5. Glass Morphism Implementation

**Container Background:**
```xml
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#1AFFFFFF" />
            <corners android:radius="26dp" />
        </shape>
    </item>
</layer-list>
```

## 📚 Usage Guide

### Basic Integration

1. **Add to Layout:**
```xml
<include
    layout="@layout/segment_bookings_ios"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

2. **Initialize in Fragment:**
```kotlin
private fun setupEnhancedGestureHandler() {
    val segments = listOf(
        binding.segmentedControlContainer.segmentActive,
        binding.segmentedControlContainer.segmentPending,
        binding.segmentedControlContainer.segmentCompleted
    )
    
    gestureHandler = EnhancedSegmentedControlGestureHandler(
        context = requireContext(),
        containerView = binding.segmentedControlContainer.segmentContainer,
        indicatorView = binding.segmentedControlContainer.segmentIndicator,
        segments = segments,
        onSelectionChanged = { index ->
            handleSegmentSelection(index)
        },
        onAccessibilityAnnouncement = { message ->
            announceForAccessibility(message)
        }
    )
    
    // Set initial selection
    gestureHandler.setSelectedIndex(0, animated = false)
}
```

3. **Handle Selection Changes:**
```kotlin
private fun handleSegmentSelection(index: Int) {
    val newStatus = when (index) {
        0 -> BookingStatus.ACTIVE
        1 -> BookingStatus.PENDING
        2 -> BookingStatus.COMPLETED
        else -> BookingStatus.ACTIVE
    }
    
    if (newStatus != currentTab) {
        currentTab = newStatus
        filterBookingsByStatus(newStatus)
        showToast("Switched to ${getSegmentTitle(newStatus)}")
    }
}
```

### Advanced Configuration

**Custom Animation Duration:**
```kotlin
// Modify in EnhancedSegmentedControlGestureHandler.kt
private val ANIMATION_DURATION_MS = 300L  // Slower animation
```

**Custom Drag Threshold:**
```kotlin
private val DRAG_THRESHOLD_DP = 12f  // More sensitive drag detection
```

**Custom Haptic Feedback:**
```kotlin
private fun triggerHapticFeedback() {
    containerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
```

## 🎨 Customization

### Visual Styling

**Change Container Dimensions:**
```xml
<!-- In segment_bookings_ios.xml -->
<androidx.constraintlayout.widget.ConstraintLayout
    android:layout_width="350dp"     <!-- Custom width -->
    android:layout_height="60dp">    <!-- Custom height -->
```

**Modify Corner Radius:**
```xml
<!-- In glass_segment_container.xml -->
<corners android:radius="30dp" />    <!-- Match height/2 -->
```

**Update Indicator Size:**
```xml
<!-- Calculate: (container_width / 3) - (margin * 2) -->
<View
    android:layout_width="108dp"     <!-- For 350dp container -->
    android:layout_height="52dp" />  <!-- height - (margin * 2) -->
```

### Color Theming

**Background Transparency:**
```xml
<!-- In glass_segment_container.xml -->
<solid android:color="#30FFFFFF" />  <!-- More opaque -->
```

**Indicator Color:**
```xml
<!-- In glass_segment_indicator.xml -->
<solid android:color="#FFFFFF" />    <!-- Solid white -->
```

**Text Colors:**
```xml
<!-- In segment_bookings_ios.xml -->
<TextView
    android:textColor="#333333"      <!-- Dark text -->
    android:textColorHint="#666666" />
```

### Behavior Customization

**Disable Drag Gestures:**
```kotlin
// In handleTouch() method
MotionEvent.ACTION_MOVE -> {
    // Comment out drag handling
    // val deltaX = abs(event.x - initialTouchX)
    // if (!isDragging && deltaX > dragThresholdPx) { ... }
    return true
}
```

**Add Vibration Patterns:**
```kotlin
private fun triggerHapticFeedback() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(10)
    }
}
```

## 🐛 Troubleshooting

### Common Issues

**1. Indicator Not Moving**
- Check if `onSelectionChanged` callback is being triggered
- Verify segment bounds calculation in `getSegmentIndexFromX()`
- Ensure `setSelectedIndex()` is using correct positioning

**2. Touch Events Not Responding**
- Verify `setupTouchListener()` is called in `init`
- Check for conflicting click listeners on segment views
- Ensure container view is properly set up

**3. Animation Stuttering**
- Confirm `ValueAnimator` is not being cancelled prematurely
- Check for UI thread blocking operations
- Verify animation duration is reasonable (200-300ms)

**4. Incorrect Positioning**
- Use actual segment `.left` positions instead of mathematical calculations
- Account for proper margin calculations (4dp converted to pixels)
- Ensure layout has completed before positioning

### Debug Logging

Enable comprehensive logging:
```kotlin
android.util.Log.d("SegmentedControl", "Touch event: ${event.action}, x: ${event.x}")
android.util.Log.d("SegmentedControl", "Found segment $i for x=$x")
android.util.Log.d("SegmentedControl", "Moving indicator to x=$targetX for segment $index")
```

### Performance Optimization

**Reduce Animation Frequency:**
```kotlin
// Throttle drag updates
private var lastUpdateTime = 0L
private fun updateIndicatorPosition(x: Float) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastUpdateTime > 16) { // ~60 FPS
        // Update position
        lastUpdateTime = currentTime
    }
}
```

## 🚀 Future Improvements

### Planned Enhancements

1. **Multi-line Text Support**
   - Automatic text wrapping for longer segment titles
   - Dynamic height adjustment based on content

2. **RTL Language Support**
   - Right-to-left layout mirroring
   - Proper animation direction handling

3. **Custom Indicator Shapes**
   - Rounded rectangle variants
   - Custom drawable support
   - Gradient backgrounds

4. **Advanced Animations**
   - Spring physics-based animations
   - Bounce effects for overshoots
   - Parallax effects during transitions

5. **Accessibility Improvements**
   - Enhanced screen reader support
   - Custom accessibility actions
   - Focus management for keyboard navigation

6. **Theme Integration**
   - Material Design 3 color tokens
   - Dynamic color support (Android 12+)
   - Dark/light theme automatic adaptation

### Extension Points

**Custom Gesture Handlers:**
```kotlin
interface SegmentedControlGestureDelegate {
    fun onSegmentTapped(index: Int): Boolean
    fun onDragStarted(startIndex: Int): Boolean
    fun onDragProgressed(progress: Float): Boolean
    fun onDragEnded(endIndex: Int): Boolean
}
```

**Animation Customization:**
```kotlin
interface SegmentedControlAnimator {
    fun animateToSegment(fromIndex: Int, toIndex: Int, duration: Long)
    fun animateIndicatorPosition(targetX: Float, duration: Long)
}
```

### Performance Metrics

**Target Performance:**
- Touch response latency: < 16ms
- Animation frame rate: 60 FPS
- Memory allocation: < 1KB per interaction
- Battery impact: Negligible

## 📝 Code Quality Standards

### Best Practices Applied

1. **Separation of Concerns**
   - Gesture handling separated from UI logic
   - Animation logic isolated in dedicated methods
   - Accessibility features modularized

2. **Defensive Programming**
   - Null safety throughout implementation
   - Bounds checking for array access
   - Error handling for edge cases

3. **Resource Management**
   - Proper lifecycle management for animators
   - Memory leak prevention with cleanup methods
   - Efficient drawable resource usage

4. **Documentation**
   - Comprehensive inline comments
   - Clear method naming conventions
   - Proper parameter documentation

### Testing Considerations

**Unit Tests:**
- Gesture recognition accuracy
- Animation timing verification
- Bounds calculation validation

**Integration Tests:**
- Fragment lifecycle compatibility
- Data binding integration
- Callback mechanism verification

**UI Tests:**
- Touch interaction scenarios
- Animation smoothness validation
- Accessibility feature testing

---

## 📞 Support

For questions, improvements, or bug reports related to this segmented control implementation, refer to this documentation and the inline code comments for detailed technical information.

**Last Updated:** October 2025  
**Version:** 1.0  
**Compatibility:** Android API 21+ (Android 5.0+)
