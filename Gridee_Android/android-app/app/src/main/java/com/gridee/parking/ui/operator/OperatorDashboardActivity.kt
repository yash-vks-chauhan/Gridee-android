package com.gridee.parking.ui.operator

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityOperatorDashboardBinding
import com.gridee.parking.ui.auth.LoginActivity
import com.gridee.parking.ui.qr.QrScannerActivity
import com.gridee.parking.utils.NotificationHelper

/**
 * Dashboard for parking lot operators
 * Allows operators to check-in/check-out vehicles by scanning license plates
 * Features minimal monochromatic design with iOS-style segmented control
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding
    private val viewModel: OperatorViewModel by viewModels()
    private var currentMode = OperatorMode.CHECK_IN

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val ANIM_DURATION = 280L
    }

    enum class OperatorMode {
        CHECK_IN, CHECK_OUT
    }

    private val vehicleScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QrScannerActivity.RESULT_QR_SCANNED) {
            val scannedData = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE)
            scannedData?.let { vehicleNumber ->
                binding.etVehicleNumber.setText(vehicleNumber)
                // Auto-process based on current mode
                when (currentMode) {
                    OperatorMode.CHECK_IN -> viewModel.checkInByVehicleNumber(vehicleNumber)
                    OperatorMode.CHECK_OUT -> viewModel.checkOutByVehicleNumber(vehicleNumber)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar to match background
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupUI()
        setupSegmentedControl()
        observeViewModel()
        
        // Initialize with Check-In mode
        updateUIForMode(OperatorMode.CHECK_IN, animate = false)
    }

    private fun setupUI() {
        // Menu Button
        binding.buttonMenu.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            showMenuOptions()
        }

        // Scan Button
        binding.btnScan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            animateButtonPress(it) {
                if (checkCameraPermission()) {
                    openVehicleScanner()
                } else {
                    requestCameraPermission()
                }
            }
        }

        // Manual Action Button
        binding.btnManualAction.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            animateButtonPress(it) {
                val vehicleNumber = binding.etVehicleNumber.text.toString().trim()
                
                // Clear previous error
                binding.tilVehicleNumber.error = null
                
                // Validate input
                when {
                    vehicleNumber.isBlank() -> {
                        binding.tilVehicleNumber.error = "Please enter vehicle number"
                        binding.etVehicleNumber.requestFocus()
                    }
                    vehicleNumber.length < 4 -> {
                        binding.tilVehicleNumber.error = "Vehicle number too short"
                        binding.etVehicleNumber.requestFocus()
                    }
                    else -> {
                        // Process check-in or check-out
                        when (currentMode) {
                            OperatorMode.CHECK_IN -> viewModel.checkInByVehicleNumber(vehicleNumber)
                            OperatorMode.CHECK_OUT -> viewModel.checkOutByVehicleNumber(vehicleNumber)
                        }
                    }
                }
            }
        }
        
        // Clear error when user starts typing
        binding.etVehicleNumber.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilVehicleNumber.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            showNotification("Dashboard refreshed", NotificationType.SUCCESS)
        }

        // Set refresh colors to match theme
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, android.R.color.black)
        )
    }

    private fun setupSegmentedControl() {
        // Make slider visible after layout
        binding.segmentSlider.post {
            binding.segmentSlider.visibility = View.VISIBLE
            
            // Set initial position for Check-In (left segment)
            val params = binding.segmentSlider.layoutParams
            params.width = binding.segmentCheckin.width
            binding.segmentSlider.layoutParams = params
            binding.segmentSlider.translationX = 0f
        }

        // Check-In Segment Click
        binding.segmentCheckin.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            if (currentMode != OperatorMode.CHECK_IN) {
                switchToMode(OperatorMode.CHECK_IN)
            }
        }

        // Check-Out Segment Click
        binding.segmentCheckout.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            if (currentMode != OperatorMode.CHECK_OUT) {
                switchToMode(OperatorMode.CHECK_OUT)
            }
        }
    }

    private fun switchToMode(mode: OperatorMode) {
        currentMode = mode
        
        // Animate slider
        val targetX = when (mode) {
            OperatorMode.CHECK_IN -> 0f
            OperatorMode.CHECK_OUT -> binding.segmentCheckin.width.toFloat()
        }
        
        ValueAnimator.ofFloat(binding.segmentSlider.translationX, targetX).apply {
            duration = ANIM_DURATION
            interpolator = OvershootInterpolator(0.55f)
            addUpdateListener { animation ->
                binding.segmentSlider.translationX = animation.animatedValue as Float
            }
            start()
        }
        
        // Update text colors
        updateSegmentTextColors(mode)
        
        // Update card content with animation
        updateUIForMode(mode, animate = true)
    }

    private fun updateSegmentTextColors(mode: OperatorMode) {
        val selectedColor = ContextCompat.getColor(this, android.R.color.white)
        val unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        
        when (mode) {
            OperatorMode.CHECK_IN -> {
                binding.textCheckin.setTextColor(selectedColor)
                binding.textCheckout.setTextColor(unselectedColor)
            }
            OperatorMode.CHECK_OUT -> {
                binding.textCheckin.setTextColor(unselectedColor)
                binding.textCheckout.setTextColor(selectedColor)
            }
        }
    }

    private fun updateUIForMode(mode: OperatorMode, animate: Boolean) {
        val (title, subtitle, buttonText) = when (mode) {
            OperatorMode.CHECK_IN -> Triple(
                "Vehicle Check-In",
                "Scan or enter vehicle number",
                "Check In Manually"
            )
            OperatorMode.CHECK_OUT -> Triple(
                "Vehicle Check-Out",
                "Scan or enter vehicle number to complete payment",
                "Check Out Manually"
            )
        }
        
        if (animate) {
            // Fade out, update, fade in
            binding.cardAction.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(140)
                .withEndAction {
                    binding.tvActionTitle.text = title
                    binding.tvActionSubtitle.text = subtitle
                    binding.btnManualAction.text = buttonText
                    
                    binding.cardAction.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(140)
                        .setInterpolator(OvershootInterpolator(0.55f))
                        .start()
                }
                .start()
        } else {
            binding.tvActionTitle.text = title
            binding.tvActionSubtitle.text = subtitle
            binding.btnManualAction.text = buttonText
        }
        
        // Clear input
        binding.etVehicleNumber.text?.clear()
    }

    private fun animateButtonPress(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction { action() }
                    .start()
            }
            .start()
    }

    private fun showMenuOptions() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_operator_menu, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Get operator info from SharedPreferences
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val operatorName = sharedPref.getString("user_name", "Operator")
        val parkingLotName = sharedPref.getString("parking_lot_name", "Parking Lot")
        
        // Set operator info
        bottomSheetView.findViewById<android.widget.TextView>(R.id.tv_operator_name)?.text = operatorName
        bottomSheetView.findViewById<android.widget.TextView>(R.id.tv_parking_lot)?.text = parkingLotName
        
        // Session Info
        bottomSheetView.findViewById<android.view.View>(R.id.menu_session_info)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            bottomSheetDialog.dismiss()
            showSessionInfo()
        }
        
        // Settings
        bottomSheetView.findViewById<android.view.View>(R.id.menu_settings)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            bottomSheetDialog.dismiss()
            showNotification("Settings coming soon", NotificationType.INFO)
        }
        
        // Help
        bottomSheetView.findViewById<android.view.View>(R.id.menu_help)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            bottomSheetDialog.dismiss()
            showNotification("Help & Support coming soon", NotificationType.INFO)
        }
        
        // Logout
        bottomSheetView.findViewById<android.view.View>(R.id.menu_logout)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            bottomSheetDialog.dismiss()
            showLogoutConfirmation()
        }
        
        bottomSheetDialog.show()
    }
    
    private fun showSessionInfo() {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val operatorName = sharedPref.getString("user_name", "Operator")
        val parkingLotName = sharedPref.getString("parking_lot_name", "Parking Lot")
        
        val message = "Operator: $operatorName\nLocation: $parkingLotName\nSession: Active"
        showNotification(message, NotificationType.INFO)
    }
    
    private fun showLogoutConfirmation() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openVehicleScanner() {
        val intent = Intent(this, QrScannerActivity::class.java).apply {
            putExtra(
                QrScannerActivity.EXTRA_SCAN_TYPE,
                when (currentMode) {
                    OperatorMode.CHECK_IN -> "VEHICLE_CHECK_IN"
                    OperatorMode.CHECK_OUT -> "VEHICLE_CHECK_OUT"
                }
            )
        }
        vehicleScannerLauncher.launch(intent)
    }

    private fun observeViewModel() {
        // Observe check-in state
        viewModel.checkInState.observe(this) { state ->
            handleCheckInState(state)
        }

        // Observe check-out state
        viewModel.checkOutState.observe(this) { state ->
            handleCheckOutState(state)
        }
    }

    private fun handleCheckInState(state: CheckInState) {
        when (state) {
            is CheckInState.Idle -> {
                binding.progressLoading.visibility = View.GONE
            }
            is CheckInState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnScan.isEnabled = false
                binding.btnManualAction.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true

                val booking = state.booking
                showNotification(
                    "✅ Check-In Successful\n" +
                    "Vehicle: ${booking.vehicleNumber}\n" +
                    "Slot: ${booking.spotId ?: "N/A"}",
                    NotificationType.SUCCESS
                )

                viewModel.resetCheckInState()
                binding.etVehicleNumber.text?.clear()
            }
            is CheckInState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true

                showNotification(
                    "❌ Check-In Failed\n${state.message}",
                    NotificationType.ERROR
                )

                viewModel.resetCheckInState()
            }
        }
    }

    private fun handleCheckOutState(state: CheckInState) {
        when (state) {
            is CheckInState.Idle -> {
                binding.progressLoading.visibility = View.GONE
            }
            is CheckInState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.btnScan.isEnabled = false
                binding.btnManualAction.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true

                val booking = state.booking
                showNotification(
                    "✅ Check-Out Successful\n" +
                    "Vehicle: ${booking.vehicleNumber}\n" +
                    "Amount: ₹${booking.amount}",
                    NotificationType.SUCCESS
                )

                viewModel.resetCheckOutState()
                binding.etVehicleNumber.text?.clear()
            }
            is CheckInState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScan.isEnabled = true
                binding.btnManualAction.isEnabled = true

                showNotification(
                    "❌ Check-Out Failed\n${state.message}",
                    NotificationType.ERROR
                )

                viewModel.resetCheckOutState()
            }
        }
    }

    enum class NotificationType {
        SUCCESS, ERROR, INFO
    }

    private fun showNotification(message: String, type: NotificationType) {
        when (type) {
            NotificationType.SUCCESS -> {
                NotificationHelper.showSuccess(
                    parent = binding.root as ViewGroup,
                    message = message,
                    duration = 3000L
                )
            }
            NotificationType.ERROR -> {
                NotificationHelper.showError(
                    parent = binding.root as ViewGroup,
                    message = message,
                    duration = 3000L
                )
            }
            NotificationType.INFO -> {
                NotificationHelper.showInfo(
                    parent = binding.root as ViewGroup,
                    message = message,
                    duration = 3000L
                )
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan vehicles",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun logout() {
        // Clear SharedPreferences
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
