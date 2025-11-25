package com.gridee.parking.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.databinding.BottomSheetVehiclesQuickActionBinding
import com.gridee.parking.ui.adapters.QuickActionVehicleAdapter
import com.gridee.parking.ui.profile.ProfileViewModel
import com.gridee.parking.utils.JwtTokenManager

class VehiclesQuickActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVehiclesQuickActionBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var vehicleAdapter: QuickActionVehicleAdapter
    private var allVehicles: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as com.google.android.material.bottomsheet.BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVehiclesQuickActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeViewModel()
        
        // Ensure data is loaded
        loadDataIfNeeded()
    }

    private fun setupRecyclerView() {
        vehicleAdapter = QuickActionVehicleAdapter(
            onVehicleClick = { vehicleNumber ->
                openEditVehicle(vehicleNumber)
            },
            onEditClick = { vehicleNumber ->
                openEditVehicle(vehicleNumber)
            }
        )
        
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vehicleAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddVehicleHeader.setOnClickListener {
            openAddVehicle()
        }
        
        binding.btnAddFirstVehicle.setOnClickListener {
            openAddVehicle()
        }
        
        binding.tvManageProfile.setOnClickListener {
            try {
                val intent = android.content.Intent(requireContext(), com.gridee.parking.ui.main.MainContainerActivity::class.java).apply {
                    putExtra(com.gridee.parking.ui.main.MainContainerActivity.EXTRA_TARGET_TAB, com.gridee.parking.ui.components.CustomBottomNavigation.TAB_PROFILE)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
                dismiss()
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Opening Profile...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        profileViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val vehicles = user.vehicleNumbers
                updateUI(vehicles)
            }
        }
    }
    
    private fun updateUI(vehicles: List<String>) {
        allVehicles = vehicles
        if (vehicles.isEmpty()) {
            binding.rvVehicles.isVisible = false
            binding.layoutEmptyState.isVisible = true
            binding.btnAddVehicleHeader.isVisible = false
            binding.etSearchVehicles.isVisible = false
        } else {
            binding.rvVehicles.isVisible = true
            binding.layoutEmptyState.isVisible = false
            binding.btnAddVehicleHeader.isVisible = true
            
            // Show search if 5+ vehicles
            if (vehicles.size >= 5) {
                binding.etSearchVehicles.isVisible = true
            } else {
                binding.etSearchVehicles.isVisible = false
            }
            
            // Initial display (no filter)
            vehicleAdapter.updateVehicles(vehicles)
        }
    }

    private fun setupSearch() {
        binding.etSearchVehicles.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterVehicles(query)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun filterVehicles(query: String) {
        if (query.isEmpty()) {
            vehicleAdapter.updateVehicles(allVehicles)
        } else {
            val filtered = allVehicles.filter { it.contains(query, ignoreCase = true) }
            vehicleAdapter.updateVehicles(filtered)
        }
    }

    private fun loadDataIfNeeded() {
        if (profileViewModel.userProfile.value == null) {
            val jwtManager = JwtTokenManager(requireContext())
            val userId = jwtManager.getUserId()
            if (!userId.isNullOrEmpty()) {
                profileViewModel.loadUserProfile(userId)
            }
        }
    }

    private fun openAddVehicle() {
        AddVehicleBottomSheet { newVehicle ->
            profileViewModel.addVehicle(newVehicle)
        }.show(parentFragmentManager, AddVehicleBottomSheet.TAG)
    }

    private fun openEditVehicle(vehicleNumber: String) {
        EditVehicleBottomSheet(
            vehicleNumber,
            onSave = { newVehicleNumber -> 
                profileViewModel.editVehicle(vehicleNumber, newVehicleNumber)
            }
        ).show(parentFragmentManager, EditVehicleBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "VehiclesQuickActionBottomSheet"
    }
}
