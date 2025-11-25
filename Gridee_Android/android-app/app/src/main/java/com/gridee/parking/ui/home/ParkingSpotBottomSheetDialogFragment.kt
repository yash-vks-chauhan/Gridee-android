package com.gridee.parking.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.os.Bundle
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.PaymentInitiateRequest
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.data.model.Vehicle
import com.gridee.parking.databinding.BottomSheetParkingSpotBinding
import com.gridee.parking.ui.booking.BookingConfirmationActivity
import com.gridee.parking.ui.booking.BookingViewModel
import com.gridee.parking.ui.profile.ProfileViewModel
import com.gridee.parking.ui.booking.DEFAULT_BOOKING_RATE
import com.gridee.parking.ui.booking.ParkingSpotSelectionAdapter
import com.gridee.parking.ui.booking.VehicleSelectionAdapter
import com.gridee.parking.ui.wallet.WalletTopUpActivity
import com.gridee.parking.ui.wallet.WalletViewModel
import com.ncorti.slidetoact.SlideToActView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.gridee.parking.databinding.BottomSheetTopUpSimpleBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import android.text.TextWatcher
import android.text.Editable
import android.widget.FrameLayout
import kotlinx.coroutines.launch

private const val ENTRY_DURATION = 320L
private const val EXIT_DURATION = 220L
private const val ENTRY_START_DELAY = 40L
private const val ENTRY_STAGGER_DELAY = 70L
private const val EXIT_STAGGER_DELAY = 50L

class ParkingSpotBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val motionInterpolator = FastOutSlowInInterpolator()

    private var _binding: BottomSheetParkingSpotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private val walletViewModel: WalletViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private val profileViewModel: ProfileViewModel by activityViewModels()

    private var parkingSpot: ParkingSpot? = null
    private var selectedLotId: String = ""
    private var selectedLotName: String = ""
    private var selectedSpotId: String = ""
    private var walletBalance: Double = 0.0
    private var isBookingInProgress = false

    private var entryAnimator: AnimatorSet? = null
    private var exitAnimator: AnimatorSet? = null
    private var isExitAnimationRunning = false

    private val sheetDateFormatter = SimpleDateFormat("MMM dd, yyyy\nhh:mm a", Locale.getDefault())
    private val sheetMonthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
    private val sheetDayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    private val sheetWeekdayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    private val sheetTimeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val sheetSummaryFormatter = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
    private val sheetDateOnlyFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val previewDateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val previewTimeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
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
        try {
            selectedSpotId = arguments?.getString(ARG_SPOT_ID).orEmpty()
            selectedLotName = arguments?.getString(ARG_LOT_NAME).orEmpty()

            setupSheetHeader()
            initializeDefaultTimes()
            setupClickListeners()
            setupObservers()
            setupWalletObservers()
            loadParkingSpot(selectedSpotId)

            // viewModel.loadUserVehicles() - Removed to rely on ProfileViewModel sync
            walletViewModel.loadWalletData()

            prepareEntryAnimationState()
            binding.root.doOnPreDraw { playEntryAnimations() }
        } catch (e: Exception) {
            e.printStackTrace()
            // Log the error and try to gracefully handle it
            android.util.Log.e(TAG, "Error in onViewCreated: ${e.message}", e)
            // Optionally dismiss the bottom sheet if initialization fails critically
            // dismissAllowingStateLoss()
        }
    }

    override fun onResume() {
        super.onResume()
        // viewModel.loadUserVehicles() - Removed to rely on ProfileViewModel sync
        walletViewModel.loadWalletData()
    }

    override fun onDestroyView() {
        entryAnimator?.cancel()
        exitAnimator?.cancel()
        isExitAnimationRunning = false
        _binding = null
        super.onDestroyView()
    }

    private fun setupSheetHeader() {
        val spotName = arguments?.getString(ARG_SPOT_NAME).orEmpty()
        binding.tvParkingSheetTitle.text = spotName.takeIf { it.isNotBlank() }
            ?: getString(R.string.home_parking_sheet_title_default)
        binding.tvParkingSheetSubtitle.text = buildSubtitleText()
    }

    private fun buildSubtitleText(): String {
        val lot = arguments?.getString(ARG_LOT_NAME)?.takeIf { it.isNotBlank() }
        val location = arguments?.getString(ARG_LOCATION_LABEL)?.takeIf { it.isNotBlank() }
        return when {
            lot != null && location != null -> "$lot • $location"
            lot != null -> lot
            location != null -> location
            else -> getString(R.string.home_parking_sheet_subtitle_default)
        }
    }

    private fun initializeDefaultTimes() {
        val selectedStartMillis = arguments?.getLong(ARG_SELECTED_START_TIME, -1L) ?: -1L
        val selectedEndMillis = arguments?.getLong(ARG_SELECTED_END_TIME, -1L) ?: -1L
        if (selectedStartMillis > 0) {
            viewModel.setStartTime(Date(selectedStartMillis))
            val endValue = if (selectedEndMillis > 0) {
                Date(selectedEndMillis)
            } else {
                Date(selectedStartMillis + TimeUnit.HOURS.toMillis(2))
            }
            viewModel.setEndTime(endValue)
            return
        } else if (selectedEndMillis > 0) {
            val fallbackStart = Date(selectedEndMillis - TimeUnit.HOURS.toMillis(2))
            viewModel.setStartTime(fallbackStart)
            viewModel.setEndTime(Date(selectedEndMillis))
            return
        }

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
        binding.btnCloseSheet.setOnClickListener {
            if (!isExitAnimationRunning) {
                closeWithExitAnimation()
            }
        }
        binding.cardCheckIn.setOnClickListener { showDateTimePicker(true) }
        binding.cardCheckOut.setOnClickListener { showDateTimePicker(false) }
        binding.btnSelectSpot.setOnClickListener { showSpotSelectionDialog() }
        binding.cardVehicleSelection.setOnClickListener { showVehicleSelectionDialog() }
        binding.btnAddWalletMoney.setOnClickListener { showTopUpDialog() }
        binding.slideConfirmBooking.onSlideCompleteListener =
            object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    view.sliderIcon = R.drawable.ic_check
                    handleConfirmSlide()
                }
            }

        binding.bookingSummaryCard.setOnClickListener {
            toggleBookingSummary()
        }

        setupSliderInteractions()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSliderInteractions() {
        try {
            val slider = binding.slideConfirmBooking
            
            // 1. Continuous Haptic Feedback
            var lastHapticX = 0f
            val hapticThreshold = 20f // Pixels to drag before triggering haptic

            slider.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastHapticX = event.x
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (Math.abs(event.x - lastHapticX) > hapticThreshold) {
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            lastHapticX = event.x
                        }
                    }
                }
                false // Let the view handle the actual slide logic
            }

            // 2. Shimmering Text Effect
            slider.post {
                try {
                    findTextView(slider)?.let { textView ->
                        startShimmerAnimation(textView)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findTextView(view: View): TextView? {
        if (view is TextView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val found = findTextView(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun startShimmerAnimation(textView: TextView) {
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            intArrayOf(
                Color.parseColor("#8E8E93"),
                Color.parseColor("#FFFFFF"),
                Color.parseColor("#8E8E93")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader

        val animator = ValueAnimator.ofFloat(0f, width * 2f)
        animator.duration = 2500
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { animation ->
            val translate = animation.animatedValue as Float
            val matrix = Matrix()
            matrix.setTranslate(translate - width, 0f)
            textShader.setLocalMatrix(matrix)
            textView.invalidate()
        }
        animator.start()
    }

    private fun toggleBookingSummary() {
        val details = binding.layoutSummaryDetails
        val isExpanded = details.visibility == View.VISIBLE

        // 1. Haptic Feedback
        binding.bookingSummaryCard.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) HapticFeedbackConstants.CONTEXT_CLICK
            else HapticFeedbackConstants.KEYBOARD_TAP
        )

        if (!isExpanded) {
            // ================= OPENING FLOW =================
            
            // 1. Setup Transition for the Container (Height Change)
            val transition = androidx.transition.TransitionSet().apply {
                ordering = androidx.transition.TransitionSet.ORDERING_TOGETHER
                duration = 400L
                interpolator = FastOutSlowInInterpolator()
                addTransition(androidx.transition.ChangeBounds())
                addTransition(androidx.transition.Fade(androidx.transition.Fade.IN))
            }
            androidx.transition.TransitionManager.beginDelayedTransition(binding.bookingSummaryCard, transition)

            // 2. Toggle Visibility
            details.visibility = View.VISIBLE

            // 3. Rotate Chevron
            binding.ivSummaryChevron.animate()
                .rotation(180f)
                .setDuration(400L)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            // 4. Staggered Row Entry (Waterfall Down)
            val rows = listOf(binding.rowSummaryCheckIn, binding.rowSummaryCheckOut, binding.rowSummaryRate)
            rows.forEach { row ->
                row.alpha = 0f
                row.translationY = -20f // Start slightly above
            }

            rows.forEachIndexed { index, view ->
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(100L + (index * 70L)) // Staggered delay
                    .setInterpolator(FastOutSlowInInterpolator())
                    .withStartAction {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            animateBlurFocus(view)
                        }
                    }
                    .start()
            }

            // 5. Divider Animation (Expand from Center)
            binding.dividerSummary.apply {
                alpha = 0f
                scaleX = 0f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }

            // 6. Trigger Shimmer
            details.postDelayed({
                startOneShotShimmer(binding.tvDetailCheckIn)
                startOneShotShimmer(binding.tvDetailCheckOut)
                startOneShotShimmer(binding.tvDetailRate)
            }, 600)

        } else {
            // ================= CLOSING FLOW =================
            
            // 1. Rotate Chevron Back Immediately
            binding.ivSummaryChevron.animate()
                .rotation(0f)
                .setDuration(300L) // Slightly faster return
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            // 2. Divider Exit (Collapse to Center)
            binding.dividerSummary.animate()
                .alpha(0f)
                .scaleX(0f)
                .setDuration(200)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            // 3. Staggered Row Exit (Zip Up: Bottom -> Top)
            // We animate the rows out BEFORE collapsing the card for a cleaner effect
            val rows = listOf(binding.rowSummaryRate, binding.rowSummaryCheckOut, binding.rowSummaryCheckIn)
            
            rows.forEachIndexed { index, view ->
                view.animate()
                    .alpha(0f)
                    .translationY(-15f) // Slide up slightly
                    .setDuration(150)
                    .setStartDelay(index * 40L) // Fast stagger
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            }

            // 4. Collapse Container (Delayed slightly to let rows start disappearing)
            details.postDelayed({
                val transition = androidx.transition.TransitionSet().apply {
                    ordering = androidx.transition.TransitionSet.ORDERING_TOGETHER
                    duration = 350L
                    interpolator = FastOutSlowInInterpolator()
                    addTransition(androidx.transition.ChangeBounds())
                    addTransition(androidx.transition.Fade(androidx.transition.Fade.OUT))
                }
                androidx.transition.TransitionManager.beginDelayedTransition(binding.bookingSummaryCard, transition)
                
                details.visibility = View.GONE

                // Reset rows for next open
                details.postDelayed({
                    rows.forEach { 
                        it.alpha = 1f 
                        it.translationY = 0f
                    }
                    // Reset divider
                    binding.dividerSummary.alpha = 1f
                    binding.dividerSummary.scaleX = 1f
                }, 350)
            }, 120) // Wait for the "Zip Up" to be partially done
        }
    }

    private fun animateBlurFocus(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val maxBlur = 10f
            val animator = ValueAnimator.ofFloat(maxBlur, 0f)
            animator.duration = 500
            animator.interpolator = FastOutSlowInInterpolator()
            animator.addUpdateListener { animation ->
                val blurRadius = animation.animatedValue as Float
                if (blurRadius > 0) {
                    view.setRenderEffect(
                        android.graphics.RenderEffect.createBlurEffect(
                            blurRadius, blurRadius, Shader.TileMode.CLAMP
                        )
                    )
                } else {
                    view.setRenderEffect(null)
                }
            }
            animator.start()
        }
    }

    private fun startOneShotShimmer(textView: TextView) {
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        if (width == 0f) return

        val currentTextColor = textView.currentTextColor
        // Create a shimmer that respects the text color but adds a white/bright gleam
        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            intArrayOf(
                currentTextColor,
                Color.parseColor("#FFFFFF"), // Bright gleam
                currentTextColor
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader

        val animator = ValueAnimator.ofFloat(0f, width * 2f)
        animator.duration = 1200 // Slow and elegant
        animator.repeatCount = 0 // Run once
        animator.addUpdateListener { animation ->
            val translate = animation.animatedValue as Float
            val matrix = Matrix()
            matrix.setTranslate(translate - width, 0f)
            textShader.setLocalMatrix(matrix)
            textView.invalidate()
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                textView.paint.shader = null
                textView.invalidate()
            }
        })
        animator.start()
    }

    private fun setupObservers() {
        viewModel.startTime.observe(viewLifecycleOwner) { time ->
            time?.let {
                updateStartTimeDisplay(it)
                calculatePricing()
            }
        }

        viewModel.endTime.observe(viewLifecycleOwner) { time ->
            time?.let {
                updateEndTimeDisplay(it)
                calculatePricing()
            }
        }

        viewModel.selectedSpot.observe(viewLifecycleOwner) { spot ->
            updateSelectedSpotDisplay(spot)
        }

        viewModel.totalPrice.observe(viewLifecycleOwner) { price ->
            price?.let {
                _binding?.let { binding ->
                    binding.tvTotalPrice.text = "₹${String.format(Locale.getDefault(), "%.2f", it)}"
                    updateWalletAffordability()
                }
            }
        }

        viewModel.duration.observe(viewLifecycleOwner) { duration ->
            _binding?.tvDuration?.text = duration
        }

        viewModel.selectedVehicle.observe(viewLifecycleOwner) { vehicle ->
            _binding?.let { binding ->
                val vehicleText = vehicle?.number ?: "Select your vehicle"
                binding.tvSelectedVehicle.text = vehicleText

                updateWalletAffordability()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            isBookingInProgress = isLoading == true
            if (!isBookingInProgress) {
                resetConfirmSlider()
            }
            updateWalletAffordability()
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
                walletViewModel.loadWalletData()
                viewModel.clearBookingCreated()
                closeWithExitAnimation()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
                showWalletError(it)
                resetConfirmSlider()
            }
        }

        // Sync vehicles from ProfileViewModel
        profileViewModel.userProfile.observe(viewLifecycleOwner) { user ->
            user?.let {
                val vehicles = it.vehicleNumbers.mapIndexed { index, number ->
                    Vehicle(
                        id = "user_vehicle_$index",
                        number = number,
                        type = "Car",
                        brand = "User",
                        model = "Vehicle",
                        isDefault = index == 0
                    )
                }
                viewModel.updateUserVehicles(vehicles)
            }
        }
    }

    private fun setupWalletObservers() {
        walletViewModel.walletDetails.observe(viewLifecycleOwner) { details ->
            val balance = details?.balance ?: 0.0
            updateWalletBalance(balance)
        }

        walletViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading == true) {
                binding.walletBalanceProgress.visibility = View.VISIBLE
                binding.tvWalletUpdated.text = getString(R.string.wallet_fetching_balance)
            } else {
                binding.walletBalanceProgress.visibility = View.GONE
                binding.tvWalletUpdated.text = getString(R.string.wallet_updated_now)
            }
        }

        walletViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showWalletError(it)
                walletViewModel.clearError()
                showToast(it)
            }
        }

        updateWalletAffordability()
    }

    private fun loadParkingSpot(spotId: String) {
        if (spotId == "quick_book") {
            parkingSpot = createDefaultParkingSpot()
            updateParkingSpotDisplay()
        } else if (spotId.isNotEmpty()) {
            viewModel.loadParkingSpotById(spotId) { spot ->
                parkingSpot = spot ?: ParkingSpot(
                    id = spotId,
                    lotId = selectedLotId,
                    name = "Selected Spot",
                    zoneName = "Unknown Spot",
                    capacity = 0,
                    available = 0,
                    status = "unknown"
                )
                updateParkingSpotDisplay()
            }
        } else {
            parkingSpot = ParkingSpot(
                id = if (spotId.isNotEmpty()) spotId else "unknown",
                lotId = selectedLotId,
                name = null,
                zoneName = null,
                capacity = 0,
                available = 0,
                status = "unknown"
            )
            updateParkingSpotDisplay()
        }
    }

    private fun updateParkingSpotDisplay() {
        parkingSpot?.let { spot ->
            selectedLotId = spot.lotId
            val headerName = if (selectedLotName.isNotEmpty()) selectedLotName else spot.name ?: "Unknown Location"
            // Show name in the header title and keep hidden holder updated
            binding.tvParkingSheetTitle.text = headerName
            binding.tvParkingName.text = headerName

            val addressText = when {
                !spot.zoneName.isNullOrBlank() && spot.zoneName != spot.name -> spot.zoneName
                selectedLotId.isNotEmpty() -> "Lot ID: $selectedLotId"
                else -> "Address unavailable"
            }
            // Keep the address value for downstream use, but not visible in UI
            binding.tvParkingAddress.text = addressText

            // Hourly rate just under the title
            val hourlyRate = spot.bookingRate?.takeIf { it > 0 } ?: DEFAULT_BOOKING_RATE
            binding.tvHourlyRate.text = "₹${String.format(Locale.getDefault(), "%.2f", hourlyRate)}/hour"
            
            // Update detail rate in summary accordion
            binding.tvDetailRate.text = "₹${String.format(Locale.getDefault(), "%.2f", hourlyRate)} / hr"

            val spotName = spot.name ?: spot.zoneName ?: "Any available spot"
            binding.tvSelectedSpot.text = spotName
            viewModel.setSelectedSpot(spotName)

            viewModel.setParkingSpot(spot)
            // viewModel.loadUserVehicles() - Removed to rely on ProfileViewModel sync
        }
    }

    private fun updateSelectedSpotDisplay(spot: String?) {
        _binding?.let { binding ->
            val displaySpot = spot ?: "Any available spot"
            binding.tvSelectedSpot.text = displaySpot
        }
    }

    private fun showDateTimePicker(isStartTime: Boolean) {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        val contentView = layoutInflater.inflate(R.layout.dialog_datetime_picker, null)
        dialog.setContentView(contentView)

        val titleView = contentView.findViewById<TextView>(R.id.tvDateTimeTitle)
        val previewDateView = contentView.findViewById<TextView>(R.id.tvPreviewDate)
        val previewTimeView = contentView.findViewById<TextView>(R.id.tvPreviewTime)
        val datePicker = contentView.findViewById<DatePicker>(R.id.customDatePicker)
        val timePicker = contentView.findViewById<TimePicker>(R.id.customTimePicker)
        val btnApply = contentView.findViewById<View>(R.id.btnApplyPicker)
        val btnDismiss = contentView.findViewById<View>(R.id.btnDismissPicker)

        titleView?.text = if (isStartTime) {
            getString(R.string.datetime_picker_start_title)
        } else {
            getString(R.string.datetime_picker_end_title)
        }

        val currentSelection = if (isStartTime) viewModel.startTime.value else viewModel.endTime.value
        val workingCalendar = Calendar.getInstance().apply {
            currentSelection?.let { time = it }
        }

        datePicker?.init(
            workingCalendar.get(Calendar.YEAR),
            workingCalendar.get(Calendar.MONTH),
            workingCalendar.get(Calendar.DAY_OF_MONTH)
        ) { view, year, monthOfYear, dayOfMonth ->
            workingCalendar.set(Calendar.YEAR, year)
            workingCalendar.set(Calendar.MONTH, monthOfYear)
            workingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updatePickerPreview(workingCalendar, previewDateView, previewTimeView)
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        timePicker?.apply {
            setIs24HourView(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = workingCalendar.get(Calendar.HOUR_OF_DAY)
                minute = workingCalendar.get(Calendar.MINUTE)
            } else {
                @Suppress("DEPRECATION")
                val currentHour = workingCalendar.get(Calendar.HOUR_OF_DAY)
                @Suppress("DEPRECATION")
                val currentMinute = workingCalendar.get(Calendar.MINUTE)
                @Suppress("DEPRECATION")
                this.currentHour = currentHour
                @Suppress("DEPRECATION")
                this.currentMinute = currentMinute
            }
            setOnTimeChangedListener { view, hourOfDay, minute ->
                workingCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                workingCalendar.set(Calendar.MINUTE, minute)
                updatePickerPreview(workingCalendar, previewDateView, previewTimeView)
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        updatePickerPreview(workingCalendar, previewDateView, previewTimeView)

        btnDismiss?.setOnClickListener { dialog.dismiss() }
        btnApply?.setOnClickListener {
            if (isStartTime) {
                viewModel.setStartTime(workingCalendar.time)
            } else {
                viewModel.setEndTime(workingCalendar.time)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSpotSelectionDialog() {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_spot_selection, null)
        dialog.setContentView(dialogView)

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_parking_spots)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progress_bar)
        val emptyState = dialogView.findViewById<android.widget.TextView>(R.id.tv_empty_state)
        val cardAnySpot = dialogView.findViewById<androidx.cardview.widget.CardView>(R.id.card_any_spot)
        val ivAnySpotSelected = dialogView.findViewById<android.widget.ImageView>(R.id.iv_any_spot_selected)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel)
        val btnSelect = dialogView.findViewById<View>(R.id.btn_select)

        var selectedSpot: ParkingSpot? = null
        val currentSelectedSpotText = binding.tvSelectedSpot.text.toString()
        var isAnySpotSelected = currentSelectedSpotText == "Any available spot"

        val spotAdapter = ParkingSpotSelectionAdapter { spot ->
            selectedSpot = spot
            isAnySpotSelected = false
            updateSpotSelection(ivAnySpotSelected, false)
            showToast("Selected spot: ${spot.name ?: spot.zoneName}")
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        recyclerView.adapter = spotAdapter

        showProgress(progressBar, recyclerView, emptyState, true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val parkingRepository = com.gridee.parking.data.repository.ParkingRepository()
                val start = viewModel.startTime.value ?: Calendar.getInstance().time
                val end = viewModel.endTime.value ?: Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2) }.time
                val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                val startStr = df.format(start)
                val endStr = df.format(end)
                val spotsResponse = if (selectedLotId.isNotEmpty()) {
                    parkingRepository.getAvailableSpots(selectedLotId, startStr, endStr)
                } else {
                    retrofit2.Response.success(emptyList<ParkingSpot>())
                }

                if (spotsResponse.isSuccessful) {
                    val filteredSpots = spotsResponse.body() ?: emptyList()
                    showProgress(progressBar, recyclerView, emptyState, false)

                    if (filteredSpots.isNotEmpty()) {
                        spotAdapter.submitList(filteredSpots)
                        recyclerView.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE

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
            if (isAnySpotSelected) {
                showToast("Applying: Any available spot")
                binding.tvSelectedSpot.text = "Any available spot"
                viewModel.setSelectedSpot(null)
            } else {
                selectedSpot?.let { spot ->
                    val spotName = spot.name ?: spot.zoneName ?: "Selected Spot"
                    showToast("Applying: $spotName")
                    binding.tvSelectedSpot.text = spotName
                    binding.tvSelectedSpot.requestLayout()
                    binding.tvSelectedSpot.invalidate()
                    viewModel.setSelectedSpot(spotName)
                } ?: run {
                    showToast("No specific spot selected, using Any available spot")
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
        if (isSelected) {
            imageView.setImageResource(R.drawable.ic_radio_button_checked)
        } else {
            imageView.setImageResource(R.drawable.ic_radio_button_unchecked)
        }
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
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_vehicle_selection, null)
        dialog.setContentView(dialogView)

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_vehicles)
        val btnAddVehicle = dialogView.findViewById<View>(R.id.btn_add_vehicle)
        val btnDismiss = dialogView.findViewById<View>(R.id.btnDismissVehicle)
        val btnSelect = dialogView.findViewById<View>(R.id.btn_select)

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
                    requireActivity().runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        if (success) {
                            showToast("Vehicle added successfully!")
                            viewModel.loadUserVehicles()
                            _binding?.cardVehicleSelection?.postDelayed({
                                showVehicleSelectionDialog()
                            }, 500)
                        } else {
                            showToast("Failed to add vehicle. Please check your connection.")
                        }
                    }
                }
            }
        }

        btnDismiss.setOnClickListener { dialog.dismiss() }

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
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_add_vehicle, null)
        dialog.setContentView(dialogView)

        val etVehicleNumber = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etVehicleNumber)
        val btnAdd = dialogView.findViewById<View>(R.id.btnAddVehicle)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)

        // Focus input and show keyboard
        etVehicleNumber.requestFocus()
        etVehicleNumber.postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(etVehicleNumber, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        btnAdd.setOnClickListener {
            val vehicleNumber = etVehicleNumber.text.toString().trim().uppercase()
            if (vehicleNumber.isNotEmpty()) {
                onVehicleAdded(vehicleNumber)
                dialog.dismiss()
            } else {
                showToast("Please enter a vehicle number")
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePickerPreview(calendar: Calendar, dateView: TextView?, timeView: TextView?) {
        val dateText = previewDateFormatter.format(calendar.time)
        val timeText = previewTimeFormatter.format(calendar.time)
        dateView?.text = dateText
        timeView?.text = timeText
    }

    private fun updateStartTimeDisplay(time: Date) {
        _binding?.let { binding ->
            binding.tvCheckInDate.text = sheetDateOnlyFormatter.format(time)
            binding.tvCheckInTime.text = sheetTimeFormatter.format(time)
            binding.tvDetailCheckIn.text = sheetSummaryFormatter.format(time)
        }
    }

    private fun updateEndTimeDisplay(time: Date) {
        _binding?.let { binding ->
            binding.tvCheckOutDate.text = sheetDateOnlyFormatter.format(time)
            binding.tvCheckOutTime.text = sheetTimeFormatter.format(time)
            binding.tvDetailCheckOut.text = sheetSummaryFormatter.format(time)
        }
    }

    private fun calculatePricing() {
        viewModel.calculatePricing()
    }

    private fun updateWalletBalance(balance: Double) {
        _binding?.let { binding ->
            walletBalance = balance
            binding.tvWalletBalance.text = "₹${formatCurrency(balance)}"
            binding.tvWalletUpdated.text = getString(R.string.wallet_updated_now)

            val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
            sharedPref.edit().putFloat("wallet_balance", balance.toFloat()).apply()

            showWalletError(null)
            updateWalletAffordability()
        }
    }

    private fun updateWalletAffordability() {
        _binding?.let { binding ->
            val total = viewModel.totalPrice.value ?: 0.0
            val selectedVehicle = viewModel.selectedVehicle.value
            val deficit = if (total > walletBalance) total - walletBalance else 0.0

            when {
                total <= 0 -> {
                    binding.tvWalletStatus.text = getString(R.string.wallet_ready_hint)
                    showWalletError(null)
                }
                deficit > 0 -> {
                    val formatted = formatCurrency(deficit)
                    binding.tvWalletStatus.text = getString(R.string.wallet_add_more_hint, formatted)
                    showWalletError(getString(R.string.wallet_insufficient_error, formatted))
                }
                else -> {
                    val formatted = formatCurrency(total)
                    binding.tvWalletStatus.text = getString(R.string.wallet_sufficient_hint, formatted)
                    showWalletError(null)
                }
            }

            val canSlide = canConfirmBooking(selectedVehicle, total, deficit)
            binding.slideConfirmBooking.isEnabled = canSlide
            if (!canSlide) {
                resetConfirmSlider()
            }
        }
    }

    private fun canConfirmBooking(
        selectedVehicle: com.gridee.parking.data.model.Vehicle? = viewModel.selectedVehicle.value,
        total: Double = viewModel.totalPrice.value ?: 0.0,
        deficit: Double = if (total > walletBalance) total - walletBalance else 0.0
    ): Boolean {
        if (isBookingInProgress) return false
        if (selectedVehicle == null || total <= 0) return false
        return deficit <= 0.01
    }

    private fun handleConfirmSlide() {
        if (!canConfirmBooking()) {
            updateWalletAffordability()
            resetConfirmSlider()
            return
        }
        createBooking()
    }

    private fun resetConfirmSlider() {
        _binding?.slideConfirmBooking?.post {
            try {
                _binding?.slideConfirmBooking?.setCompleted(false, true)
            } catch (_: Exception) {
            }
        }
    }

    private fun showWalletError(message: String?) {
        if (message.isNullOrBlank()) {
            binding.tvWalletError.visibility = View.GONE
        } else {
            binding.tvWalletError.text = message
            binding.tvWalletError.visibility = View.VISIBLE
        }
    }

    private fun formatCurrency(amount: Double): String {
        return String.format(Locale.getDefault(), "%,.2f", amount)
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

    private fun showTopUpDialog() {
        try {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
            val bottomSheetBinding = BottomSheetTopUpSimpleBinding.inflate(layoutInflater)
            bottomSheetDialog.setContentView(bottomSheetBinding.root)

            bottomSheetDialog.window?.apply {
                setWindowAnimations(R.style.BottomSheetSpringAnimation)
                setDimAmount(0.45f)
            }

            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
                isFitToContents = true
            }

            bottomSheetDialog.setOnShowListener {
                val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(android.R.color.transparent)
                bottomSheet?.post {
                    bottomSheet.translationY = 120f
                    bottomSheet.alpha = 0f
                    val spring = SpringAnimation(bottomSheet, DynamicAnimation.TRANSLATION_Y, 0f).apply {
                        spring = SpringForce(0f).apply {
                            dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                            stiffness = SpringForce.STIFFNESS_LOW
                        }
                    }
                    bottomSheet.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .start()
                    spring.start()
                }
            }

            // Set current balance
            bottomSheetBinding.tvCurrentBalance.text = "₹${formatCurrency(walletBalance)}"

            // Setup click listeners for quick amount buttons
            bottomSheetBinding.btnAmount50.setOnClickListener {
                bottomSheetBinding.etAmount.setText("50")
                updateAddButtonState(bottomSheetBinding)
            }

            bottomSheetBinding.btnAmount100.setOnClickListener {
                bottomSheetBinding.etAmount.setText("100")
                updateAddButtonState(bottomSheetBinding)
            }

            bottomSheetBinding.btnAmount200.setOnClickListener {
                bottomSheetBinding.etAmount.setText("200")
                updateAddButtonState(bottomSheetBinding)
            }

            // Setup text change listener for amount input
            bottomSheetBinding.etAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateAddButtonState(bottomSheetBinding)
                }
            })

            // Close button
            bottomSheetBinding.btnClose.setOnClickListener {
                animateAndDismiss(bottomSheetDialog)
            }

            // Add money button
            bottomSheetBinding.btnAddMoneyConfirm.setOnClickListener {
                val amountText = bottomSheetBinding.etAmount.text.toString()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        showToast("Redirecting to Razorpay checkout...")
                        startRazorpayCheckout(amount)
                        animateAndDismiss(bottomSheetDialog)
                    } else {
                        showToast("Please enter a valid amount")
                    }
                }
            }

            bottomSheetDialog.show()

        } catch (e: Exception) {
            showToast("Error opening top-up dialog: ${e.message}")
        }
    }

    private fun updateAddButtonState(binding: BottomSheetTopUpSimpleBinding) {
        val amountText = binding.etAmount.text.toString()
        val amount = amountText.toDoubleOrNull()
        val isValidAmount = amount != null && amount > 0

        binding.btnAddMoneyConfirm.isEnabled = isValidAmount
        binding.btnAddMoneyConfirm.text = if (isValidAmount) {
            "Add ₹${amount?.toInt()}"
        } else {
            "Add Money"
        }
    }

    private fun animateAndDismiss(dialog: BottomSheetDialog) {
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet == null) {
            dialog.dismiss()
            return
        }
        bottomSheet.animate()
            .translationY(bottomSheet.height * 0.25f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                dialog.dismiss()
                bottomSheet.translationY = 0f
                bottomSheet.alpha = 1f
            }
            .start()
    }

    private fun startRazorpayCheckout(amount: Double) {
        val userId = getUserId()
        if (userId.isNullOrBlank()) {
            showToast("Please log in to add money")
            return
        }

        binding.walletBalanceProgress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val initResp = ApiClient.apiService.initiatePayment(
                    PaymentInitiateRequest(
                        userId = userId,
                        amount = amount
                    )
                )

                if (!initResp.isSuccessful) {
                    showToast("Failed to initiate payment: ${initResp.code()}")
                    return@launch
                }

                val body = initResp.body()
                val orderId = body?.orderId
                if (orderId.isNullOrBlank()) {
                    showToast("Invalid payment order from server")
                    return@launch
                }

                val intent = Intent(requireContext(), WalletTopUpActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                    putExtra("AMOUNT", amount)
                    putExtra("ORDER_ID", orderId)
                    body?.keyId?.let { putExtra("KEY_ID", it) }
                }
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.walletBalanceProgress.visibility = View.GONE
            }
        }
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
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

    private fun prepareEntryAnimationState() {
        // If morphing, we handle state in playEntryAnimations
        val startW = arguments?.getInt("ARG_START_W", 0) ?: 0
        if (startW > 0) return

        val binding = _binding ?: return
        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
        listOf(
            binding.tvParkingSheetTitle,
            binding.tvHourlyRate,
            binding.bookingScroll
        ).forEach { view ->
            view.translationY = offset
            view.alpha = 0f
        }
    }

    private fun playEntryAnimations() {
        val binding = _binding ?: return
        
        try {
            val startX = arguments?.getInt("ARG_START_X", 0) ?: 0
            val startY = arguments?.getInt("ARG_START_Y", 0) ?: 0
            val startW = arguments?.getInt("ARG_START_W", 0) ?: 0
            val startH = arguments?.getInt("ARG_START_H", 0) ?: 0

            // "Magic" Interpolator: Starts fast, settles very smoothly (Natural Fluidity)
            val magicInterpolator = android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

            if (startW > 0 && startH > 0) {
                // Fluid Morph Animation
                val root = binding.root
                
                root.post {
                    try {
                        val location = IntArray(2)
                        root.getLocationOnScreen(location)
                        val targetX = location[0]
                        val targetY = location[1]
                        val targetW = root.width
                        val targetH = root.height

                        if (targetW == 0 || targetH == 0) return@post

                        val scaleX = startW.toFloat() / targetW.toFloat()
                        val scaleY = startH.toFloat() / targetH.toFloat()

                        root.pivotX = 0f
                        root.pivotY = 0f
                        
                        val transX = startX - targetX
                        val transY = startY - targetY
                        
                        // Initial State
                        root.translationX = transX.toFloat()
                        root.translationY = transY.toFloat()
                        root.scaleX = scaleX
                        root.scaleY = scaleY
                        root.alpha = 0f 

                        val morphDuration = 550L // Slightly longer for "Magic" feel
                        
                        // 1. Container Morph
                        root.animate()
                            .translationX(0f)
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(morphDuration)
                            .setInterpolator(magicInterpolator)
                            .start()

                        // 2. Container Alpha (Seamless blend)
                        val alphaAnim = ObjectAnimator.ofFloat(root, View.ALPHA, 0f, 1f)
                        alphaAnim.duration = 150
                        alphaAnim.start()

                        // 3. Content Entry (Flowing Pop with Waterfall Stagger)
                        val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
                        
                        // Collect all views to animate in order
                        val viewsToAnimate = mutableListOf<View>()
                        viewsToAnimate.add(binding.tvParkingSheetTitle)
                        viewsToAnimate.add(binding.tvHourlyRate)
                        
                        // Add children of the scroll content layout (Waterfall effect)
                        // Accessing the LinearLayout inside NestedScrollView
                        val contentLayout = binding.bookingScroll.getChildAt(0) as? ViewGroup
                        if (contentLayout != null) {
                            for (i in 0 until contentLayout.childCount) {
                                val child = contentLayout.getChildAt(i)
                                // Only animate visible children
                                if (child.visibility == View.VISIBLE) {
                                    viewsToAnimate.add(child)
                                }
                            }
                        } else {
                            // Fallback if layout structure changes
                            viewsToAnimate.add(binding.bookingScroll)
                        }
                        
                        viewsToAnimate.forEachIndexed { index, view ->
                            view.alpha = 0f
                            view.translationY = offset * 0.8f
                            view.scaleX = 0.9f // Start slightly smaller
                            view.scaleY = 0.9f
                            
                            view.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(500) // Slightly longer for smoother flow
                                .setStartDelay(100L + (index * 50L)) // Progressive stagger
                                .setInterpolator(magicInterpolator)
                                .start()
                        }
                        
                        // 4. Subtle Haptic "Lock" at the end
                        root.postDelayed({
                            if (context != null) root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }, morphDuration)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return
            }

            // Fallback: Standard Slide Up
            val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
            val titleAnimator = binding.tvParkingSheetTitle.let {
                createSlideAnimator(it, offset, 0f, true).apply { duration = ENTRY_DURATION }
            }
            val subtitleAnimator = binding.tvHourlyRate.let {
                createSlideAnimator(it, offset, 0f, true).apply { duration = ENTRY_DURATION }
            }
            val contentAnimator = binding.bookingScroll.let {
                createSlideAnimator(it, offset, 0f, true).apply { duration = ENTRY_DURATION }
            }
            val animators = listOf(titleAnimator, subtitleAnimator, contentAnimator)

            titleAnimator.startDelay = ENTRY_START_DELAY
            subtitleAnimator.startDelay = ENTRY_START_DELAY + ENTRY_STAGGER_DELAY
            contentAnimator.startDelay = ENTRY_START_DELAY + ENTRY_STAGGER_DELAY * 2

            entryAnimator?.cancel()
            entryAnimator = AnimatorSet().apply {
                playTogether(animators)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeWithExitAnimation() {
        try {
            if (isExitAnimationRunning) {
                dismissAllowingStateLoss()
                return
            }

            val binding = _binding ?: run {
                dismissAllowingStateLoss()
                return
            }

            val startX = arguments?.getInt("ARG_START_X", 0) ?: 0
            val startY = arguments?.getInt("ARG_START_Y", 0) ?: 0
            val startW = arguments?.getInt("ARG_START_W", 0) ?: 0
            val startH = arguments?.getInt("ARG_START_H", 0) ?: 0

            if (startW > 0 && startH > 0) {
                // Reverse Fluid Morph Animation
                isExitAnimationRunning = true
                val root = binding.root
                
                val location = IntArray(2)
                root.getLocationOnScreen(location)
                val currentX = location[0]
                val currentY = location[1]
                val currentW = root.width
                val currentH = root.height

                if (currentW == 0 || currentH == 0) {
                    dismissAllowingStateLoss()
                    return
                }

                val scaleX = startW.toFloat() / currentW.toFloat()
                val scaleY = startH.toFloat() / currentH.toFloat()
                
                val transX = startX - currentX
                val transY = startY - currentY

                root.pivotX = 0f
                root.pivotY = 0f

                // 1. Reverse Waterfall Staggered Exit (Organic & Subtle)
                val viewsToAnimate = mutableListOf<View>()
                viewsToAnimate.add(binding.tvParkingSheetTitle)
                viewsToAnimate.add(binding.tvHourlyRate)
                
                val contentLayout = binding.bookingScroll.getChildAt(0) as? ViewGroup
                if (contentLayout != null) {
                    for (i in 0 until contentLayout.childCount) {
                        val child = contentLayout.getChildAt(i)
                        if (child.visibility == View.VISIBLE) {
                            viewsToAnimate.add(child)
                        }
                    }
                } else {
                    viewsToAnimate.add(binding.bookingScroll)
                }
                
                // Reverse order: Bottom -> Top
                viewsToAnimate.reverse()
                
                // "Organic" Exit Interpolator (Starts slow, accelerates)
                val exitInterpolator = android.view.animation.PathInterpolator(0.4f, 0.0f, 1f, 1f)

                viewsToAnimate.forEachIndexed { index, view ->
                    view.animate()
                        .alpha(0f)
                        .translationY(40f) // Subtle drop
                        .scaleX(0.96f) // Very subtle shrink
                        .scaleY(0.96f)
                        .setDuration(250) // Fast but smooth
                        .setStartDelay(index * 25L) // Rapid ripple
                        .setInterpolator(exitInterpolator)
                        .start()
                }

                // 2. Morph back to card with "Anticipation"
                // The sheet will slightly expand/breathe before shrinking, giving it life.
                val morphDelay = 150L 
                
                // Custom "Anticipate-Overshoot" curve for the morph
                // It pulls back slightly (anticipate) then snaps to the target
                val organicMorphInterpolator = android.view.animation.PathInterpolator(0.4f, -0.1f, 0.2f, 1f)
                
                root.animate()
                    .translationX(transX.toFloat())
                    .translationY(transY.toFloat())
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .setDuration(600) 
                    .setStartDelay(morphDelay) 
                    .setInterpolator(organicMorphInterpolator)
                    .withEndAction {
                        isExitAnimationRunning = false
                        dismissAllowingStateLoss()
                    }
                    .start()
                
                // 3. Fade out root smoothly at the end
                root.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .setStartDelay(morphDelay + 400L) // Fade out as it snaps into place
                    .start()
                
                return
            }

            // Fallback: Standard Slide Down Animation
            val offset = resources.getDimension(R.dimen.coin_sheet_slide_offset)
            val subtitleAnimator = createSlideAnimator(binding.tvHourlyRate, 0f, offset, false).apply { duration = EXIT_DURATION }
            val titleAnimator = createSlideAnimator(binding.tvParkingSheetTitle, 0f, offset, false).apply { duration = EXIT_DURATION }
            val contentAnimator = createSlideAnimator(binding.bookingScroll, 0f, offset, false).apply { duration = EXIT_DURATION }
            val animators = listOf(contentAnimator, subtitleAnimator, titleAnimator)

            contentAnimator.startDelay = 0L
            subtitleAnimator.startDelay = EXIT_STAGGER_DELAY
            titleAnimator.startDelay = EXIT_STAGGER_DELAY * 2

            entryAnimator?.cancel()
            isExitAnimationRunning = true
            exitAnimator?.cancel()
            exitAnimator = AnimatorSet().apply {
                playTogether(animators)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        isExitAnimationRunning = false
                        this@ParkingSpotBottomSheetDialogFragment.dismissAllowingStateLoss()
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        isExitAnimationRunning = false
                    }
                })
                start()
            }
        } catch (e: Exception) {
            dismissAllowingStateLoss()
        }
    }

    private fun createSlideAnimator(
        target: View,
        fromY: Float,
        toY: Float,
        fadeIn: Boolean
    ): AnimatorSet {
        target.translationY = fromY
        target.alpha = if (fadeIn) 0f else 1f

        val slide = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, fromY, toY)
        val alphaValues = if (fadeIn) floatArrayOf(0f, 1f) else floatArrayOf(1f, 0f)
        val fade = ObjectAnimator.ofFloat(target, View.ALPHA, *alphaValues)

        return AnimatorSet().apply {
            interpolator = motionInterpolator
            playTogether(slide, fade)
        }
    }

    companion object {
        private const val ARG_SPOT_ID = "ARG_SPOT_ID"
        private const val ARG_SPOT_NAME = "ARG_SPOT_NAME"
        private const val ARG_LOT_NAME = "ARG_LOT_NAME"
        private const val ARG_LOCATION_LABEL = "ARG_LOCATION_LABEL"
        private const val ARG_CAPACITY = "ARG_CAPACITY"
        private const val ARG_AVAILABLE = "ARG_AVAILABLE"
        private const val ARG_STATUS = "ARG_STATUS"
        private const val ARG_SELECTED_START_TIME = "ARG_SELECTED_START_TIME"
        private const val ARG_SELECTED_END_TIME = "ARG_SELECTED_END_TIME"

        const val TAG = "ParkingSpotSheet"

        fun newInstance(
            spot: HomeParkingSpot,
            startRect: android.graphics.Rect? = null,
            startTime: Date? = null,
            endTime: Date? = null
        ): ParkingSpotBottomSheetDialogFragment {
            return ParkingSpotBottomSheetDialogFragment().apply {
                arguments = bundleOf(
                    ARG_SPOT_ID to spot.id,
                    ARG_SPOT_NAME to spot.spotName,
                    ARG_LOT_NAME to spot.lotName,
                    ARG_LOCATION_LABEL to spot.locationLabel,
                    ARG_CAPACITY to spot.capacity,
                    ARG_AVAILABLE to spot.availableUnits,
                    ARG_STATUS to spot.status,
                    "ARG_START_X" to startRect?.left,
                    "ARG_START_Y" to startRect?.top,
                    "ARG_START_W" to startRect?.width(),
                    "ARG_START_H" to startRect?.height(),
                    ARG_SELECTED_START_TIME to startTime?.time,
                    ARG_SELECTED_END_TIME to endTime?.time
                )
            }
        }
    }
}
