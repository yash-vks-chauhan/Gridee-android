# Operator UI Redesign Plan V2
## Based on ACTUAL Normal User UI (BookingsFragmentNew)

> **Objective**: Redesign Operator Dashboard to match the exact design language of BookingsFragmentNew — minimal, clean, monochromatic (white/grey/black only).

---

## 📁 **Files to Modify**

### Core Files (Must Edit):
1. **Layout XML** 
   - `Gridee_Android/android-app/app/src/main/res/layout/activity_operator_dashboard.xml`
   
2. **Activity Kotlin**
   - `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/operator/OperatorDashboardActivity.kt`
   
3. **ViewModel**
   - `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/operator/OperatorViewModel.kt`

### New Files to Create:
4. **Drawables**
   - `Gridee_Android/android-app/app/src/main/res/drawable/segment_slider_black.xml`

### Supporting Files (No Changes Needed):
5. **Data Models**
   - `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/model/CheckInRequest.kt`
   - `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/data/model/CheckInMode.kt`
   
6. **Colors/Strings** (verify exist, add if missing)
   - `Gridee_Android/android-app/app/src/main/res/values/colors.xml`
   - `Gridee_Android/android-app/app/src/main/res/values/strings.xml`

7. **Manifest** (verify registration)
    - `Gridee_Android/android-app/app/src/main/AndroidManifest.xml`

---

## 🎯 **Analysis of ACTUAL Normal User UI**

### From `BookingsFragmentNew.kt` & `fragment_bookings_new.xml`:

#### Visual Design:
- **Background**: `#F5F5F5` (light grey)
- **Cards**: White with `1dp` stroke `#F0F0F0`, `16dp` corner radius, `0dp` elevation
- **Header**: 28sp bold title, 44dp circular filter button with 1dp stroke
- **Segmented Control**: iOS-style with sliding indicator, smooth animations
- **Typography**: `sans-serif-medium` for bold, `sans-serif` for regular
- **Status Labels**: 10sp uppercase, black text, thin stroke outlines

#### Interactive Elements:
- **Haptic Feedback**: `HapticFeedbackConstants.CONTEXT_CLICK` on all taps
- **Scale Animations**: Subtle press feedback (0.92f scale on touch)
- **Pull-to-Refresh**: Standard Material Design pattern
- **Bottom Sheets**: For filters and details, with blur/overlay effects

