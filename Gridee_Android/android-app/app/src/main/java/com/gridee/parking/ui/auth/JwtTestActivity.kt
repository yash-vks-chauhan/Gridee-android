package com.gridee.parking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.gridee.parking.R
import com.gridee.parking.data.repository.UserRepository
import com.gridee.parking.ui.main.MainContainerActivity
import com.gridee.parking.utils.JwtTokenManager

/**
 * Dedicated test activity for JWT authentication
 * 
 * This activity is designed specifically for testing the JWT authentication
 * implementation without interfering with your existing login flow.
 * 
 * Features:
 * - Test login with JWT
 * - View stored token
 * - Check authentication status
 * - Test logout
 * - View all auth data
 * 
 * To use:
 * 1. Add to AndroidManifest.xml as LAUNCHER (temporarily)
 * 2. Build and install app
 * 3. Test all features
 * 4. Remove LAUNCHER when done testing
 */
class JwtTestActivity : AppCompatActivity() {
    
    private val TAG = "JwtTestActivity"
    private val viewModel: JwtLoginViewModel by viewModels()
    
    // UI Elements
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnCheckAuth: Button
    private lateinit var btnViewToken: Button
    private lateinit var btnViewUserInfo: Button
    private lateinit var btnFetchOAuth2User: Button
    private lateinit var btnLogout: Button
    private lateinit var btnClearLogs: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvLogs: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jwt_test)
        
        initViews()
        setupClickListeners()
        observeViewModel()
        
        // Check initial auth status
        checkAuthenticationStatus()
        
        addLog("✅ JWT Test Activity initialized")
        addLog("📱 Backend URL: ${com.gridee.parking.config.ApiConfig.BASE_URL}")
    }
    
    private fun initViews() {
        etEmail = findViewById(R.id.etTestEmail)
        etPassword = findViewById(R.id.etTestPassword)
        btnLogin = findViewById(R.id.btnTestLogin)
        btnCheckAuth = findViewById(R.id.btnCheckAuth)
        btnViewToken = findViewById(R.id.btnViewToken)
        btnViewUserInfo = findViewById(R.id.btnViewUserInfo)
        btnFetchOAuth2User = findViewById(R.id.btnFetchOAuth2User)
        btnLogout = findViewById(R.id.btnTestLogout)
        btnClearLogs = findViewById(R.id.btnClearLogs)
        progressBar = findViewById(R.id.progressBarTest)
        tvStatus = findViewById(R.id.tvStatus)
        tvLogs = findViewById(R.id.tvLogs)
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            testLogin()
        }
        
        btnCheckAuth.setOnClickListener {
            checkAuthenticationStatus()
        }
        
        btnViewToken.setOnClickListener {
            viewToken()
        }
        
        btnViewUserInfo.setOnClickListener {
            viewUserInfo()
        }

        btnFetchOAuth2User.setOnClickListener {
            fetchOAuth2User()
        }
        
        btnLogout.setOnClickListener {
            testLogout()
        }
        
        btnClearLogs.setOnClickListener {
            clearLogs()
        }
    }

    private fun fetchOAuth2User() {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("🌐 Fetching OAuth2 user from /api/oauth2/user...")

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = UserRepository().getOAuth2User()
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val body = response.body()
                    addLog("✅ OAuth2 user info fetched successfully")
                    addLog("📦 Response: ${body}")

                    AlertDialog.Builder(this@JwtTestActivity)
                        .setTitle("OAuth2 User Info")
                        .setMessage(body?.entries?.joinToString("\n") { (k, v) -> "$k: $v" } ?: "<empty>")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val err = response.errorBody()?.string()
                    addLog("❌ OAuth2 user fetch failed: ${response.code()} ${response.message()}")
                    addLog("📝 Error Body: ${err}")
                    Toast.makeText(this@JwtTestActivity, "Error: ${response.code()} ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                addLog("❌ Network error while fetching OAuth2 user: ${e.message}")
                Toast.makeText(this@JwtTestActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is JwtAuthState.Idle -> {
                    progressBar.visibility = View.GONE
                    updateStatus("⚪ Idle", android.R.color.darker_gray)
                }
                
                is JwtAuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    updateStatus("🔄 Loading...", android.R.color.holo_blue_light)
                    addLog("🔄 Login in progress...")
                }
                
                is JwtAuthState.Success -> {
                    progressBar.visibility = View.GONE
                    updateStatus("✅ Authenticated", android.R.color.holo_green_light)
                    
                    val auth = state.authResponse
                    addLog("✅ LOGIN SUCCESS!")
                    addLog("📝 Token: ${auth.token.take(20)}...")
                    addLog("👤 User ID: ${auth.id}")
                    addLog("👤 Name: ${auth.name}")
                    addLog("🎭 Role: ${auth.role}")
                    
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Show success dialog
                    showSuccessDialog(auth.name)
                }
                
                is JwtAuthState.Error -> {
                    progressBar.visibility = View.GONE
                    updateStatus("❌ Error", android.R.color.holo_red_light)
                    addLog("❌ LOGIN FAILED: ${state.message}")
                    
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                
                is JwtAuthState.LoggedOut -> {
                    progressBar.visibility = View.GONE
                    updateStatus("🚪 Logged Out", android.R.color.darker_gray)
                    addLog("🚪 User logged out successfully")
                    
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.validationErrors.observe(this) { errors ->
            if (errors.isNotEmpty()) {
                errors.forEach { (field, message) ->
                    addLog("⚠️ Validation Error - $field: $message")
                }
            }
        }
    }
    
    private fun testLogin() {
        val email = etEmail.text.toString()
        val password = etPassword.text.toString()
        
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("🔐 Starting JWT Login Test")
        addLog("📧 Email: $email")
        addLog("🔑 Password: ${password.take(3)}***")
        
        viewModel.loginWithJwt(this, email, password)
    }
    
    private fun checkAuthenticationStatus() {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("🔍 Checking authentication status...")
        
        val isAuth = viewModel.checkAuthentication(this)
        
        if (isAuth) {
            updateStatus("✅ Authenticated", android.R.color.holo_green_light)
            addLog("✅ User IS authenticated")
            
            val jwtManager = JwtTokenManager(this)
            addLog("👤 User ID: ${jwtManager.getUserId()}")
            addLog("👤 Name: ${jwtManager.getUserName()}")
            addLog("🎭 Role: ${jwtManager.getUserRole()}")
        } else {
            updateStatus("❌ Not Authenticated", android.R.color.holo_red_light)
            addLog("❌ User is NOT authenticated")
        }
    }
    
    private fun viewToken() {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("🔑 Viewing JWT Token...")
        
        val jwtManager = JwtTokenManager(this)
        val token = jwtManager.getAuthToken()
        
        if (token != null) {
            addLog("✅ Token found!")
            addLog("📝 Token: ${token.take(50)}...")
            addLog("📝 Bearer: ${jwtManager.getBearerToken()?.take(50)}...")
            addLog("📏 Length: ${token.length} characters")
            
            // Show full token in dialog
            AlertDialog.Builder(this)
                .setTitle("JWT Token")
                .setMessage(token)
                .setPositiveButton("Copy") { _, _ ->
                    copyToClipboard(token)
                }
                .setNegativeButton("Close", null)
                .show()
        } else {
            addLog("❌ No token found")
            Toast.makeText(this, "No token available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun viewUserInfo() {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("👤 Viewing User Info...")
        
        val jwtManager = JwtTokenManager(this)
        val authData = jwtManager.getAuthData()
        
        val info = StringBuilder()
        authData.forEach { (key, value) ->
            val displayValue = if (key == "token" && value != null) {
                "${value.take(30)}..."
            } else {
                value ?: "null"
            }
            addLog("📋 $key: $displayValue")
            info.append("$key: $displayValue\n")
        }
        
        // Show in dialog
        AlertDialog.Builder(this)
            .setTitle("Authentication Data")
            .setMessage(info.toString())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun testLogout() {
        addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        addLog("🚪 Testing Logout...")
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to logout? This will clear all JWT tokens.")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout(this)
                clearInputFields()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearLogs() {
        tvLogs.text = ""
        addLog("🧹 Logs cleared")
    }
    
    private fun clearInputFields() {
        etEmail.text.clear()
        etPassword.text.clear()
    }
    
    private fun updateStatus(status: String, colorResId: Int) {
        tvStatus.text = status
        tvStatus.setTextColor(resources.getColor(colorResId, theme))
    }
    
    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logMessage = "[$timestamp] $message\n"
        
        Log.d(TAG, message)
        
        runOnUiThread {
            tvLogs.append(logMessage)
            // Auto-scroll to bottom
            val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollViewLogs)
            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("JWT Token", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Token copied to clipboard", Toast.LENGTH_SHORT).show()
    }
    
    private fun showSuccessDialog(userName: String) {
        AlertDialog.Builder(this)
            .setTitle("Login Successful! 🎉")
            .setMessage("Welcome back, $userName!\n\nWhat would you like to do?")
            .setPositiveButton("Go to Main App") { _, _ ->
                navigateToMainApp()
            }
            .setNegativeButton("Continue Testing", null)
            .show()
    }
    
    private fun navigateToMainApp() {
        val intent = Intent(this, MainContainerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
