# Edge-to-Edge Implementation Guide

This guide explains how to fix the gray gaps around the home gesture pill in Android applications using Jetpack Compose and Material 3.

## Problem Description

On devices with gesture navigation, you may notice light gray areas on both sides of the home gesture pill (bottom center of the screen). This happens when:

1. The app doesn't properly handle edge-to-edge drawing
2. The system applies contrast enforcement to the navigation bar
3. Window insets aren't properly configured

## Solution Overview

The complete solution involves:

1. **Enable edge-to-edge drawing**
2. **Disable navigation bar contrast enforcement**
3. **Use transparent navigation bar**
4. **Properly handle window insets in Compose**
5. **Configure Material 3 NavigationBar correctly**

## Implementation

### 1. Activity Configuration

```kotlin
class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge drawing
        enableEdgeToEdge()
        
        // Additional configuration
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            YourAppTheme {
                MainContent()
            }
        }
    }
}
```

### 2. System UI Configuration

```kotlin
@Composable
fun MainContent() {
    val systemUiController = rememberSystemUiController()
    
    LaunchedEffect(Unit) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent, // Key: Use transparent
            darkIcons = true,
            navigationBarContrastEnforced = false // Critical: Disable contrast
        )
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = true
        )
    }
    
    // Your UI content...
}
```

### 3. NavigationBar with Window Insets

```kotlin
Scaffold(
    bottomBar = {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            // Important: Handle navigation bar insets properly
            windowInsets = WindowInsets.navigationBars
        ) {
            // Navigation items...
        }
    }
) { paddingValues ->
    // Content with proper padding
    Content(modifier = Modifier.padding(paddingValues))
}
```

### 4. Theme Configuration

In `themes.xml`:

```xml
<style name="Theme.YourApp" parent="Theme.Material3.DayNight">
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:windowLightStatusBar">true</item>
    <item name="android:windowLightNavigationBar">true</item>
    
    <!-- Edge-to-edge configuration -->
    <item name="android:windowTranslucentStatus">false</item>
    <item name="android:windowTranslucentNavigation">false</item>
    <item name="android:fitsSystemWindows">false</item>
    
    <!-- Disable contrast enforcement (API 29+) -->
    <item name="android:enforceNavigationBarContrast">false</item>
    <item name="android:enforceStatusBarContrast">false</item>
</style>
```

### 5. Build Configuration

In `build.gradle`:

```gradle
android {
    compileSdk 34
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    buildFeatures {
        compose true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = '1.5.8'
    }
}

dependencies {
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.activity:activity-compose:1.8.2'
    
    // System UI controller for window insets
    implementation 'com.google.accompanist:accompanist-systemuicontroller:0.32.0'
}
```

## Key Points

### Critical Settings

1. **`navigationBarContrastEnforced = false`** - This is the most important setting to prevent gray areas
2. **`Color.Transparent`** for navigation bar color
3. **`WindowInsets.navigationBars`** on NavigationBar
4. **`enableEdgeToEdge()`** in Activity

### Material 3 NavigationBar

The Material 3 `NavigationBar` component automatically handles:
- Proper padding for the navigation area
- Background color extension into system area
- Safe area handling for the gesture indicator

### Testing

Test on devices with:
- Gesture navigation enabled
- Different screen sizes
- Light and dark themes
- Different Android versions (API 24+)

## Common Issues

### Issue: Gray areas still visible
**Solution**: Ensure `navigationBarContrastEnforced = false` is set

### Issue: Navigation bar overlaps content
**Solution**: Use `WindowInsets.navigationBars` on NavigationBar and proper padding

### Issue: Status bar issues
**Solution**: Configure both status and navigation bars consistently

## Files Modified

1. `MainComposeActivity.kt` - New Compose-based main activity
2. `EdgeToEdgeComposeActivity.kt` - Example implementation
3. `EdgeToEdgeUtils.kt` - Utility functions
4. `build.gradle` - Added Compose dependencies
5. `themes.xml` - Updated theme configuration
6. `AndroidManifest.xml` - Added new activities

## Migration from View System

If migrating from View-based system:

1. Replace ViewBinding activities with Compose activities
2. Use `NavigationBar` instead of custom bottom navigation
3. Apply proper window insets handling
4. Update theme configuration
5. Test thoroughly on different devices

## Result

After implementing these changes:
- ✅ No gray gaps around home gesture pill
- ✅ Seamless navigation bar integration
- ✅ Proper edge-to-edge experience
- ✅ Material 3 design compliance
- ✅ Support for light and dark themes
