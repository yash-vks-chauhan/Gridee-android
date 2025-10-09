package com.gridee.parking.ui.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.FragmentHomeBinding
import com.gridee.parking.ui.MainViewModel
import com.gridee.parking.ui.base.BaseTabFragment

class HomeFragment : BaseTabFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: MainViewModel

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun getScrollableView(): View? {
        return try {
            binding.scrollContent
        } catch (e: IllegalStateException) {
            null
        }
    }

    override fun setupUI() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupUserWelcome()
        setupClickListeners()
    }

    private fun setupUserWelcome() {
        // Get user name from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        val userName = arguments?.getString("USER_NAME") 
            ?: sharedPref.getString("user_name", "User") 
            ?: "User"
        
        binding.tvWelcome.text = "Welcome back, $userName!"
    }

    private fun setupClickListeners() {
        binding.cardSearch.setOnClickListener {
            // Navigate to Find Parking screen
            try {
                val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.discovery.ParkingDiscoveryActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Find Parking feature coming soon!")
            }
        }
        
        // FAB click listener - parking lot selection
        binding.fabBookParking.setOnClickListener {
            try {
                // Navigate to parking lot selection first
                val intent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.booking.ParkingLotSelectionActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback to ParkingDiscoveryActivity if ParkingLotSelectionActivity fails
                try {
                    val fallbackIntent = Intent(requireContext(), Class.forName("com.gridee.parking.ui.discovery.ParkingDiscoveryActivity"))
                    startActivity(fallbackIntent)
                } catch (fallbackException: Exception) {
                    showToast("Unable to open parking selection")
                }
            }
        }
    }

    companion object {
        fun newInstance(userName: String? = null): HomeFragment {
            val fragment = HomeFragment()
            userName?.let {
                val args = android.os.Bundle()
                args.putString("USER_NAME", it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
