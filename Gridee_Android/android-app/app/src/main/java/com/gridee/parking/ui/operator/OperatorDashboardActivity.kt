package com.gridee.parking.ui.operator

import android.Manifest
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityOperatorDashboardBinding
import com.gridee.parking.ui.auth.LoginActivity
import com.gridee.parking.ui.qr.QrScannerActivity
import com.gridee.parking.utils.AuthSession
import com.gridee.parking.utils.NotificationHelper

/**
 * Dashboard for parking lot operators
 * Ultra-clean minimal design with progressive disclosure
 * Features single-action scan mode + hidden manual entry
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding
    private val viewModel: OperatorViewModel by viewModels()
    private var currentMode = OperatorMode.CHECK_IN
    private var isManualEntryMode = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val ANIM_DURATION = 200L
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

        if (!AuthSession.isAuthenticated(this)) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(LoginActivity.EXTRA_FORCE_LOGIN, true)
            startActivity(intent)
            finish()
            return
        }
        AuthSession.syncLegacyPrefsFromJwt(this)

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

        // Circular Scan Button (Primary CTA)
        binding.btnScanCircular.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            animateButtonPress(it) {
                if (checkCameraPermission()) {
                    openVehicleScanner()
                } else {
                    requestCameraPermission()
                }
            }
        }

        // Manual Entry Link
        binding.linkManualEntry.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            switchToManualEntry()
        }

        // Back to Scan
        binding.btnBackToScan.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            switchToScanMode()
        }

        // Submit Manual Button
        binding.btnSubmitManual.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            val vehicleNumber = binding.etVehicleNumberClean.text.toString().trim()
            
            when {
                vehicleNumber.isBlank() -> {
                    binding.etVehicleNumberClean.error = "Enter vehicle number"
                    binding.etVehicleNumberClean.requestFocus()
                }
                vehicleNumber.replace(" ", "").length < 8 -> {
                    binding.etVehicleNumberClean.error = "Invalid vehicle number"
                    binding.etVehicleNumberClean.requestFocus()
                }
                else -> {
                    when (currentMode) {
                        OperatorMode.CHECK_IN -> viewModel.checkInByVehicleNumber(vehicleNumber.replace(" ", ""))
                        OperatorMode.CHECK_OUT -> viewModel.checkOutByVehicleNumber(vehicleNumber.replace(" ", ""))
                    }
                }
            }
        }

        // Auto-format vehicle number
        binding.etVehicleNumberClean.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val cleaned = s.toString().replace(" ", "").uppercase()
                
                val formatted = when {
                    cleaned.length <= 2 -> cleaned
                    cleaned.length <= 4 -> "${cleaned.substring(0, 2)} ${cleaned.substring(2)}"
                    cleaned.length <= 6 -> "${cleaned.substring(0, 2)} ${cleaned.substring(2, 4)} ${cleaned.substring(4)}"
                    else -> "${cleaned.substring(0, 2)} ${cleaned.substring(2, 4)} ${cleaned.substring(4, 6)} ${cleaned.substring(6).take(4)}"
                }
                
                if (formatted != s.toString()) {
                    s?.replace(0, s.length, formatted)
                }
                
                isFormatting = false
            }
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etVehicleNumberClean.error = null
            }
        })

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, android.R.color.black)
        )
    }

    private fun switchToManualEntry() {
        isManualEntryMode = true
        
        // Fade transition
        binding.layoutScanMode.animate()
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                binding.layoutScanMode.visibility = View.GONE
                binding.layoutManualMode.visibility = View.VISIBLE
                binding.layoutManualMode.alpha = 0f
                binding.layoutManualMode.animate()
                    .alpha(1f)
                    .setDuration(ANIM_DURATION)
                    .start()
                
                // Focus on input
                binding.etVehicleNumberClean.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etVehicleNumberClean, InputMethodManager.SHOW_IMPLICIT)
            }
            .start()
    }

    private fun switchToScanMode() {
        isManualEntryMode = false
        
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etVehicleNumberClean.windowToken, 0)
        
        // Clear input
        binding.etVehicleNumberClean.text?.clear()
        
        // Fade transition
        binding.layoutManualMode.animate()
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                binding.layoutManualMode.visibility = View.GONE
                binding.layoutScanMode.visibility = View.VISIBLE
                binding.layoutScanMode.alpha = 0f
                binding.layoutScanMode.animate()
                    .alpha(1f)
                    .setDuration(ANIM_DURATION)
                    .start()
            }
            .start()
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
        // Update scan label
        val scanLabel = when (mode) {
            OperatorMode.CHECK_IN -> "Tap to Scan Vehicle"
            OperatorMode.CHECK_OUT -> "Tap to Scan Vehicle"
        }
        binding.tvScanLabel.text = scanLabel
        
        // Update submit button text
        val submitText = when (mode) {
            OperatorMode.CHECK_IN -> "Check In"
            OperatorMode.CHECK_OUT -> "Check Out"
        }
        binding.btnSubmitManual.text = submitText
        
        // If in manual mode, clear input
        if (isManualEntryMode) {
            binding.etVehicleNumberClean.text?.clear()
        }
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
        
        // Make background transparent to show rounded corners
        bottomSheetDialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
        
        // Get operator info from SharedPreferences
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val operatorName = sharedPref.getString("user_name", "Operator")
        val parkingLotName = sharedPref.getString("parking_lot_name", "Parking Lot")
        
        // Set operator info
        bottomSheetView.findViewById<android.widget.TextView>(R.id.tv_operator_name)?.text = operatorName
        bottomSheetView.findViewById<android.widget.TextView>(R.id.tv_parking_lot)?.text = parkingLotName
        
        // Settings
        bottomSheetView.findViewById<android.view.View>(R.id.menu_settings)?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            bottomSheetDialog.dismiss()
            showNotification("Settings coming soon", NotificationType.INFO)
        }
        
        // Help & Support
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
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(R.layout.dialog_logout_confirmation)
        
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnLogout = dialog.findViewById<MaterialButton>(R.id.btn_logout)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnLogout.setOnClickListener {
            dialog.dismiss()
            logout()
        }
        
        dialog.show()
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
                binding.btnScanCircular.isEnabled = false
                binding.btnSubmitManual.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScanCircular.isEnabled = true
                binding.btnSubmitManual.isEnabled = true

                val booking = state.booking
                showNotification(
                    "✅ Check-In Successful\nVehicle: ${booking.vehicleNumber}",
                    NotificationType.SUCCESS
                )

                // Return to scan mode
                if (isManualEntryMode) {
                    binding.etVehicleNumberClean.text?.clear()
                    switchToScanMode()
                }
                
                viewModel.resetCheckInState()
            }
            is CheckInState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScanCircular.isEnabled = true
                binding.btnSubmitManual.isEnabled = true

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
                binding.btnScanCircular.isEnabled = false
                binding.btnSubmitManual.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScanCircular.isEnabled = true
                binding.btnSubmitManual.isEnabled = true

                val booking = state.booking
                showNotification(
                    "✅ Check-Out Successful\nVehicle: ${booking.vehicleNumber}",
                    NotificationType.SUCCESS
                )

                // Return to scan mode
                if (isManualEntryMode) {
                    binding.etVehicleNumberClean.text?.clear()
                    switchToScanMode()
                }
                
                viewModel.resetCheckOutState()
            }
            is CheckInState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.btnScanCircular.isEnabled = true
                binding.btnSubmitManual.isEnabled = true

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
        AuthSession.clearSession(this)

        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
