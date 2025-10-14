package com.gridee.parking.ui.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.R
import com.gridee.parking.databinding.FragmentHomeBinding
import com.gridee.parking.ui.MainViewModel
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.search.SearchActivity

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
        animateSearchBarEntrance()
    }
    
    private fun animateSearchBarEntrance() {
        // Set initial state
        binding.cardSearch.alpha = 0f
        binding.cardSearch.translationY = 40f
        binding.cardSearch.scaleX = 0.95f
        binding.cardSearch.scaleY = 0.95f
        
        // Animate to final state
        binding.cardSearch.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
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
        setupSearchBarInteractions()
        setupFabListener()
    }
    
    private fun setupSearchBarInteractions() {
        binding.cardSearch.setOnClickListener {
            openSearchExperience()
        }
        binding.ivSearchIcon.setOnClickListener { binding.cardSearch.performClick() }
        binding.tvSearchPlaceholder.setOnClickListener { binding.cardSearch.performClick() }
        binding.ivSearchArrow.setOnClickListener { binding.cardSearch.performClick() }
    }

    private fun openSearchExperience() {
        val context = requireContext()
        val intent = Intent(context, SearchActivity::class.java)
        val transitionName = binding.cardSearch.transitionName ?: getString(R.string.transition_search_bar)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            binding.cardSearch,
            transitionName
        )
        startActivity(intent, options.toBundle())
    }
    
    private fun setupFabListener() {
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