#### From `item_booking.xml`:
- Minimal card design with **1dp stroke** instead of elevation
- Small grey labels (11sp, #666666) above values
- Black text (14sp-20sp) for primary content
- Subtle gradient divider between sections
- No colored icons or backgrounds

---

## 📋 **Current Operator UI Problems**

1. ❌ Old blue header (#1E88E5) - should be white card
2. ❌ MaterialCardView with high elevation (12dp) - should be 0dp with 1dp stroke
3. ❌ No segmented control - actions mixed together
4. ❌ Colored buttons (green/orange) - should be black/grey
5. ❌ No haptic feedback or animations
6. ❌ Basic Toast messages - should use proper notifications
7. ❌ No pull-to-refresh
8. ❌ Single shared input field

---

## 🎨 **New Design System (From BookingsFragmentNew)**

### Colors (Monochromatic Only):
```xml
<!-- Background -->
<color name="app_background">#F5F5F5</color>

<!-- Cards & Surfaces -->
<color name="white">#FFFFFF</color>

<!-- Strokes & Borders -->
<color name="stroke_light">#F0F0F0</color>
<color name="stroke_medium">#D0D0D0</color>
<color name="stroke_dark">#E0E0E0</color>

<!-- Text -->
<color name="text_primary">#212121</color>      <!-- Black -->
<color name="text_secondary">#666666</color>    <!-- Dark Grey -->
<color name="text_tertiary">#999999</color>     <!-- Light Grey -->
```

### Typography:
- **Large Titles**: 28sp, bold, sans-serif-medium, black
- **Headers**: 18-20sp, bold, sans-serif-medium, black
- **Body**: 14-16sp, regular, sans-serif, black
- **Labels**: 11sp, regular, sans-serif, grey (#666666)
- **Status**: 10sp, bold, uppercase, letterSpacing 0.05

### Card Design:
```xml
<androidx.cardview.widget.CardView
    app:cardBackgroundColor="@android:color/white"
    app:cardCornerRadius="16dp"
    app:cardElevation="0dp"
    app:strokeWidth="1dp"
    app:strokeColor="#F0F0F0"
    android:foreground="?attr/selectableItemBackground">
```

---

## 🏗️ **Redesigned Operator Dashboard Layout**

### Phase 1: Complete Layout Overhaul

**📍 File**: `Gridee_Android/android-app/app/src/main/res/layout/activity_operator_dashboard.xml`

**Action**: Replace the entire XML content with the new minimal design.

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F5F5F5">

    <!-- Header Row -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/header_row"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="24dp"
        android:layout_marginTop="20dp"
        android:layout_marginEnd="24dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <TextView
            android:id="@+id/text_header_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:text="Operator Dashboard"
            android:textColor="@color/text_primary"
            android:textSize="28sp"
            android:textStyle="bold"
            android:fontFamily="sans-serif-medium"
            android:gravity="start"
            android:paddingTop="12dp"
            android:paddingBottom="4dp"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toStartOf="@id/button_menu" />

        <!-- Menu Button (replaces logout) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/button_menu"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:clickable="true"
            android:focusable="true"
            app:cardBackgroundColor="@color/white"
            app:cardCornerRadius="22dp"
            app:cardElevation="2dp"
            app:strokeColor="#E0E0E0"
            app:strokeWidth="0.5dp"
            app:rippleColor="#10000000"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="@id/text_header_title"
            app:layout_constraintBottom_toBottomOf="@id/text_header_title">

            <ImageView
                android:layout_width="20dp"
                android:layout_height="20dp"
                android:layout_gravity="center"
                android:src="@drawable/ic_menu"
                android:tint="@color/text_primary" />

        </com.google.android.material.card.MaterialCardView>

    </androidx.constraintlayout.widget.ConstraintLayout>

    <!-- Segmented Control (Check-In / Check-Out) -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/segmented_control_container"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:layout_marginStart="16dp"
        android:layout_marginEnd="16dp"
        app:cardCornerRadius="32dp"
        app:cardElevation="0dp"
        app:cardBackgroundColor="#FAFAFA"
        app:strokeWidth="1.5dp"
        app:strokeColor="#E0E0E0"
        app:layout_constraintTop_toBottomOf="@id/header_row"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <FrameLayout
            android:id="@+id/segment_root"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="6dp">

            <!-- Sliding Indicator -->
            <View
                android:id="@+id/segment_slider"
                android:layout_width="0dp"
                android:layout_height="46dp"
                android:visibility="invisible"
                android:background="@drawable/segment_slider_black" />

            <LinearLayout
                android:id="@+id/segment_group"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center">

                <!-- Check-In Segment -->
                <LinearLayout
                    android:id="@+id/segment_checkin"
                    android:layout_width="0dp"
                    android:layout_height="46dp"
                    android:layout_weight="1"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:background="?attr/selectableItemBackgroundBorderless">

                    <TextView
                        android:id="@+id/text_checkin"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Check-In"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:fontFamily="sans-serif-medium"
                        android:textColor="@color/text_secondary" />

                </LinearLayout>

                <!-- Check-Out Segment -->
                <LinearLayout
                    android:id="@+id/segment_checkout"
                    android:layout_width="0dp"
                    android:layout_height="46dp"
                    android:layout_weight="1"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:background="?attr/selectableItemBackgroundBorderless">

                    <TextView
                        android:id="@+id/text_checkout"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Check-Out"
                        android:textSize="14sp"
                        android:textStyle="bold"
                        android:fontFamily="sans-serif-medium"
                        android:textColor="@color/text_secondary" />

                </LinearLayout>

            </LinearLayout>

        </FrameLayout>

    </com.google.android.material.card.MaterialCardView>

    <!-- Content Area with Pull-to-Refresh -->
    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipe_refresh"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/segmented_control_container"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent">

        <androidx.core.widget.NestedScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:fillViewport="true"
            android:clipToPadding="false"
            android:paddingTop="16dp"
            android:paddingBottom="24dp"
            android:paddingHorizontal="16dp">

            <!-- Action Card (Check-In or Check-Out) -->
            <androidx.cardview.widget.CardView
                android:id="@+id/card_action"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardBackgroundColor="@android:color/white"
                app:cardCornerRadius="16dp"
                app:cardElevation="0dp"
                app:strokeWidth="1dp"
                app:strokeColor="#F0F0F0"
                android:foreground="?attr/selectableItemBackground">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="20dp">

                        <!-- Title -->
                        <TextView
                            android:id="@+id/tv_action_title"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Vehicle Check-In"
                            android:textColor="@color/text_primary"
                            android:textSize="18sp"
                            android:textStyle="bold"
                            android:fontFamily="sans-serif-medium"
                            android:layout_marginBottom="8dp" />

                        <!-- Subtitle -->
                        <TextView
                            android:id="@+id/tv_action_subtitle"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Scan or enter vehicle number"
                            android:textColor="@color/text_secondary"
                            android:textSize="14sp"
                            android:layout_marginBottom="16dp" />

                        <!-- Scan Button (Black) -->
                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btn_scan"
                            style="@style/Widget.MaterialComponents.Button"
                            android:layout_width="match_parent"
                            android:layout_height="56dp"
                            android:text="Scan Vehicle Number"
                            android:textAllCaps="false"
                            android:textSize="16sp"
                            android:textColor="@color/white"
                            app:icon="@drawable/ic_camera"
                            app:iconGravity="start"
                            app:iconPadding="8dp"
                            app:iconTint="@color/white"
                            app:cornerRadius="12dp"
                            app:backgroundTint="@color/text_primary" />

                        <!-- OR Divider -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginVertical="16dp"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">

                            <View
                                android:layout_width="0dp"
                                android:layout_height="1dp"
                                android:layout_weight="1"
                                android:background="#E0E0E0" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginHorizontal="12dp"
                                android:text="OR"
                                android:textColor="@color/text_secondary"
                                android:textSize="12sp"
                                android:textStyle="bold" />

                            <View
                                android:layout_width="0dp"
                                android:layout_height="1dp"
                                android:layout_weight="1"
                                android:background="#E0E0E0" />
                        </LinearLayout>

                        <!-- Manual Entry -->
                        <com.google.android.material.textfield.TextInputLayout
                            android:id="@+id/til_vehicle_number"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:hint="Enter Vehicle Number"
                            app:boxCornerRadiusBottomEnd="12dp"
                            app:boxCornerRadiusBottomStart="12dp"
                            app:boxCornerRadiusTopEnd="12dp"
                            app:boxCornerRadiusTopStart="12dp"
                            app:startIconDrawable="@drawable/ic_car"
                            app:startIconTint="@color/text_secondary">

                            <com.google.android.material.textfield.TextInputEditText
                                android:id="@+id/et_vehicle_number"
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:inputType="textCapCharacters"
                                android:maxLines="1" />
                        </com.google.android.material.textfield.TextInputLayout>

                        <!-- Manual Action Button (Outlined) -->
                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btn_manual_action"
                            style="@style/Widget.MaterialComponents.Button.OutlinedButton"
                            android:layout_width="match_parent"
                            android:layout_height="48dp"
                            android:layout_marginTop="12dp"
                            android:text="Check In Manually"
                            android:textAllCaps="false"
                            android:textColor="@color/text_primary"
                            app:cornerRadius="12dp"
                            app:strokeColor="@color/text_primary"
                            app:strokeWidth="1.5dp" />

                    </LinearLayout>

                </androidx.cardview.widget.CardView>

        </androidx.core.widget.NestedScrollView>

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <!-- Loading Indicator -->
    <ProgressBar
        android:id="@+id/progress_loading"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## ⚙️ **Phase 2: Kotlin Implementation**

**📍 File**: `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/operator/OperatorDashboardActivity.kt`

**Action**: Replace the entire class with the new implementation below.

### Key Changes:

```kotlin
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding
    private val viewModel: OperatorViewModel by viewModels()
    
    private var currentMode = OperatorMode.CHECK_IN
    
    enum class OperatorMode {
        CHECK_IN, CHECK_OUT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Use system bars like BookingsFragment
        window.statusBarColor = android.graphics.Color.parseColor("#F5F5F5")
        
        setupUI()
        setupSegmentedControl()
        setupPullToRefresh()
        observeViewModel()
    }

    private fun setupUI() {
        // Scan button with haptic feedback
        binding.btnScan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            animateButtonPress(it)
            
            if (checkCameraPermission()) {
                openVehicleScanner()
            } else {
                requestCameraPermission()
            }
        }
        
        // Manual action button
        binding.btnManualAction.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            
            if (vehicleNumber.isNotBlank()) {
                when (currentMode) {
                    OperatorMode.CHECK_IN -> viewModel.checkInByVehicleNumber(vehicleNumber)
                    OperatorMode.CHECK_OUT -> viewModel.checkOutByVehicleNumber(vehicleNumber)
                }
                binding.etVehicleNumber.text?.clear()
            } else {
                binding.tilVehicleNumber.error = "Please enter vehicle number"
            }
        }
        
        // Menu button (for settings/logout)
        binding.buttonMenu.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            animateButtonPress(it)
            showMenuBottomSheet()
        }
    }

    private fun setupSegmentedControl() {
        val segmentCheckIn = binding.segmentCheckin
        val segmentCheckOut = binding.segmentCheckout
        
        // Initial state
        selectSegment(OperatorMode.CHECK_IN)
        
        segmentCheckIn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            selectSegment(OperatorMode.CHECK_IN)
        }
        
        segmentCheckOut.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            selectSegment(OperatorMode.CHECK_OUT)
        }
        
        // Animate slider on first layout
        binding.segmentRoot.doOnLayout {
            positionSliderInstantly(when (currentMode) {
                OperatorMode.CHECK_IN -> segmentCheckIn
                OperatorMode.CHECK_OUT -> segmentCheckOut
            })
        }
    }

    private fun selectSegment(mode: OperatorMode) {
        if (mode == currentMode) return
        
        currentMode = mode
        
        // Update UI based on mode
        when (mode) {
            OperatorMode.CHECK_IN -> {
                binding.tvActionTitle.text = "Vehicle Check-In"
                binding.tvActionSubtitle.text = "Scan or enter vehicle number"
                binding.btnManualAction.text = "Check In Manually"
                animateSlider(binding.segmentCheckin)
                updateSegmentTextColors(selectedMode = OperatorMode.CHECK_IN)
            }
            OperatorMode.CHECK_OUT -> {
                binding.tvActionTitle.text = "Vehicle Check-Out"
                binding.tvActionSubtitle.text = "Scan or enter vehicle number"
                binding.btnManualAction.text = "Check Out Manually"
                animateSlider(binding.segmentCheckout)
                updateSegmentTextColors(selectedMode = OperatorMode.CHECK_OUT)
            }
        }
    }

    private fun animateSlider(targetSegment: View) {
        val slider = binding.segmentSlider
        val targetX = targetSegment.left.toFloat()
        val targetWidth = targetSegment.width
        
        // Width animation
        val widthAnimator = ValueAnimator.ofInt(slider.width, targetWidth).apply {
            addUpdateListener { animator ->
                val params = slider.layoutParams
                params.width = animator.animatedValue as Int
                slider.layoutParams = params
            }
        }
        
        // Translation animation
        val translationAnimator = ObjectAnimator.ofFloat(
            slider, View.TRANSLATION_X,
            slider.translationX, targetX
        )
        
        AnimatorSet().apply {
            playTogether(widthAnimator, translationAnimator)
            duration = 280
            interpolator = OvershootInterpolator(0.55f)
            start()
        }
        
        slider.visibility = View.VISIBLE
    }

    private fun positionSliderInstantly(targetSegment: View) {
        val slider = binding.segmentSlider
        val params = slider.layoutParams
        params.width = targetSegment.width
        slider.layoutParams = params
        slider.translationX = targetSegment.left.toFloat()
        slider.visibility = View.VISIBLE
    }

    private fun updateSegmentTextColors(selectedMode: OperatorMode) {
        val selectedColor = ContextCompat.getColor(this, R.color.text_primary)
        val unselectedColor = ContextCompat.getColor(this, R.color.text_secondary)
        
        binding.textCheckin.setTextColor(
            if (selectedMode == OperatorMode.CHECK_IN) selectedColor else unselectedColor
        )
        binding.textCheckout.setTextColor(
            if (selectedMode == OperatorMode.CHECK_OUT) selectedColor else unselectedColor
        )
    }

    private fun animateButtonPress(view: View) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setupPullToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.text_primary)
        )
        binding.swipeRefresh.setOnRefreshListener {
            // Just refresh the UI state
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewModel.checkInState.observe(this) { state ->
            handleOperationState(state, "Check-In")
        }
        
        viewModel.checkOutState.observe(this) { state ->
            handleOperationState(state, "Check-Out")
        }
    }

    private fun handleOperationState(state: CheckInState, operation: String) {
        when (state) {
            is CheckInState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnScan.isEnabled = false
                binding.btnManualAction.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true
                
                showSuccessMessage(
                    "$operation Successful",
                    "Vehicle ${state.booking.vehicleNumber}"
                )
                
                viewModel.resetCheckInState()
            }
            is CheckInState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true
                
                showErrorMessage(
                    "$operation Failed",
                    state.message
                )
                
                viewModel.resetCheckInState()
            }
            else -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true
            }
        }
    }

    private fun showSuccessMessage(title: String, message: String) {
        // Use same notification style as BookingsFragment
        NotificationHelper.showSuccess(
            parent = binding.root,
            title = title,
            message = message,
            duration = 3000L
        )
    }

    private fun showErrorMessage(title: String, message: String) {
        NotificationHelper.showError(
            parent = binding.root,
            title = title,
            message = message,
            duration = 3000L
        )
    }

    private fun showMenuBottomSheet() {
        // Bottom sheet for settings, profile, logout
        // Similar to booking details bottom sheet
    }
}
```

---

## 📦 **Phase 3: Supporting Files**

### 1. Segment Slider Background (Black)
**📍 File**: `Gridee_Android/android-app/app/src/main/res/drawable/segment_slider_black.xml`

**Action**: Create this new drawable file.
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#212121" />
    <corners android:radius="23dp" />
</shape>
```

### 2. Activity Item Layout (Minimal)
**📍 File**: `Gridee_Android/android-app/app/src/main/res/layout/item_operator_activity.xml`

**Action**: Create this new layout file for RecyclerView items.
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:gravity="center_vertical"
    android:background="?attr/selectableItemBackground">

    <!-- Small Circle Indicator -->
    <View
        android:layout_width="8dp"
        android:layout_height="8dp"
        android:layout_marginEnd="12dp"
        android:background="@drawable/circle_small_grey" />

    <!-- Activity Info -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_vehicle_number"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="DL01AB1234"
            android:textColor="@color/text_primary"
            android:textSize="14sp"
            android:textStyle="bold"
            android:fontFamily="sans-serif-medium" />

        <TextView
            android:id="@+id/tv_action_type"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:text="Checked In"
            android:textColor="@color/text_secondary"
            android:textSize="12sp" />
    </LinearLayout>

    <!-- Timestamp -->
    <TextView
        android:id="@+id/tv_timestamp"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="2m ago"
        android:textColor="@color/text_tertiary"
        android:textSize="11sp" />

</LinearLayout>
```

### 3. Circle Drawable
**📍 File**: `Gridee_Android/android-app/app/src/main/res/drawable/circle_small_grey.xml`

**Action**: Create this new drawable file for activity indicators.
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#E0E0E0" />
    <size android:width="8dp" android:height="8dp" />
</shape>
```

---

## 📦 **Phase 4: Adapter Implementation**

**📍 File**: `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/operator/OperatorActivityAdapter.kt`

**Action**: Create this new adapter class.

```kotlin
package com.gridee.parking.ui.operator

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.databinding.ItemOperatorActivityBinding
import java.text.SimpleDateFormat
import java.util.*

class OperatorActivityAdapter(
    private var activities: List<OperatorActivity>
) : RecyclerView.Adapter<OperatorActivityAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemOperatorActivityBinding) : 
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOperatorActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        
        holder.binding.tvVehicleNumber.text = activity.vehicleNumber
        holder.binding.tvActionType.text = when (activity.action) {
            "CHECK_IN" -> "Checked In"
            "CHECK_OUT" -> "Checked Out"
            else -> activity.action
        }
        holder.binding.tvTimestamp.text = getTimeAgo(activity.timestamp)
    }

    override fun getItemCount() = activities.size

    fun updateActivities(newActivities: List<OperatorActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }

    private fun getTimeAgo(timestamp: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp.time
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}
```

---

## 📊 **Phase 5: ViewModel Updates**

**📍 File**: `Gridee_Android/android-app/app/src/main/java/com/gridee/parking/ui/operator/OperatorViewModel.kt`

**Action**: Add these data classes and functions to the existing ViewModel.

```kotlin
// Add to OperatorViewModel.kt

data class OperatorActivity(
    val id: String,
    val vehicleNumber: String,
    val action: String, // "CHECK_IN" or "CHECK_OUT"
    val timestamp: Date,
    val spotId: String?,
    val amount: Double?
)

private val _recentActivity = MutableLiveData<List<OperatorActivity>>()
val recentActivity: LiveData<List<OperatorActivity>> = _recentActivity

fun loadRecentActivity() {
    viewModelScope.launch {
        try {
            // TODO: Call repository method when backend is ready
            // val response = bookingRepository.getRecentOperatorActivity(limit = 10)
            // if (response.isSuccessful) {
            //     _recentActivity.value = response.body() ?: emptyList()
            // }
            
            // For now, use mock data or empty list
            _recentActivity.value = emptyList()
        } catch (e: Exception) {
            _recentActivity.value = emptyList()
        }
    }
}
```

---

## ✅ **Implementation Checklist**

### Week 1: Layout & Design
- [ ] **EDIT**: `app/src/main/res/layout/activity_operator_dashboard.xml` - Replace entire XML with new minimal design
- [ ] **CREATE**: `app/src/main/res/drawable/segment_slider_black.xml` - Black slider background
- [ ] **VERIFY**: Colors exist in `app/src/main/res/values/colors.xml` (text_primary, text_secondary, etc.)
- [ ] **VERIFY**: Background color is #F5F5F5 throughout
- [ ] Remove recent activity card section from layout

### Week 2: Interactive Features
- [ ] **EDIT**: `app/src/main/java/com/gridee/parking/ui/operator/OperatorDashboardActivity.kt` - Replace entire class
  - [ ] Add segmented control logic with sliding indicator
  - [ ] Add haptic feedback to all buttons (`HapticFeedbackConstants.CONTEXT_CLICK`)
  - [ ] Add scale animations on button press (0.92f scale)
  - [ ] Implement pull-to-refresh (simple refresh, no data loading)
  - [ ] Add smooth slider animations (OvershootInterpolator)
  - [ ] Create menu bottom sheet
  - [ ] Replace Toast with NotificationHelper
  - [ ] Add OperatorMode enum (CHECK_IN / CHECK_OUT)
  - [ ] Remove all recent activity related code

### Week 3: Testing & Polish
- [ ] Test segmented control animations and interactions
- [ ] Test haptic feedback on all buttons
- [ ] Test check-in flow (scan and manual)
- [ ] Test check-out flow (scan and manual)
- [ ] Test pull-to-refresh behavior
- [ ] Verify proper error handling and notifications
- [ ] Polish typography and spacing
- [ ] Test on different screen sizes
- [ ] Final QA and testing

---

## 🎯 **Expected Results**

### Before → After:

| Aspect | Before | After |
|--------|--------|-------|
| **Header** | Blue background (#1E88E5) | White card, clean title |
| **Cards** | High elevation (12dp) | 1dp stroke, 0dp elevation |
| **Buttons** | Green/Orange colors | Black/Grey only |
| **Layout** | Mixed actions | Segmented control |
| **Animations** | None | Haptic + smooth transitions |
| **Feedback** | Basic Toast | NotificationHelper |
| **Refresh** | None | Pull-to-refresh |
| **UI Focus** | Cluttered with stats | Clean, action-focused |

---

## 📐 **Design Specifications**

### Spacing:
- Screen margins: 16-24dp
- Card padding: 20dp
- Card margins: 16dp horizontal, 6-16dp vertical
- Element spacing: 8-16dp

### Typography:
- Title: 28sp bold
- Card header: 18sp bold
- Body: 14-16sp regular
- Label: 11-12sp regular grey
- Button: 14-16sp medium

### Colors (Final):
- Background: #F5F5F5
- Cards: #FFFFFF
- Primary text: #212121
- Secondary text: #666666
- Tertiary text: #999999
- Strokes: #F0F0F0, #E0E0E0

---

**This plan creates an Operator Dashboard that perfectly matches the actual design language of BookingsFragmentNew — minimal, clean, professional, and monochromatic.**
