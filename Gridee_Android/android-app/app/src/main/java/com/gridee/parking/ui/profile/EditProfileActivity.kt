package com.gridee.parking.ui.profile

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.ActivityEditProfileBinding
import com.gridee.parking.ui.base.BaseActivity
import com.gridee.parking.ui.bottomsheet.EditPhotoBottomSheet
import java.util.Calendar

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    private lateinit var viewModel: EditProfileViewModel
    private var selectedDob: String = ""
    private var selectedPhotoUri: Uri? = null

    override fun getViewBinding(): ActivityEditProfileBinding {
        return ActivityEditProfileBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[EditProfileViewModel::class.java]
        setupObservers()
        setupClickListeners()
        loadUserData()
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(this) { user ->
            user?.let {
                // Set user initials
                val initials = it.name.split(" ").mapNotNull { name -> 
                    name.firstOrNull()?.uppercaseChar() 
                }.take(2).joinToString("")
                binding.tvUserInitials.text = if (initials.isNotEmpty()) initials else "U"
                
                // Set user info
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
                binding.etPhone.setText(it.phone)
                binding.tvProfileName.text = it.name
                binding.tvProfileEmail.text = it.email
                binding.tvProfilePhone.text = it.phone

                // TODO: Load DOB from user profile when backend supports it
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "Saving..." else "Save Changes"
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                showToast("Profile updated successfully")
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (validateInput(name, email, phone)) {
                viewModel.updateProfile(name, email, phone)
            }
        }
        
        // Date of Birth picker
        binding.etDob.setOnClickListener {
            showDatePicker { date ->
                selectedDob = date
                binding.etDob.setText(date)
            }
        }
        
        // Edit profile photo - Show bottom sheet
        binding.ivEditPhoto.setOnClickListener {
            showEditPhotoBottomSheet()
        }
    }
    
    private fun showEditPhotoBottomSheet() {
        val bottomSheet = EditPhotoBottomSheet { uri ->
            // Handle the selected photo
            selectedPhotoUri = uri
            
            // Update the user initials view to show the actual image
            binding.tvUserInitials.visibility = android.view.View.GONE
            
            // You can load the image into a hidden ImageView or show it in place of initials
            // For now, just show a success message
            showToast("Photo selected! Click Save Changes to update.")
            
            // Optional: Update the preview immediately
            // If you have an ImageView for profile photo, you can do:
            // binding.ivProfilePhoto.setImageURI(uri)
            // binding.ivProfilePhoto.visibility = View.VISIBLE
        }
        
        bottomSheet.show(supportFragmentManager, EditPhotoBottomSheet.TAG)
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        
        datePickerDialog.show()
    }

    private fun validateInput(name: String, email: String, phone: String): Boolean {
        when {
            name.isEmpty() -> {
                binding.tilName.error = "Name is required"
                return false
            }
            name.length < 2 -> {
                binding.tilName.error = "Name must be at least 2 characters"
                return false
            }
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Invalid email format"
                return false
            }
            phone.isEmpty() -> {
                binding.tilPhone.error = "Phone number is required"
                return false
            }
            phone.length < 10 -> {
                binding.tilPhone.error = "Phone number must be at least 10 digits"
                return false
            }
            else -> {
                // Clear any existing errors
                binding.tilName.error = null
                binding.tilEmail.error = null
                binding.tilPhone.error = null
                return true
            }
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        
        if (userId != null && isLoggedIn) {
            viewModel.loadUserProfile(userId)
        } else {
            showToast("User session expired")
            finish()
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
