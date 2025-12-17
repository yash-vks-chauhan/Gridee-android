package com.gridee.parking.ui.fragments

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.data.repository.ParkingRepository
import com.gridee.parking.databinding.FragmentHomeBinding
import com.gridee.parking.ui.adapters.ParkingSpotHomeAdapter
import com.gridee.parking.ui.MainViewModel
import com.gridee.parking.ui.base.BaseTabFragment
import kotlinx.coroutines.launch

class HomeFragment : BaseTabFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: MainViewModel
    private val parkingRepository = ParkingRepository()
    private lateinit var parkingSpotAdapter: ParkingSpotHomeAdapter

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

        setupClickListeners()
        setupParkingSpots()
        loadParkingSpots()
    }

    private fun setupParkingSpots() {
        parkingSpotAdapter = ParkingSpotHomeAdapter()
        binding.recyclerParkingSpots.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = parkingSpotAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun loadParkingSpots() {
        binding.progressParkingSpots.visibility = View.VISIBLE
        binding.recyclerParkingSpots.visibility = View.GONE
        binding.tvParkingSpotsEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val spots = fetchAllParkingSpots()
                println("DEBUG HomeFragment.loadParkingSpots: Fetched spots size=${spots.size}")
                binding.tvParkingSpotsTitle.text = "Parking Spots (${spots.size})"

                if (spots.isEmpty()) {
                    showEmptyState("No parking spots available")
                } else {
                    // Ensure RecyclerView is ALWAYS visible when we have data
                    binding.progressParkingSpots.visibility = View.GONE
                    binding.tvParkingSpotsEmpty.visibility = View.GONE
                    binding.recyclerParkingSpots.visibility = View.VISIBLE
                    parkingSpotAdapter.submitList(spots)
                    println("DEBUG HomeFragment.loadParkingSpots: RecyclerView set to VISIBLE with ${spots.size} spots")
                }
            } catch (e: Exception) {
                println("DEBUG HomeFragment.loadParkingSpots: Exception - ${e.message}")
                showEmptyState("Unable to load parking spots")
            }
        }
    }

    private suspend fun fetchAllParkingSpots(): List<com.gridee.parking.data.model.ParkingSpot> {
        println("DEBUG HomeFragment.fetchAllParkingSpots: Starting to fetch all parking spots")
        
        // Try primary endpoint first
        try {
            val resp = parkingRepository.getParkingSpots()
            if (resp.isSuccessful) {
                val spots = resp.body() ?: emptyList()
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary /api/parking-spots returned ${spots.size} spots")
                if (spots.isNotEmpty()) {
                    spots.take(3).forEach { spot ->
                        println("DEBUG HomeFragment.fetchAllParkingSpots: Sample spot - id=${spot.id}, name=${spot.name}, available=${spot.available}")
                    }
                    return spots
                }
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint returned empty, trying fallback")
            } else {
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint failed with status=${resp.code()}")
            }
        } catch (e: Exception) {
            println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint exception - ${e.message}")
        }

        // Fallback: aggregate by lot
        println("DEBUG HomeFragment.fetchAllParkingSpots: Using lot-based aggregation fallback")
        return try {
            val lotsResp = parkingRepository.getParkingLots()
            if (!lotsResp.isSuccessful) {
                println("DEBUG HomeFragment.fetchAllParkingSpots: Failed to get lots - status=${lotsResp.code()}")
                return emptyList()
            }

            val lots = lotsResp.body() ?: emptyList()
            println("DEBUG HomeFragment.fetchAllParkingSpots: Got ${lots.size} parking lots")
            
            val combined = mutableListOf<com.gridee.parking.data.model.ParkingSpot>()

            for (lot in lots) {
                println("DEBUG HomeFragment.fetchAllParkingSpots: Processing lot: id=${lot.id}, name=${lot.name}")
                
                val attempts = listOf(lot.name, lot.id).filter { it.isNotBlank() }.distinct()
                var lotSpots: List<com.gridee.parking.data.model.ParkingSpot> = emptyList()

                for (key in attempts) {
                    try {
                        val resp = parkingRepository.getParkingSpotsByLot(key)
                        if (resp.isSuccessful) {
                            val body = resp.body() ?: emptyList()
                            println("DEBUG HomeFragment.fetchAllParkingSpots: Lot '$key' returned ${body.size} spots")
                            if (body.isNotEmpty()) {
                                lotSpots = body
                                break
                            }
                        } else {
                            println("DEBUG HomeFragment.fetchAllParkingSpots: Lot '$key' API failed - status=${resp.code()}")
                        }
                    } catch (e: Exception) {
                        println("DEBUG HomeFragment.fetchAllParkingSpots: Lot '$key' exception - ${e.message}")
                    }
                }

                combined.addAll(lotSpots)
            }

            println("DEBUG HomeFragment.fetchAllParkingSpots: Total aggregated spots: ${combined.size}")
            combined
        } catch (e: Exception) {
            println("DEBUG HomeFragment.fetchAllParkingSpots: Fallback exception - ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun showEmptyState(message: String) {
        binding.progressParkingSpots.visibility = View.GONE
        binding.recyclerParkingSpots.visibility = View.GONE
        binding.tvParkingSpotsEmpty.text = message
        binding.tvParkingSpotsEmpty.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        setupFabListener()
    }
    
    private fun setupFabListener() {
        // Hero Animation Click Listener
        binding.heroAnimation.setOnClickListener {
            val bottomSheet = com.gridee.parking.ui.bottomsheet.UniversalBottomSheet.newInstance(
                lottieFileName = "premium_crown.json"
            )
            bottomSheet.show(parentFragmentManager, com.gridee.parking.ui.bottomsheet.UniversalBottomSheet.TAG)
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
