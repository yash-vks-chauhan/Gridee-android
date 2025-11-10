package com.gridee.parking.ui.operator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gridee.parking.databinding.ActivityOperatorDashboardBinding
import com.gridee.parking.ui.auth.LoginActivity
import com.gridee.parking.ui.qr.QrScannerActivity

/**
 * Dashboard for parking lot operators
 * Allows operators to check-in/check-out vehicles by scanning license plates
 */
class OperatorDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorDashboardBinding
    private val viewModel: OperatorViewModel by viewModels()

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    private val vehicleScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == QrScannerActivity.RESULT_QR_SCANNED) {
            val scannedData = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE)
            scannedData?.let { vehicleNumber ->
                // Determine if this was check-in or check-out
                val scanType = result.data?.getStringExtra(QrScannerActivity.EXTRA_SCAN_TYPE)
                when (scanType) {
                    "VEHICLE_CHECK_IN" -> viewModel.checkInByVehicleNumber(vehicleNumber)
                    "VEHICLE_CHECK_OUT" -> viewModel.checkOutByVehicleNumber(vehicleNumber)
                    else -> viewModel.checkInByVehicleNumber(vehicleNumber)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOperatorDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color
        window.statusBarColor = android.graphics.Color.parseColor("#1E88E5")

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Get operator info from SharedPreferences
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val operatorName = sharedPref.getString("user_name", "Operator")
        val parkingLotName = sharedPref.getString("parking_lot_name", "Parking Lot")

        binding.tvOperatorName.text = "Welcome, $operatorName"
        binding.tvParkingLotName.text = parkingLotName

        // Scan Vehicle Button (Check-In)
        binding.btnScanVehicle.setOnClickListener {
            if (checkCameraPermission()) {
                openVehicleScanner(isCheckOut = false)
            } else {
                requestCameraPermission()
            }
        }

        // Manual Check-In Button
        binding.btnManualCheckIn.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            if (vehicleNumber.isNotBlank()) {
                viewModel.checkInByVehicleNumber(vehicleNumber)
                binding.etVehicleNumber.text?.clear()
            } else {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
            }
        }

        // Check-Out Button
        binding.btnCheckOut.setOnClickListener {
            if (checkCameraPermission()) {
                openVehicleScanner(isCheckOut = true)
            } else {
                requestCameraPermission()
            }
        }

        // Manual Check-Out Button
        binding.btnManualCheckOut.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            if (vehicleNumber.isNotBlank()) {
                viewModel.checkOutByVehicleNumber(vehicleNumber)
                binding.etVehicleNumber.text?.clear()
            } else {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout Button
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun openVehicleScanner(isCheckOut: Boolean) {
        val intent = Intent(this, QrScannerActivity::class.java).apply {
            putExtra(
                QrScannerActivity.EXTRA_SCAN_TYPE,
                if (isCheckOut) "VEHICLE_CHECK_OUT" else "VEHICLE_CHECK_IN"
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
                binding.progressBar.visibility = View.GONE
            }
            is CheckInState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnScanVehicle.isEnabled = false
                binding.btnManualCheckIn.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.btnScanVehicle.isEnabled = true
                binding.btnManualCheckIn.isEnabled = true

                val booking = state.booking
                Toast.makeText(
                    this,
                    """
                    ✅ Check-In Successful
                    Vehicle: ${booking.vehicleNumber}
                    Slot: ${booking.spotId ?: "N/A"}
                    Time: ${booking.checkInTime}
                    """.trimIndent(),
                    Toast.LENGTH_LONG
                ).show()

                viewModel.resetCheckInState()
            }
            is CheckInState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnScanVehicle.isEnabled = true
                binding.btnManualCheckIn.isEnabled = true

                Toast.makeText(
                    this,
                    "❌ Check-In Failed\n${state.message}",
                    Toast.LENGTH_LONG
                ).show()

                viewModel.resetCheckInState()
            }
        }
    }

    private fun handleCheckOutState(state: CheckInState) {
        when (state) {
            is CheckInState.Idle -> {
                binding.progressBar.visibility = View.GONE
            }
            is CheckInState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnCheckOut.isEnabled = false
                binding.btnManualCheckOut.isEnabled = false
            }
            is CheckInState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.btnCheckOut.isEnabled = true
                binding.btnManualCheckOut.isEnabled = true

                val booking = state.booking
                Toast.makeText(
                    this,
                    """
                    ✅ Check-Out Successful
                    Vehicle: ${booking.vehicleNumber}
                    Duration: ${calculateDuration(booking)}
                    Amount: ${booking.amount}
                    """.trimIndent(),
                    Toast.LENGTH_LONG
                ).show()

                viewModel.resetCheckOutState()
            }
            is CheckInState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnCheckOut.isEnabled = true
                binding.btnManualCheckOut.isEnabled = true

                Toast.makeText(
                    this,
                    "❌ Check-Out Failed\n${state.message}",
                    Toast.LENGTH_LONG
                ).show()

                viewModel.resetCheckOutState()
            }
        }
    }

    private fun calculateDuration(booking: com.gridee.parking.data.model.Booking): String {
        // TODO: Calculate duration between checkInTime and checkOutTime
        return "N/A"
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
