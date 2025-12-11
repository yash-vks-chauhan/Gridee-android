# Enhanced Booking Card with Accordion Implementation

## Core Design Philosophy
Professional and modern design with smooth accordion expansion and premium animations.

## Visual Structure

### States Overview
1. **Collapsed State**
   - Clean booking card with essential info
   - Subtle elevation and shadow
   - Indicator for expandability

2. **Expanded State**
   - Card elevated with spring animation
   - Dark accordion panel slides out
   - Background blur effect
   - Action buttons with smooth entrance

### Main Container (White Card)
```
Appearance:
- Clean white background (#FFFFFF)
- Subtle elevation (2dp)
- Rounded corners (8dp)
- Fine border (1dp, #E0E0E0)
- Consistent padding (16dp)
```

### Layout Hierarchy
```
┌─────────────────────────────────┐
│ ○ Active • Today, 2:30 PM      │  <- Status Bar
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │    4hrs 30min Remaining     │ │  <- Time Card (Grey)
│ │    Check-in:  2:30 PM       │ │
│ │    Check-out: 7:00 PM       │ │
│ └─────────────────────────────┘ │
│                                 │
│ 📍 Block A, Level 2            │  <- Location
│ 🚗 KA-01-XX-XXXX              │  <- Vehicle
│ 💳 ₹150                       │  <- Amount
└─────────────────────────────────┘
```

## Implementation Steps

1. **Base Setup**
   ```kotlin
   // Create custom ViewGroup
   class ExpandableBookingCard : MotionLayout {
       private var currentState = State.COLLAPSED
       private val springAnimation: SpringAnimation
       // ... setup spring parameters and states
   }
   ```

2. **Animation Configuration**
   ```kotlin
   // Spring constants for natural feel
   private val SPRING_STIFFNESS = 350f
   private val SPRING_DAMPING = 0.8f
   
   // Setup spring animation
   springAnimation = SpringAnimation(this).apply {
       spring = SpringForce().apply {
           dampingRatio = SPRING_DAMPING
           stiffness = SPRING_STIFFNESS
       }
   }
   ```

3. **Accordion Implementation**
   ```kotlin
   // Dark themed panel with buttons
   private fun setupAccordionPanel() {
       // Add blur effect (API 31+)
       background.setRenderEffect(
           RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP)
       )
       
       // Setup action buttons with delayed entrance
       setupActionButtons()
   }
   ```

4. **Interaction Handling**
   ```kotlin
   // Manage touch events and state transitions
   private fun handleExpansion() {
       // Toggle expansion with spring animation
       springAnimation.start()
       
       // Coordinate sub-animations (elevation, blur, buttons)
       animateElevation()
       animateButtons()
   }
   ```

## Styling Guidelines

### Typography
```
Headings: 16sp, SemiBold
Primary Text: 14sp, Medium
Secondary: 12sp, Regular
```

### Spacing
```
Outer Margin: 16dp
Inner Padding: 12dp
Element Gap: 8dp
```

### Animations
```kotlin
// Status changes
implementation 'smooth fade transitions'
duration = 300ms
interpolator = FastOutSlowIn

// Touch feedback
implementation 'subtle scale animation'
scaleDown = 0.98f
duration = 100ms
```

## Best Practices
1. Keep text crisp (no transparency)
2. Use consistent spacing
3. Maintain proper touch targets (48dp)
4. Implement proper state handling
5. Cache time calculations

## Sample Usage
```kotlin
bookingCard.apply {
    setStatus(BookingStatus.ACTIVE)
    setTimes(checkIn, checkOut)
    setLocation("Block A, Level 2")
    setVehicle("KA-01-XX-XXXX")
    setAmount(150.0)
}
```

## Testing Checklist
- [ ] Animation smoothness (60fps)
- [ ] Spring physics feel natural
- [ ] Blur effect performance
- [ ] Button interactions
- [ ] State management
- [ ] Memory efficiency
- [ ] Accessibility compliance
- [ ] RTL support
- [ ] Dark mode compatibility

2. **Inner Grey Card** (Middle)
   - Background: #F5F5F5
   - Corner Radius: 4dp
   - Padding: 12dp
   - Content:
     - Check-in/out times
     - Duration
     - Amount

3. **Details Section** (Bottom)
   - Spot information
   - Vehicle details
   - Action buttons

## Typography Hierarchy
1. **Primary Text**
   - Size: 16sp
   - Weight: Bold (700)
   - Color: #000000

2. **Secondary Text**
   - Size: 14sp
   - Weight: Medium (500)
   - Color: #424242

3. **Supporting Text**
   - Size: 12sp
   - Weight: Regular (400)
   - Color: #757575

## Icons & Visual Elements
- Material Icons (outlined style)
- Size: 18dp
- Color: #757575
- Consistent padding: 8dp

## Implementation Steps

1. **Layout Implementation**
   - Create base card layout
   - Implement inner grey card
   - Add status indicators
   - Set up typography styles

2. **Component Creation**
   - Custom BookingCardView
   - Status indicator component
   - Time display component

3. **Animations & Interactions**
   - Card press feedback
   - Status transitions
   - Expand/collapse animations

4. **Testing & Refinement**
   - Layout testing
   - Accessibility verification
   - Performance optimization

## Sample Implementation
```xml
<com.gridee.parking.ui.components.BookingCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="8dp">

    <!-- Main Content Container -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- Header with Status -->
        <include layout="@layout/booking_card_header"/>

        <!-- Inner Grey Card -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_margin="8dp"
            app:cardBackgroundColor="#F5F5F5"
            app:cardCornerRadius="4dp"
            app:cardElevation="0dp">
            
            <include layout="@layout/booking_details_content"/>
            
        </com.google.android.material.card.MaterialCardView>

        <!-- Additional Details -->
        <include layout="@layout/booking_additional_info"/>

    </LinearLayout>

</com.gridee.parking.ui.components.BookingCardView>
```

## Best Practices
1. Use consistent spacing (8dp grid)
2. Maintain proper touch targets (48dp)
3. Follow Material Design guidelines
4. Implement proper accessibility
5. Use proper state handling
6. Maintain consistent animations
