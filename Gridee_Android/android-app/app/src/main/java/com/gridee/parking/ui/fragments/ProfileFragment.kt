package com.gridee.parking.ui.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.FragmentProfileBinding
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.profile.ProfileViewModel

class ProfileFragment : BaseTabFragment<FragmentProfileBinding>() {

    private lateinit var viewModel: ProfileViewModel

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun getScrollableView(): View? {
        return try {
            binding.scrollContent
        } catch (e: IllegalStateException) {
            null
        }
    }

    override fun setupUI() {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    private fun showVehicleManagementDialog() {
        val vehicles = viewModel.userProfile.value?.vehicleNumbers ?: emptyList()
        val options = if (vehicles.isEmpty()) {
            arrayOf("Add Vehicle")
        } else {
            arrayOf("View Vehicles", "Add Vehicle")
        }
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Vehicle Management")
        builder.setItems(options) { _, which ->
            when {
                vehicles.isEmpty() && which == 0 -> showAddVehicleDialog()
                vehicles.isNotEmpty() && which == 0 -> showVehicleListDialog()
                vehicles.isNotEmpty() && which == 1 -> showAddVehicleDialog()
            }
        }
        builder.show()
    }

    private fun showVehicleListDialog() {
        val vehicles = viewModel.userProfile.value?.vehicleNumbers ?: emptyList()
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("My Vehicles")
        builder.setItems(vehicles.toTypedArray()) { _, which ->
            showVehicleOptionsDialog(vehicles[which])
        }
        builder.setPositiveButton("Add Vehicle") { _, _ ->
            showAddVehicleDialog()
        }
        builder.setNegativeButton("Close", null)
        builder.show()
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(this) { user ->
            user?.let {
                // Set user initials in the avatar
                val initials = it.name.split(" ").mapNotNull { name -> 
                    name.firstOrNull()?.uppercaseChar() 
                }.take(2).joinToString("")
                binding.tvUserInitials.text = if (initials.isNotEmpty()) initials else "U"
                
                // Set user info
                binding.tvUserName.text = it.name
                
                // Update vehicle count 
                binding.tvVehicleCount.text = "${it.vehicleNumbers.size} vehicles"
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
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
                val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
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
        // Account section click
        binding.btnEditProfile.setOnClickListener {
            try {
                val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.profile.EditProfileActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Edit Profile - Coming Soon!")
            }
        }

        // Account & Profile category
        binding.btnAccountSettings.setOnClickListener {
            showToast("Account Settings - Coming Soon!")
        }

        binding.btnSecurityPrivacy.setOnClickListener {
            showToast("Security & Privacy - Coming Soon!")
        }

        binding.btnNotifications.setOnClickListener {
            showToast("Notification Settings - Coming Soon!")
        }

        // Vehicle Management category
        binding.btnMyVehicles.setOnClickListener {
            showVehicleManagementDialog()
        }

        binding.btnParkingPreferences.setOnClickListener {
            showToast("Parking Preferences - Coming Soon!")
        }

        // App Preferences category
        binding.btnDisplayTheme.setOnClickListener {
            showToast("Display & Theme - Coming Soon!")
        }

        binding.btnSoundsVibration.setOnClickListener {
            showToast("Sounds & Vibration - Coming Soon!")
        }

        binding.btnLanguageRegion.setOnClickListener {
            showToast("Language & Region - Coming Soon!")
        }

        // Support & Legal category
        binding.btnHelpSupport.setOnClickListener {
            showToast("Help & Support - Coming Soon!")
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            try {
                val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.profile.PrivacySettingsActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Privacy Policy - Coming Soon!")
            }
        }

        binding.btnAbout.setOnClickListener {
            showToast("About - Coming Soon!")
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
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
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter new vehicle number"
        input.setText(vehicleNumber) // Pre-fill with current vehicle number
        input.selectAll() // Select all text for easy editing
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Remove Vehicle")
        builder.setMessage("Are you sure you want to remove vehicle $vehicleNumber?")
        builder.setPositiveButton("Remove") { _, _ ->
            viewModel.removeVehicle(vehicleNumber)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddVehicleDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter vehicle number (e.g., MH01AB1234)"
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")
        builder.setPositiveButton("Logout") { _, _ ->
            viewModel.logout()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.auth.LoginActivity"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            showToast("Unable to logout at this time")
        }
    }

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
