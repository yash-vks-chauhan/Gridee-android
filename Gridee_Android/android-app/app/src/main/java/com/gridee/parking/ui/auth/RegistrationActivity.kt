package com.gridee.parking.ui.auth

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityRegistrationBinding
import com.gridee.parking.ui.MainActivity

class RegistrationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegistrationBinding
    private val viewModel: RegistrationViewModel by viewModels()
    private var additionalVehicleCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // Password visibility toggle
        var isPasswordVisible = false
        binding.tilPassword.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.tilPassword.endIconDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye)
            } else {
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.tilPassword.endIconDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_eye_off)
            }
            // Move cursor to end
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Add more vehicles click handler
        binding.tvAddMoreVehicles.setOnClickListener {
            addVehicleField()
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }
        
        binding.tvLoginLink.setOnClickListener {
            // Navigate to login activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun addVehicleField() {
        additionalVehicleCount++
        
        // Create new vehicle input field
        val vehicleLayout = LayoutInflater.from(this).inflate(R.layout.item_vehicle_input, null) as LinearLayout
        val tilVehicle = vehicleLayout.findViewById<TextInputLayout>(R.id.tilVehicleAdditional)
        val etVehicle = vehicleLayout.findViewById<TextInputEditText>(R.id.etVehicleAdditional)
        
        // Set hint with number
        etVehicle.hint = "Enter vehicle number ${additionalVehicleCount + 1}"
        
        // Add remove functionality
        tilVehicle.setEndIconOnClickListener {
            removeVehicleField(vehicleLayout)
        }
        
        // Add to container
        binding.llAdditionalVehicles.addView(vehicleLayout)
        
        // Show container with animation if first additional vehicle
        if (additionalVehicleCount == 1) {
            binding.llAdditionalVehicles.visibility = View.VISIBLE
            binding.llAdditionalVehicles.alpha = 0f
            
            ObjectAnimator.ofFloat(binding.llAdditionalVehicles, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
        
        // Animate the new field
        vehicleLayout.alpha = 0f
        vehicleLayout.translationY = 50f
        
        ObjectAnimator.ofFloat(vehicleLayout, "alpha", 0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        ObjectAnimator.ofFloat(vehicleLayout, "translationY", 50f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        // Hide "Add more" text after 3 vehicles
        if (additionalVehicleCount >= 3) {
            binding.tvAddMoreVehicles.visibility = View.GONE
        }
    }
    
    private fun removeVehicleField(vehicleLayout: LinearLayout) {
        // Animate removal
        ObjectAnimator.ofFloat(vehicleLayout, "alpha", 1f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        ObjectAnimator.ofFloat(vehicleLayout, "translationX", 0f, 100f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        // Remove after animation
        vehicleLayout.postDelayed({
            binding.llAdditionalVehicles.removeView(vehicleLayout)
            additionalVehicleCount--
            
            // Show "Add more" button if less than 3 vehicles
            if (additionalVehicleCount < 3) {
                binding.tvAddMoreVehicles.visibility = View.VISIBLE
            }
            
            // Hide container if no additional vehicles
            if (additionalVehicleCount == 0) {
                binding.llAdditionalVehicles.visibility = View.GONE
            }
        }, 200)
    }
    
    private fun registerUser() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val confirmPassword = password // Use same password since no confirm field
        
        // Collect all vehicle numbers
        val vehicleNumbers = mutableListOf<String>()
        
        // Primary vehicle
        val primaryVehicle = binding.etVehicle1.text.toString().trim()
        if (primaryVehicle.isNotEmpty()) {
            vehicleNumbers.add(primaryVehicle)
        }
        
        // Additional vehicles
        for (i in 0 until binding.llAdditionalVehicles.childCount) {
            val vehicleLayout = binding.llAdditionalVehicles.getChildAt(i) as LinearLayout
            val etVehicle = vehicleLayout.findViewById<TextInputEditText>(R.id.etVehicleAdditional)
            val vehicleNumber = etVehicle.text.toString().trim()
            if (vehicleNumber.isNotEmpty()) {
                vehicleNumbers.add(vehicleNumber)
            }
        }
        
        viewModel.registerUser(name, email, phone, password, confirmPassword, vehicleNumbers)
    }
    
    private fun observeViewModel() {
        viewModel.registrationState.observe(this) { state ->
            when (state) {
                is RegistrationState.Loading -> {
                    showLoading(true)
                }
                is RegistrationState.Success -> {
                    showLoading(false)
                    
                    // Save user data to SharedPreferences
                    val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
                    sharedPref.edit()
                        .putString("user_id", state.user.id)
                        .putString("user_name", state.user.name)
                        .putString("user_email", state.user.email)
                        .putString("user_phone", state.user.phone)
                        .putBoolean("is_logged_in", true)
                        .apply()
                    
                    Toast.makeText(this, "Registration successful! Welcome to Gridee!", Toast.LENGTH_LONG).show()
                    
                    // Navigate directly to main activity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("USER_NAME", state.user.name)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is RegistrationState.Error -> {
                    showLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        
        viewModel.validationErrors.observe(this) { errors ->
            clearErrors()
            errors.forEach { (field, message) ->
                when (field) {
                    "name" -> binding.tilName.error = message
                    "email" -> binding.tilEmail.error = message
                    "phone" -> binding.tilPhone.error = message
                    "password" -> binding.tilPassword.error = message
                    "vehicle" -> binding.tilVehicle1.error = message
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnRegister.text = if (show) "" else "Register"
    }
    
    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilPassword.error = null
        binding.tilVehicle1.error = null
    }
}
