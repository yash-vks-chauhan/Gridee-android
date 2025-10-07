package com.gridee.parking.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.ActivityEditProfileBinding
import com.gridee.parking.ui.base.BaseActivity

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    private lateinit var viewModel: EditProfileViewModel

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
                binding.etName.setText(it.name)
                binding.etEmail.setText(it.email)
                binding.etPhone.setText(it.phone)
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
