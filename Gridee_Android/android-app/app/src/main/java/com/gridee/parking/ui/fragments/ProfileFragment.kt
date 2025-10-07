package com.gridee.parking.ui.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gridee.parking.databinding.FragmentProfileBinding
import com.gridee.parking.ui.base.BaseTabFragment

class ProfileFragment : BaseTabFragment<FragmentProfileBinding>() {

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
        setupUserInfo()
        setupClickListeners()
    }

    private fun setupUserInfo() {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "User") ?: "User"
        val userEmail = sharedPref.getString("user_email", "user@example.com") ?: "user@example.com"
        
        binding.tvUserName.text = userName
        binding.tvUserEmail.text = userEmail
    }

    private fun setupClickListeners() {
        binding.cardVehicles.setOnClickListener {
            showToast("My Vehicles feature coming soon!")
        }

        binding.cardNotifications.setOnClickListener {
            showToast("Notifications settings coming soon!")
        }

        binding.cardLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Clear any stored user data
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        // Navigate back to login
        try {
            val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.auth.LoginActivity"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            showToast("Unable to logout at this time")
        }
    }
}
