package com.gridee.parking.ui.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.repository.ParkingRepository
import com.gridee.parking.databinding.FragmentHomeBinding
import com.gridee.parking.ui.adapters.ParkingSpotHomeAdapter
import com.gridee.parking.ui.MainViewModel
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.bottomsheet.ParkingSpotBottomSheet
import kotlinx.coroutines.launch

class HomeFragment : BaseTabFragment<FragmentHomeBinding>() {

    private lateinit var viewModel: MainViewModel
    private val parkingRepository = ParkingRepository()
    private lateinit var parkingSpotAdapter: ParkingSpotHomeAdapter
    private var isLoadingParkingSpots: Boolean = false

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
        refreshParkingSpots(showBlockingLoading = true)
    }

    private fun setupParkingSpots() {
        parkingSpotAdapter = ParkingSpotHomeAdapter { spot ->
            openParkingSpotBottomSheet(spot)
        }
        binding.recyclerParkingSpots.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = parkingSpotAdapter
            edgeEffectFactory = com.gridee.parking.ui.utils.BounceEdgeEffectFactory()
            
            // Remove any existing ItemDecorations if we added them previously (safe guard)
            // if (itemDecorationCount > 0) removeItemDecorationAt(0)
            
            // Add snapping for the "one after another" feel
            val snapHelper = androidx.recyclerview.widget.PagerSnapHelper()
            // Reset snap helper if attached previously to avoid crash "SnapHelper is already attached"
            binding.recyclerParkingSpots.onFlingListener = null 
            snapHelper.attachToRecyclerView(this)
        }
    }
    
    private fun openParkingSpotBottomSheet(spot: ParkingSpot) {
        val lotName = spot.lotName?.takeIf { it.isNotBlank() }
            ?: spot.zoneName?.takeIf { it.isNotBlank() }
            ?: spot.name?.takeIf { it.isNotBlank() }
            ?: ""
        
        val sheet = ParkingSpotBottomSheet.newInstance(
            parkingSpotId = spot.id,
            parkingLotId = spot.lotId,
            parkingLotName = lotName
        )
        sheet.show(parentFragmentManager, ParkingSpotBottomSheet.TAG)
    }

    private fun refreshParkingSpots(showBlockingLoading: Boolean) {
        if (isLoadingParkingSpots) return
        isLoadingParkingSpots = true

        setParkingSpotsRefreshing(isRefreshing = true, showBlockingLoading = showBlockingLoading)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val spots = fetchAllParkingSpots()
                println("DEBUG HomeFragment.refreshParkingSpots: Fetched spots size=${spots.size}")
                binding.tvParkingSpotsTitle.text = "Parking Spots (${spots.size})"

                if (spots.isEmpty()) {
                    showEmptyState("No parking spots available")
                } else {
                    binding.progressParkingSpots.visibility = View.GONE
                    binding.tvParkingSpotsEmpty.visibility = View.GONE
                    binding.recyclerParkingSpots.visibility = View.VISIBLE
                    parkingSpotAdapter.submitList(spots)
                    println("DEBUG HomeFragment.refreshParkingSpots: RecyclerView set to VISIBLE with ${spots.size} spots")
                }
            } catch (e: Exception) {
                println("DEBUG HomeFragment.refreshParkingSpots: Exception - ${e.message}")
                showEmptyState("Unable to load parking spots")
            } finally {
                isLoadingParkingSpots = false
                setParkingSpotsRefreshing(isRefreshing = false, showBlockingLoading = showBlockingLoading)
            }
        }
    }

    private fun setParkingSpotsRefreshing(isRefreshing: Boolean, showBlockingLoading: Boolean) {
        binding.btnRefreshParkingSpots.isEnabled = !isRefreshing
        binding.progressRefreshParkingSpots.visibility = if (isRefreshing) View.VISIBLE else View.GONE
        binding.btnRefreshParkingSpots.visibility = if (isRefreshing) View.INVISIBLE else View.VISIBLE

        if (showBlockingLoading) {
            binding.progressParkingSpots.visibility = if (isRefreshing) View.VISIBLE else View.GONE
            if (isRefreshing) {
                binding.recyclerParkingSpots.visibility = View.GONE
                binding.tvParkingSpotsEmpty.visibility = View.GONE
            }
        }
    }

    private suspend fun fetchAllParkingSpots(): List<com.gridee.parking.data.model.ParkingSpot> {
        println("DEBUG HomeFragment.fetchAllParkingSpots: Starting to fetch all parking spots")
        
        // ✅ CURRENT WORKING SOLUTION: Fetch all spots directly
        try {
            val resp = parkingRepository.getParkingSpots()
            println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint status=${resp.code()}, success=${resp.isSuccessful}")
            
            if (resp.isSuccessful) {
                val spots = resp.body() ?: emptyList()
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary /api/parking-spots returned ${spots.size} spots")
                if (spots.isNotEmpty()) {
                    spots.take(3).forEach { spot ->
                        println("DEBUG HomeFragment.fetchAllParkingSpots: Sample spot - id=${spot.id}, name=${spot.name}, available=${spot.available}")
                    }
                    return spots
                }
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint returned empty")
            } else {
                val errorBody = resp.errorBody()?.string()
                println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint failed - status=${resp.code()}, error=$errorBody")
            }
        } catch (e: Exception) {
            println("DEBUG HomeFragment.fetchAllParkingSpots: Primary endpoint exception - ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }

        // Return empty if primary fails
        return emptyList()

        /* ========================================
         * 📦 OLD LOT-BASED AGGREGATION APPROACH
         * ========================================
         * This code fetches parking lots first, then aggregates spots from each lot.
         * Currently COMMENTED OUT because direct /api/parking-spots endpoint works.
         * Keeping this for reference in case we need lot-based filtering in future.
         * 
         * To re-enable: Uncomment this block and remove the "return emptyList()" above.
         */
        
        /*
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
                            val errorBody = resp.errorBody()?.string()
                            println("DEBUG HomeFragment.fetchAllParkingSpots: Lot '$key' API failed - status=${resp.code()}, error=$errorBody")
                        }
                    } catch (e: Exception) {
                        println("DEBUG HomeFragment.fetchAllParkingSpots: Lot '$key' exception - ${e.javaClass.simpleName}: ${e.message}")
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
        */
    }

    private fun showEmptyState(message: String) {
        binding.progressParkingSpots.visibility = View.GONE
        binding.recyclerParkingSpots.visibility = View.GONE
        binding.tvParkingSpotsEmpty.text = message
        binding.tvParkingSpotsEmpty.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnRefreshParkingSpots.setOnClickListener {
            refreshParkingSpots(showBlockingLoading = false)
        }

        // Hero Animation Click Listener
        binding.heroAnimation.setOnClickListener {
            val bottomSheet = com.gridee.parking.ui.bottomsheet.UniversalBottomSheet.newInstance(
                lottieFileName = "premium_crown.json",
                isRewardMode = true
            )
            bottomSheet.show(parentFragmentManager, com.gridee.parking.ui.bottomsheet.UniversalBottomSheet.TAG)
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
