package com.gridee.parking.ui.fragments

import android.animation.ValueAnimator
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.FragmentProfileBinding
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.bottomsheet.AddVehicleBottomSheet
import com.gridee.parking.ui.bottomsheet.EditVehicleBottomSheet
import com.gridee.parking.ui.profile.ProfileViewModel
import com.gridee.parking.utils.NotificationHelper

class ProfileFragment : BaseTabFragment<FragmentProfileBinding>() {

    private lateinit var viewModel: ProfileViewModel
    private var isVehiclesExpanded = false

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
        viewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        
        // Setup collapsing toolbar with smooth parallax
        setupCollapsingToolbar()
        
        setupObservers()
        setupClickListeners()
        loadUserData()
    }
    
    private fun setupCollapsingToolbar() {
        binding.collapsingToolbar.title = "Profile"
        
        // Add offset change listener for smooth transitions
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange.toFloat()
            val scrollPercentage = Math.abs(verticalOffset / totalScrollRange)
            
            // Smooth alpha transition for better visual feedback
            binding.collapsingToolbar.alpha = 1f - (scrollPercentage * 0.1f)
        }
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
            // Use the root view as anchor since this is from a dialog
            showVehicleOptionsDialog(vehicles[which], binding.root)
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
                val vehicleCount = it.vehicleNumbers.size
                binding.tvVehicleCount.text = if (vehicleCount == 1) {
                    "$vehicleCount vehicle"
                } else {
                    "$vehicleCount vehicles"
                }
                
                // Populate vehicles in accordion if it's already expanded
                if (isVehiclesExpanded) {
                    populateVehicles(it.vehicleNumbers, animate = false)
                }
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

        // Vehicle Management category - Accordion Animation
        binding.btnMyVehicles.setOnClickListener {
            toggleVehiclesAccordion()
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

    private fun showVehicleOptionsDialog(vehicleNumber: String, anchorView: View) {
        // Create PopupMenu with custom styling
        val wrapper = android.view.ContextThemeWrapper(requireContext(), com.gridee.parking.R.style.PopupMenuTheme)
        val popup = android.widget.PopupMenu(wrapper, anchorView)
        popup.menuInflater.inflate(com.gridee.parking.R.menu.menu_vehicle_options, popup.menu)
        
        // Force show icons in popup menu
        try {
            val fieldMPopup = android.widget.PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Check if this vehicle is already default
        val isDefault = viewModel.userProfile.value?.defaultVehicle == vehicleNumber
        if (isDefault) {
            // Change "Make Default" to "Default" and disable it
            popup.menu.findItem(com.gridee.parking.R.id.action_make_default)?.apply {
                title = "Default ✓"
                isEnabled = false
            }
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                com.gridee.parking.R.id.action_edit_vehicle -> {
                    editVehicle(vehicleNumber)
                    true
                }
                com.gridee.parking.R.id.action_make_default -> {
                    setDefaultVehicle(vehicleNumber)
                    true
                }
                com.gridee.parking.R.id.action_delete_vehicle -> {
                    removeVehicle(vehicleNumber)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun editVehicle(vehicleNumber: String) {
        // Show the edit vehicle bottom sheet with spring animation
        val bottomSheet = EditVehicleBottomSheet(vehicleNumber) { newVehicleNumber ->
            // Update the vehicle in the backend
            viewModel.editVehicle(vehicleNumber, newVehicleNumber.uppercase())
            
            // Show professional success notification
            NotificationHelper.showSuccess(
                parent = binding.root,
                title = "Success",
                message = "Vehicle number saved successfully",
                duration = 3000L
            )
        }
        
        bottomSheet.show(childFragmentManager, EditVehicleBottomSheet.TAG)
    }

    private fun setDefaultVehicle(vehicleNumber: String) {
        viewModel.setDefaultVehicle(vehicleNumber)
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
        // Show the add vehicle bottom sheet with spring animation
        val bottomSheet = AddVehicleBottomSheet { newVehicleNumber ->
            // Add the vehicle to the backend
            viewModel.addVehicle(newVehicleNumber.uppercase())
            
            // Show professional success notification
            NotificationHelper.showSuccess(
                parent = binding.root,
                title = "Success",
                message = "Vehicle added successfully",
                duration = 3000L
            )
        }
        
        bottomSheet.show(childFragmentManager, AddVehicleBottomSheet.TAG)
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

    private fun populateVehicles(vehicles: List<String>, animate: Boolean = false) {
        val container = binding.vehiclesContainer
        container.removeAllViews()
        
        val initialAlpha = if (animate) 0f else 1f
        val initialTranslationY = if (animate) 50f else 0f
        
        if (vehicles.isEmpty()) {
            // Show empty state
            val emptyView = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                container,
                false
            ) as android.widget.TextView
            emptyView.text = "No vehicles added yet\nTap here to add a vehicle"
            emptyView.setTextColor(android.graphics.Color.parseColor("#757575"))
            emptyView.textSize = 14f
            emptyView.gravity = android.view.Gravity.CENTER
            emptyView.setPadding(0, 24, 0, 24)
            emptyView.setOnClickListener {
                showAddVehicleDialog()
            }
            
            // Prepare for animation
            emptyView.alpha = initialAlpha
            emptyView.translationY = initialTranslationY
            
            container.addView(emptyView)
            return
        }
        
        vehicles.forEachIndexed { index, vehicleNumber ->
            val vehicleView = layoutInflater.inflate(
                com.gridee.parking.R.layout.item_vehicle,
                container,
                false
            )
            
            val tvVehicleNumber = vehicleView.findViewById<android.widget.TextView>(com.gridee.parking.R.id.tv_vehicle_number)
            val tvVehicleLabel = vehicleView.findViewById<android.widget.TextView>(com.gridee.parking.R.id.tv_vehicle_label)
            val ivVehicleMenu = vehicleView.findViewById<android.widget.ImageView>(com.gridee.parking.R.id.iv_vehicle_menu)
            val ivDefaultIndicator = vehicleView.findViewById<android.widget.ImageView>(com.gridee.parking.R.id.iv_default_indicator)
            
            tvVehicleNumber.text = vehicleNumber
            tvVehicleLabel.text = "Vehicle ${index + 1}"
            
            // Show default indicator if this is the default vehicle
            val defaultVehicle = viewModel.userProfile.value?.defaultVehicle
            if (vehicleNumber == defaultVehicle) {
                ivDefaultIndicator.visibility = View.VISIBLE
                tvVehicleLabel.text = "Default Vehicle"
            } else {
                ivDefaultIndicator.visibility = View.GONE
            }
            
            // Add margin if not the first item
            if (index > 0) {
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.topMargin = (8 * resources.displayMetrics.density).toInt()
                vehicleView.layoutParams = params
            }
            
            // Add click listener for the entire vehicle item
            vehicleView.setOnClickListener {
                // Optional: Can be used for selection or other actions
            }
            
            // Add click listener for the three-dot menu
            ivVehicleMenu.setOnClickListener {
                showVehicleOptionsDialog(vehicleNumber, it)
            }
            
            // Prepare for animation
            vehicleView.alpha = initialAlpha
            vehicleView.translationY = initialTranslationY
            
            container.addView(vehicleView)
        }
        
        // Add "Add New Vehicle" button at the end
        val addVehicleView = layoutInflater.inflate(
            com.gridee.parking.R.layout.item_add_vehicle,
            container,
            false
        )
        
        // Add margin for spacing
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = (16 * resources.displayMetrics.density).toInt()
        addVehicleView.layoutParams = params
        
        // Add click listener
        addVehicleView.setOnClickListener {
            showAddVehicleDialog()
        }
        
        // Prepare for animation
        addVehicleView.alpha = initialAlpha
        addVehicleView.translationY = initialTranslationY
        
        container.addView(addVehicleView)
    }
    
    private fun toggleVehiclesAccordion() {
        isVehiclesExpanded = !isVehiclesExpanded
        
        val expandedLayout = binding.layoutVehiclesExpanded
        val arrowIcon = binding.ivVehiclesExpand
        val container = binding.vehiclesContainer
        
        if (isVehiclesExpanded) {
            // Populate vehicles with real data and prepare for animation
            val vehicles = viewModel.userProfile.value?.vehicleNumbers ?: emptyList()
            populateVehicles(vehicles, animate = true)
            
            // Prepare for expansion
            expandedLayout.alpha = 0f
            expandedLayout.visibility = View.VISIBLE
            
            // Measure the expanded height
            // Calculate available width: Screen width - Card margins (16dp * 2)
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val cardMargin = (32 * displayMetrics.density).toInt()
            val availableWidth = screenWidth - cardMargin
            
            val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
            
            expandedLayout.measure(
                widthSpec,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = expandedLayout.measuredHeight
            
            // Height animation
            val heightAnimation = ValueAnimator.ofInt(0, targetHeight)
            heightAnimation.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val layoutParams = expandedLayout.layoutParams
                layoutParams.height = value
                expandedLayout.layoutParams = layoutParams
            }
            heightAnimation.duration = 400
            heightAnimation.interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
            heightAnimation.start()
            
            // Alpha animation for the container (dividers etc)
            expandedLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // Staggered reveal animation for children
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(100L + (i * 50L)) // Stagger delay
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            
            // Rotate arrow icon
            arrowIcon.animate()
                .rotation(180f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
                
        } else {
            // Collapse animation
            val initialHeight = expandedLayout.height
            
            // Height animation
            val heightAnimation = ValueAnimator.ofInt(initialHeight, 0)
            heightAnimation.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val layoutParams = expandedLayout.layoutParams
                layoutParams.height = value
                expandedLayout.layoutParams = layoutParams
                
                // Hide when fully collapsed
                if (value == 0) {
                    expandedLayout.visibility = View.GONE
                }
            }
            heightAnimation.duration = 300
            heightAnimation.interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
            heightAnimation.start()
            
            // Fade out container
            expandedLayout.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // Fade out children quickly
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                child.animate()
                    .alpha(0f)
                    .translationY(20f)
                    .setDuration(200)
                    .setStartDelay(0)
                    .start()
            }
            
            // Rotate arrow icon back
            arrowIcon.animate()
                .rotation(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
