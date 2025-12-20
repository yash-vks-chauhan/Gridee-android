package com.gridee.parking.ui.bottomsheet

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.model.TopUpRequest
import com.gridee.parking.databinding.BottomSheetParkingSpotBinding
import com.gridee.parking.ui.booking.BookingConfirmationActivity
import com.gridee.parking.ui.booking.BookingViewModel
import com.gridee.parking.ui.booking.ParkingSpotSelectionAdapter
import com.gridee.parking.ui.booking.VehicleSelectionAdapter
import com.gridee.parking.ui.wallet.WalletTopUpActivity
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ParkingSpotBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetParkingSpotBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BookingViewModel

    private var parkingSpot: ParkingSpot? = null
    private var selectedLotId: String = ""
    private var selectedLotName: String = ""
    private var selectedSpotId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)

        arguments?.let { args ->
            selectedSpotId = args.getString(ARG_PARKING_SPOT_ID).orEmpty()
            selectedLotId = args.getString(ARG_PARKING_LOT_ID).orEmpty()
            selectedLotName = args.getString(ARG_PARKING_LOT_NAME).orEmpty()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog

            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null
                sheet.fitsSystemWindows = false

                val params = sheet.layoutParams as? ViewGroup.MarginLayoutParams
                params?.setMargins(0, 0, 0, 0)
                sheet.layoutParams = params

                ViewCompat.setOnApplyWindowInsetsListener(sheet) { view, insets ->
                    view.setPadding(0, 0, 0, 0)
                    insets
                }
            }

            bottomSheetDialog.behavior.isGestureInsetBottomIgnored = true

            bottomSheetDialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val navBarColor =
                    ContextCompat.getColor(requireContext(), R.color.background_primary)
                window.navigationBarColor = navBarColor
                window.isNavigationBarContrastEnforced = false

                val wic = WindowCompat.getInsetsController(window, window.decorView)
                wic.isAppearanceLightNavigationBars = true

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.attributes.blurBehindRadius = 50
                    window.attributes = window.attributes
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetParkingSpotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookingViewModel::class.java]

        setupInsets()
        setupBehaviors()
        setupUI()
        setupClickListeners()
        setupObservers()

        loadParkingSpot(selectedSpotId)
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { bottomSheetDialog ->
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams = bottomSheet?.layoutParams?.apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            bottomSheet?.requestLayout()
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetDialog.behavior.skipCollapsed = true
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserVehicles()
        viewModel.loadWalletBalance()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bars.bottom)
            insets
        }
    }

    private fun setupBehaviors() {
        val bottomSheetBehavior = (dialog as? BottomSheetDialog)?.behavior
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED ||
                    newState == BottomSheetBehavior.STATE_HIDDEN
                ) {
                    bottomSheet.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                }

                if (newState == BottomSheetBehavior.STATE_COLLAPSED ||
                    newState == BottomSheetBehavior.STATE_EXPANDED
                ) {
                    animateHandle(32)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                animateHandle(48)
            }
        })
    }

    private fun animateHandle(targetWidthDp: Int) {
        val targetWidthPx = (targetWidthDp * resources.displayMetrics.density).toInt()
        if (binding.dragHandle.layoutParams.width != targetWidthPx) {
            val params = binding.dragHandle.layoutParams
            params.width = targetWidthPx
            binding.dragHandle.layoutParams = params
        }
    }

    private fun setupUI() {
        binding.tvTitle.text = "Book Parking"

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        if (currentHour >= 20) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            viewModel.setStartTime(calendar.time)

            calendar.add(Calendar.HOUR_OF_DAY, 2)
            viewModel.setEndTime(calendar.time)
        } else {
            if (currentHour < 6) {
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }

            viewModel.setStartTime(calendar.time)
            calendar.add(Calendar.HOUR_OF_DAY, 2)
            viewModel.setEndTime(calendar.time)
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.cardStartTime.setOnClickListener { showDateTimePicker(true) }
        binding.cardEndTime.setOnClickListener { showDateTimePicker(false) }
        binding.btnSelectSpot.setOnClickListener { showSpotSelectionDialog() }
        binding.cardVehicleSelection.setOnClickListener { showVehicleSelectionDialog() }
        binding.btnContinueToPayment.setOnClickListener { createBooking() }
        binding.btnAddMoney.setOnClickListener { showAddMoneyDialog() }
    }

    private fun setupObservers() {
        viewModel.startTime.observe(viewLifecycleOwner) { time ->
            updateStartTimeDisplay(time)
            calculatePricing()
        }

        viewModel.endTime.observe(viewLifecycleOwner) { time ->
            updateEndTimeDisplay(time)
            calculatePricing()
        }

        viewModel.selectedSpot.observe(viewLifecycleOwner) { spot ->
            updateSelectedSpotDisplay(spot)
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            binding.tvTotalPrice.text = "₹${String.format(Locale.getDefault(), "%.2f", price)}"
        }

        viewModel.duration.observe(viewLifecycleOwner) { duration ->
            binding.tvDuration.text = duration
        }

        viewModel.selectedVehicle.observe(viewLifecycleOwner) { vehicle ->
            binding.tvSelectedVehicle.text = vehicle?.number ?: "Select your vehicle"
        }

        viewModel.walletBalance.observe(viewLifecycleOwner) { balance ->
            binding.tvWalletBalance.text = "₹${String.format(Locale.getDefault(), "%.2f", balance)}"
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnContinueToPayment.isEnabled = !isLoading
        }

        viewModel.bookingCreated.observe(viewLifecycleOwner) { booking ->
            booking?.let {
                val startMillis = viewModel.startTime.value?.time ?: System.currentTimeMillis()
                val endMillis = viewModel.endTime.value?.time ?: (startMillis + 60 * 60 * 1000)
                val totalAmount = viewModel.totalPrice.value ?: 0.0
                val selectedSpotName = viewModel.selectedSpot.value
                    ?: parkingSpot?.name
                    ?: parkingSpot?.zoneName
                val parkingName = selectedLotName.ifEmpty { binding.tvParkingName.text.toString() }
                val parkingAddress = binding.tvParkingAddress.text.toString()
                val vehicleNumber = viewModel.selectedVehicle.value?.number

                val confirmationIntent = Intent(requireContext(), BookingConfirmationActivity::class.java).apply {
                    putExtra("BOOKING_ID", it.id ?: "")
                    putExtra("TRANSACTION_ID", it.qrCode ?: "")
                    putExtra("PARKING_NAME", parkingName)
                    putExtra("PARKING_ADDRESS", parkingAddress)
                    putExtra("SELECTED_SPOT", selectedSpotName)
                    putExtra("PARKING_SPOT_ID", it.spotId)
                    putExtra("VEHICLE_NUMBER", vehicleNumber)
                    putExtra("START_TIME", startMillis)
                    putExtra("END_TIME", endMillis)
                    putExtra("TOTAL_AMOUNT", totalAmount)
                    putExtra("PAYMENT_METHOD", "Wallet")
                    putExtra("PAYMENT_STATUS", it.status ?: "Pending")
                    putExtra("BOOKING_TIMESTAMP", it.createdAt?.time ?: System.currentTimeMillis())
                }

                startActivity(confirmationIntent)
                viewModel.clearBookingCreated()
                dismissAllowingStateLoss()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }
    }

    private fun loadParkingSpot(spotId: String) {
        if (spotId == "quick_book") {
            parkingSpot = createDefaultParkingSpot()
            updateParkingSpotDisplay()
            return
        }

        if (spotId.isNotEmpty()) {
            viewModel.loadParkingSpotById(spotId) { spot ->
                parkingSpot = spot ?: ParkingSpot(
                    id = spotId,
                    lotId = selectedLotId,
                    spotCode = spotId,
                    name = "Selected Spot",
                    zoneName = "Unknown Spot",
                    capacity = 0,
                    available = 0,
                    status = "unknown"
                )
                updateParkingSpotDisplay()
            }
            return
        }

        parkingSpot = ParkingSpot(
            id = "unknown",
            lotId = selectedLotId,
            spotCode = null,
            name = null,
            zoneName = null,
            capacity = 0,
            available = 0,
            status = "unknown"
        )
        updateParkingSpotDisplay()
    }

    private fun updateParkingSpotDisplay() {
        parkingSpot?.let { spot ->
            if (selectedLotId.isBlank() && spot.lotId.isNotBlank()) {
                selectedLotId = spot.lotId
            }

            binding.tvParkingName.text =
                if (selectedLotName.isNotEmpty()) selectedLotName else "Unknown Location"

            val addressText = when {
                !spot.zoneName.isNullOrBlank() && spot.zoneName != spot.name -> spot.zoneName
                selectedLotId.isNotEmpty() -> "Lot ID: $selectedLotId"
                else -> "Address unavailable"
            }
            binding.tvParkingAddress.text = addressText

            binding.tvHourlyRate.text = "₹${String.format(Locale.getDefault(), "%.2f", 2.5)}/hour"

            val spotName = spot.name ?: spot.zoneName ?: spot.spotCode ?: "Any available spot"
            binding.tvSelectedSpot.text = spotName
            viewModel.setSelectedSpot(spotName)

            viewModel.setParkingSpot(spot)
            viewModel.loadUserVehicles()
        }
    }

    private fun updateSelectedSpotDisplay(spot: String?) {
        binding.tvSelectedSpot.text = spot ?: "Any available spot"
    }

    private fun showDateTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val currentTime = if (isStartTime) viewModel.startTime.value else viewModel.endTime.value
        currentTime?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        if (isStartTime) {
                            viewModel.setStartTime(calendar.time)
                        } else {
                            viewModel.setEndTime(calendar.time)
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showSpotSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_spot_selection, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val recyclerView =
            dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_parking_spots)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        val emptyState = dialogView.findViewById<android.widget.TextView>(R.id.tv_empty_state)
        val cardAnySpot = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.card_any_spot)
        val ivAnySpotSelected = dialogView.findViewById<android.widget.ImageView>(R.id.iv_any_spot_selected)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btn_cancel)
        val btnSelect = dialogView.findViewById<android.widget.Button>(R.id.btn_select)

        var selectedSpot: ParkingSpot? = null

        val currentSelectedSpotText = binding.tvSelectedSpot.text.toString()
        var isAnySpotSelected = currentSelectedSpotText == "Any available spot"

        val spotAdapter = ParkingSpotSelectionAdapter { spot ->
            selectedSpot = spot
            isAnySpotSelected = false
            updateSpotSelection(ivAnySpotSelected, false)
            showToast("Selected spot: ${spot.name ?: spot.zoneName ?: spot.spotCode ?: spot.id}")
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = spotAdapter

        showProgress(progressBar, recyclerView, emptyState, true)

        println("ParkingSpotBottomSheet: Loading spots for lot ID: '$selectedLotId'")
        showToast("Loading spots for lot: $selectedLotId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val parkingRepository = com.gridee.parking.data.repository.ParkingRepository()
                val start = viewModel.startTime.value ?: Calendar.getInstance().time
                val end =
                    viewModel.endTime.value ?: Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) }.time
                val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                val startStr = df.format(start)
                val endStr = df.format(end)
                val spotsResponse = if (selectedLotId.isNotEmpty()) {
                    parkingRepository.getAvailableSpots(selectedLotId, startStr, endStr)
                } else {
                    Response.success(emptyList())
                }

                if (!isAdded || _binding == null) return@launch

                if (spotsResponse.isSuccessful) {
                    val filteredSpots = spotsResponse.body() ?: emptyList()

                    showToast("Filtered spots for lot '$selectedLotId': ${filteredSpots.size}")
                    println("ParkingSpotBottomSheet: Received ${filteredSpots.size} spots for lot $selectedLotId")

                    showProgress(progressBar, recyclerView, emptyState, false)

                    if (filteredSpots.isNotEmpty()) {
                        spotAdapter.submitList(filteredSpots)
                        recyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE

                        recyclerView.requestLayout()

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isAdded) showToast("Adapter item count: ${spotAdapter.itemCount}")
                        }, 500)

                        if (!isAnySpotSelected) {
                            val currentSpot = filteredSpots.find { spot ->
                                val spotName = spot.name ?: spot.zoneName ?: ""
                                spotName == currentSelectedSpotText
                            }
                            if (currentSpot != null) {
                                selectedSpot = currentSpot
                                spotAdapter.setSelectedSpot(currentSpot.id)
                            }
                        }
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyState.visibility = View.VISIBLE
                    }
                } else {
                    showProgress(progressBar, recyclerView, emptyState, false)
                    showToast("API call failed: ${spotsResponse.code()}")
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                if (!isAdded || _binding == null) return@launch
                showProgress(progressBar, recyclerView, emptyState, false)
                showToast("Error loading spots: ${e.message}")
                recyclerView.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            }
        }

        cardAnySpot.setOnClickListener {
            selectedSpot = null
            isAnySpotSelected = true
            updateSpotSelection(ivAnySpotSelected, true)
            spotAdapter.setSelectedSpot(null)
            showToast("Selected: Any available spot")
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSelect.setOnClickListener {
            println("ParkingSpotBottomSheet: Select clicked. isAnySpotSelected: $isAnySpotSelected, selectedSpot: ${selectedSpot?.id}")

            if (isAnySpotSelected) {
                binding.tvSelectedSpot.text = "Any available spot"
                viewModel.setSelectedSpot(null)
            } else {
                selectedSpot?.let { spot ->
                    val spotName = spot.name ?: spot.zoneName ?: "Selected Spot"
                    binding.tvSelectedSpot.text = spotName
                    binding.tvSelectedSpot.requestLayout()
                    binding.tvSelectedSpot.invalidate()
                    viewModel.setSelectedSpot(spotName)
                } ?: run {
                    binding.tvSelectedSpot.text = "Any available spot"
                    viewModel.setSelectedSpot(null)
                }
            }
            dialog.dismiss()
        }

        updateSpotSelection(ivAnySpotSelected, isAnySpotSelected)
        dialog.show()
    }

    private fun updateSpotSelection(imageView: android.widget.ImageView, isSelected: Boolean) {
        imageView.setImageResource(
            if (isSelected) R.drawable.ic_radio_button_checked else R.drawable.ic_radio_button_unchecked
        )
    }

    private fun showProgress(
        progressBar: android.widget.ProgressBar,
        content: View,
        emptyState: android.widget.TextView,
        show: Boolean
    ) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            content.visibility = View.GONE
            emptyState.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun showVehicleSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vehicle_selection, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_vehicles)
        val btnAddVehicle = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.btn_add_vehicle)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btn_cancel)
        val btnSelect = dialogView.findViewById<android.widget.Button>(R.id.btn_select)

        val vehicles = viewModel.userVehicles.value ?: emptyList()
        var selectedVehicle: com.gridee.parking.data.model.Vehicle? = null

        val adapter = VehicleSelectionAdapter(vehicles) { vehicle ->
            selectedVehicle = vehicle
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.selectedVehicle.value?.let { currentVehicle ->
            val position = vehicles.indexOfFirst { it.id == currentVehicle.id }
            if (position >= 0) {
                adapter.setSelectedPosition(position)
                selectedVehicle = currentVehicle
            }
        }

        btnAddVehicle.setOnClickListener {
            dialog.dismiss()
            showAddVehicleDialog { newVehicleNumber ->
                viewModel.addVehicleToProfile(newVehicleNumber) { success ->
                    if (!isAdded) return@addVehicleToProfile
                    if (success) {
                        showToast("Vehicle added successfully!")
                        viewModel.loadUserVehicles()
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isAdded) showVehicleSelectionDialog()
                        }, 500)
                    } else {
                        showToast("Failed to add vehicle. Please check your connection.")
                    }
                }
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSelect.setOnClickListener {
            selectedVehicle?.let { vehicle ->
                viewModel.setSelectedVehicle(vehicle)
                dialog.dismiss()
            } ?: run {
                showToast("Please select a vehicle")
            }
        }

        dialog.show()
    }

    private fun showAddVehicleDialog(onVehicleAdded: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            hint = "Enter vehicle number (e.g., MH01AB1234)"
        }

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Add Vehicle")
        builder.setView(input)
        builder.setPositiveButton("Add") { _, _ ->
            val vehicleNumber = input.text.toString().trim().uppercase()
            if (vehicleNumber.isNotEmpty()) {
                onVehicleAdded(vehicleNumber)
            } else {
                showToast("Please enter a vehicle number")
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateStartTimeDisplay(time: Date) {
        val formatter = SimpleDateFormat("MMM dd, yyyy\nhh:mm a", Locale.getDefault())
        binding.tvStartTime.text = formatter.format(time)
    }

    private fun updateEndTimeDisplay(time: Date) {
        val formatter = SimpleDateFormat("MMM dd, yyyy\nhh:mm a", Locale.getDefault())
        binding.tvEndTime.text = formatter.format(time)
    }

    private fun calculatePricing() {
        viewModel.calculatePricing()
    }

    private fun createBooking() {
        val selectedVehicle = viewModel.selectedVehicle.value

        if (selectedVehicle == null) {
            showToast("Please select a vehicle")
            return
        }

        viewModel.setVehicleNumber(selectedVehicle.number)
        viewModel.createBackendBooking()
    }

    private fun showAddMoneyDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter amount (e.g., 500)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Money to Wallet")
        builder.setMessage("Enter the amount you want to add to your wallet:")
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val amountText = input.text.toString().trim()
            if (amountText.isNotEmpty()) {
                try {
                    val amount = amountText.toDouble()
                    if (amount > 0) {
                        initiateWalletTopUp(amount)
                    } else {
                        showToast("Please enter a valid amount")
                    }
                } catch (e: NumberFormatException) {
                    showToast("Invalid amount format")
                }
            } else {
                showToast("Please enter an amount")
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun initiateWalletTopUp(amount: Double) {
        val sharedPref = requireContext().getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)

        if (userId == null) {
            showToast("Please log in to add money")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val topUpRequest = TopUpRequest(amount)
                val response = ApiClient.apiService.topUpWallet(userId, topUpRequest)

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        val intent = Intent(requireContext(), WalletTopUpActivity::class.java).apply {
                            putExtra("USER_ID", userId)
                            putExtra("AMOUNT", amount)
                            putExtra("ORDER_ID", result.orderId ?: "")
                            putExtra("KEY_ID", result.keyId)
                        }
                        startActivity(intent)
                    } else {
                        showToast("Failed to initiate payment")
                    }
                } else {
                    showToast("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun createDefaultParkingSpot(): ParkingSpot {
        return ParkingSpot(
            id = "default_spot",
            lotId = selectedLotId,
            name = null,
            zoneName = null,
            capacity = 0,
            available = 0,
            status = "unknown"
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ParkingSpotBottomSheet"

        private const val ARG_PARKING_SPOT_ID = "PARKING_SPOT_ID"
        private const val ARG_PARKING_LOT_ID = "PARKING_LOT_ID"
        private const val ARG_PARKING_LOT_NAME = "PARKING_LOT_NAME"

        fun newInstance(
            parkingSpotId: String,
            parkingLotId: String,
            parkingLotName: String
        ): ParkingSpotBottomSheet {
            return ParkingSpotBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARKING_SPOT_ID, parkingSpotId)
                    putString(ARG_PARKING_LOT_ID, parkingLotId)
                    putString(ARG_PARKING_LOT_NAME, parkingLotName)
                }
            }
        }
    }
}

