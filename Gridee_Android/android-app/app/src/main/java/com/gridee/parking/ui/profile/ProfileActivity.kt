package com.gridee.parking.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.databinding.ActivityProfileBinding
import com.gridee.parking.ui.auth.LoginActivity
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation

class ProfileActivity : BaseActivityWithBottomNav<ActivityProfileBinding>() {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var vehicleAdapter: VehicleAdapter
    
    companion object {
        private const val EDIT_PROFILE_REQUEST_CODE = 1001
    }

    override fun getViewBinding(): ActivityProfileBinding {
        return ActivityProfileBinding.inflate(layoutInflater)
    }

    override fun getCurrentTab(): Int {
        return CustomBottomNavigation.TAB_PROFILE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    override fun setupUI() {
        // UI setup will be handled in loadUserData() and observers
        
        // Setup scroll behavior for RecyclerView
        setupScrollBehaviorForView(binding.rvVehicles)
    }

    private fun setupRecyclerView() {
        vehicleAdapter = VehicleAdapter { vehicle ->
            // Handle vehicle item click (edit/delete)
            showVehicleOptionsDialog(vehicle)
        }
        
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = vehicleAdapter
        }
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(this) { user ->
            user?.let {
                binding.tvUserName.text = it.name
                binding.tvUserEmail.text = it.email
                binding.tvUserPhone.text = it.phone.ifEmpty { "Not provided" }
                binding.tvMemberSince.text = "Member since registration"
                
                // Update vehicle count and show vehicles
                binding.tvVehicleCount.text = "${it.vehicleNumbers.size} Vehicle(s)"
                vehicleAdapter.updateVehicles(it.vehicleNumbers)
                
                // Show/hide empty state for vehicles
                if (it.vehicleNumbers.isEmpty()) {
                    binding.rvVehicles.visibility = android.view.View.GONE
                    binding.tvNoVehicles.visibility = android.view.View.VISIBLE
                } else {
                    binding.rvVehicles.visibility = android.view.View.VISIBLE
                    binding.tvNoVehicles.visibility = android.view.View.GONE
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Show/hide loading indicator
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.logoutSuccess.observe(this) { success ->
            if (success) {
                // Clear user data from SharedPreferences
                val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
                sharedPref.edit().apply {
                    remove("user_id")
                    putBoolean("is_logged_in", false)
                    apply()
                }
                navigateToLogin()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            // Navigate to Edit Profile screen
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivityForResult(intent, EDIT_PROFILE_REQUEST_CODE)
        }

        binding.btnAddVehicle.setOnClickListener {
            showAddVehicleDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            // TODO: Navigate to Change Password screen
            showToast("Change Password - Coming Soon!")
        }

        binding.btnNotifications.setOnClickListener {
            // TODO: Navigate to Notification Settings
            showToast("Notification Settings - Coming Soon!")
        }

        binding.btnPrivacy.setOnClickListener {
            // Navigate to Privacy Settings
            val intent = Intent(this, PrivacySettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnHelp.setOnClickListener {
            // TODO: Navigate to Help & Support
            showToast("Help & Support - Coming Soon!")
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        
        if (userId != null && isLoggedIn) {
            viewModel.loadUserProfile(userId)
        } else if (isLoggedIn) {
            // User is logged in but we don't have user ID, ask them to log in again
            showToast("Please log in again to access your profile")
            navigateToLogin()
        } else {
            showToast("User session expired")
            navigateToLogin()
        }
    }

    private fun showVehicleOptionsDialog(vehicleNumber: String) {
        val options = arrayOf("Edit Vehicle", "Remove Vehicle")
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Vehicle Options")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> editVehicle(vehicleNumber)
                1 -> removeVehicle(vehicleNumber)
            }
        }
        builder.show()
    }

    private fun editVehicle(vehicleNumber: String) {
        val input = android.widget.EditText(this)
        input.hint = "Enter new vehicle number"
        input.setText(vehicleNumber) // Pre-fill with current vehicle number
        input.selectAll() // Select all text for easy editing
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Edit Vehicle")
        builder.setMessage("Edit vehicle number:")
        builder.setView(input)
        builder.setPositiveButton("Update") { _, _ ->
            val newVehicleNumber = input.text.toString().trim().uppercase()
            if (newVehicleNumber.isNotEmpty()) {
                viewModel.editVehicle(vehicleNumber, newVehicleNumber)
            } else {
                showToast("Please enter a valid vehicle number")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
        
        // Focus on the input and show keyboard
        input.requestFocus()
    }

    private fun removeVehicle(vehicleNumber: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Remove Vehicle")
        builder.setMessage("Are you sure you want to remove vehicle $vehicleNumber?")
        builder.setPositiveButton("Remove") { _, _ ->
            viewModel.removeVehicle(vehicleNumber)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddVehicleDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter vehicle number (e.g., MH01AB1234)"
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Add Vehicle")
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val vehicleNumber = input.text.toString().trim().uppercase()
            if (vehicleNumber.isNotEmpty()) {
                viewModel.addVehicle(vehicleNumber)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showLogoutConfirmation() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Logout") { _, _ ->
            viewModel.logout()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun formatDate(dateString: String?): String {
        // Simple date formatting - you can enhance this
        return dateString?.substring(0, 10) ?: "Unknown"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // Reload user data after profile edit
            loadUserData()
        }
    }
}
