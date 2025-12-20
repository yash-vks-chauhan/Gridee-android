package com.gridee.parking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityRegistrationBinding
import com.gridee.parking.ui.main.MainContainerActivity

class RegistrationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegistrationBinding
    private val viewModel: RegistrationViewModel by viewModels()
    private var parkingLotAdapter: ArrayAdapter<String>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set light status bar with dark icons for white background
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = android.graphics.Color.parseColor("#F5F5F5")
        
        setupUI()
        observeViewModel()
        viewModel.loadParkingLotNames()
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

        binding.etParkingLot.setOnItemClickListener { _, _, position, _ ->
            val selection = parkingLotAdapter?.getItem(position)
            binding.etParkingLot.setText(selection ?: "", false)
            binding.tilParkingLot.error = null
        }

        binding.etParkingLot.setOnClickListener {
            binding.etParkingLot.showDropDown()
        }

        binding.etParkingLot.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etParkingLot.post { binding.etParkingLot.showDropDown() }
            }
        }

        binding.btnRegister.setOnClickListener {
            registerUser()
        }
        
        binding.tvLoginLink.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            // Navigate to login activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun registerUser() {
        val name = binding.etName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val password = binding.etPassword.text.toString()
        val parkingLotName = binding.etParkingLot.text?.toString()?.trim() ?: ""
        
        viewModel.registerUser(
            context = this,
            name = name,
            email = email,
            phone = phone,
            password = password,
            parkingLotName = parkingLotName
        )
    }
    
    private fun observeViewModel() {
        viewModel.parkingLotLoading.observe(this) { isLoading ->
            binding.parkingLotProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.etParkingLot.isEnabled = !isLoading
            binding.tilParkingLot.isEnabled = !isLoading
        }

        viewModel.parkingLotNames.observe(this) { names ->
            parkingLotAdapter = ArrayAdapter(this, R.layout.item_dropdown_menu, names)
            binding.etParkingLot.setAdapter(parkingLotAdapter)
        }

        viewModel.parkingLotError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

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
                        .putString("parking_lot_name", state.user.parkingLotName)
                        .putBoolean("is_logged_in", true)
                        .apply()
                    
                    Toast.makeText(this, "Registration successful! Welcome to Gridee!", Toast.LENGTH_LONG).show()
                    
                    // Navigate directly to main container (same as login)
                    val intent = Intent(this, MainContainerActivity::class.java)
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
                    "parkingLot" -> binding.tilParkingLot.error = message
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
        binding.tilParkingLot.error = null
    }
}
