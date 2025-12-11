# Gridee Splash Screen Implementation

## Overview
A professional splash screen has been implemented with smooth animations showing:
1. A card on a road (representing parking payment)
2. A bike on a road (representing parking/vehicle)
3. The "Gridee" logo in bold center

## Files Created/Modified

### 1. SplashActivity.kt
**Location:** `/app/src/main/java/com/gridee/parking/ui/SplashActivity.kt`

**Features:**
- Black background for professional look
- Sequential animations with smooth transitions
- Card slides in from left (0.5s - 2s)
- Bike slides in from left (2s - 3.5s)
- Gridee logo fades and scales in with bounce effect (3.5s - 4.5s)
- Automatically navigates to LoginActivity after animations
- Back button disabled during splash

### 2. activity_splash.xml
**Location:** `/app/src/main/res/layout/activity_splash.xml`

**Components:**
- Black background (#000000)
- ImageView for card icon
- ImageView for bike icon
- TextView for "Gridee" logo (48sp, bold, white)
- All centered on screen

### 3. Drawable Resources

#### ic_splash_card.xml
**Location:** `/app/src/main/res/drawable/ic_splash_card.xml`
- Vector drawable of a credit card on a road
- Includes road with yellow markings
- White credit card with blue magnetic strip and gold chip

#### ic_splash_bike.xml
**Location:** `/app/src/main/res/drawable/ic_splash_bike.xml`
- Vector drawable of a motorcycle/bike on a road
- Includes road with yellow markings
- White bike with gray/black wheels

### 4. Strings Added
**Location:** `/app/src/main/res/values/strings.xml`
```xml
<string name="app_name_gridee">Gridee</string>
<string name="card_on_road">Card on road</string>
<string name="bike_on_road">Bike on road</string>
```

### 5. Theme Added
**Location:** `/app/src/main/res/values/themes.xml`
```xml
<style name="Theme.Gridee.Splash" parent="Theme.Gridee.NoActionBar">
    - Black background
    - Black status bar and navigation bar
    - Dark system UI icons
</style>
```

### 6. AndroidManifest.xml Updated
- SplashActivity is now the LAUNCHER activity
- LoginActivity changed to exported="false"

## Animation Timeline

| Time | Animation |
|------|-----------|
| 0-0.5s | Initial delay |
| 0.5-1.5s | Card slides in from left |
| 0.8-1.8s | Card fades in |
| 2.0-2.3s | Card fades out |
| 2.0-3.0s | Bike slides in from left |
| 2.0-2.3s | Bike fades in |
| 3.5-3.8s | Bike fades out |
| 3.5-4.1s | Logo fades in and scales with bounce |
| 4.5-5.5s | Display logo |
| 5.5s | Navigate to Login |

## How to Test

1. Build and run the app
2. The splash screen will show automatically on app launch
3. Watch the smooth transitions:
   - Card slides across the screen
   - Transitions to bike
   - Finally shows Gridee logo
4. App will automatically navigate to login screen

## Customization Options

### Adjust Animation Speed
In `SplashActivity.kt`, modify the `duration` and `startDelay` values:
```kotlin
val cardSlideIn = ObjectAnimator.ofFloat(...).apply {
    duration = 1000  // Change this (milliseconds)
    startDelay = 500 // Change this (milliseconds)
}
```

### Change Colors
- Background: Update `android:background="#000000"` in `activity_splash.xml`
- Icons: Update `app:tint="#FFFFFF"` in `activity_splash.xml`
- Logo text: Update `android:textColor="#FFFFFF"` in `activity_splash.xml`

### Adjust Logo Size
In `activity_splash.xml`:
```xml
android:textSize="48sp"  <!-- Change this -->
```

## Professional Features

✅ Smooth deceleration interpolators for natural movement
✅ Overshoot interpolator for logo bounce effect
✅ Black background for premium feel
✅ White icons for high contrast
✅ Sequential storytelling (payment → vehicle → brand)
✅ Edge-to-edge design
✅ Proper timing (5.5 seconds total)
✅ Disabled back button during splash
✅ Fade transitions for smooth changes

## Notes

- The splash screen uses vector drawables for crisp rendering on all screen sizes
- All animations use ObjectAnimator for smooth performance
- The app follows Material Design principles for transitions
- System bars are styled to match the black theme
