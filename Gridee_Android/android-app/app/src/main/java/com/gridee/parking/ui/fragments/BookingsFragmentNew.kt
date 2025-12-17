package com.gridee.parking.ui.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Button
import android.app.Dialog
import android.view.HapticFeedbackConstants
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.Booking as BackendBooking
import com.gridee.parking.data.model.UIBooking
import com.gridee.parking.databinding.FragmentBookingsNewBinding
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.ui.adapters.BookingStatus
import com.gridee.parking.ui.adapters.BookingsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.databinding.BottomSheetBookingFiltersBinding
import com.gridee.parking.databinding.BottomSheetBookingOverviewBinding
import android.view.WindowManager
import android.graphics.Typeface
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import com.gridee.parking.ui.qr.QrScannerActivity
import com.gridee.parking.data.repository.BookingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.google.android.material.ripple.RippleUtils
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.math.abs
import kotlin.math.roundToInt

class BookingsFragmentNew : BaseTabFragment<FragmentBookingsNewBinding>() {

    private lateinit var bookingsAdapter: BookingsAdapter
    private var userBookings = mutableListOf<BackendBooking>()
    private var currentTab = BookingStatus.ACTIVE
    private var blurOverlayView: View? = null
    private var isSliderDragging = false
    private var sliderDragOffset = 0f
    private val selectedLabelColor by lazy { ContextCompat.getColor(requireContext(), R.color.text_primary) }
    private val unselectedLabelColor by lazy { ContextCompat.getColor(requireContext(), R.color.segment_button_text_unchecked) }
    private var currentSortOption = BookingSortOption.NEWEST_FIRST
    private var sortBottomSheetDialog: BottomSheetDialog? = null
    private var selectedSpotFilter: String? = null
    
    // Cache for parking lot and spot names
    private val parkingLotCache = mutableMapOf<String, String>() // lotId -> name
    private val parkingSpotCache = mutableMapOf<String, String>() // spotId -> name
    private var isCacheLoaded = false
    private data class NavigationRequest(val showPending: Boolean, val highlightBookingId: String?)
    private var pendingNavigationRequest: NavigationRequest? = null
    private var pendingHighlightBookingId: String? = null

    private val bookingRepository by lazy { BookingRepository(requireContext()) }

    private enum class ScanType { CHECK_IN, CHECK_OUT }
    private enum class BookingSortOption {
        NEWEST_FIRST,
        OLDEST_FIRST
    }
    
    // Date filter fields
    private var filterStartDate: Long? = null
    private var filterEndDate: Long? = null
    
    private var pendingScanBookingId: String? = null
    private var pendingScanType: ScanType? = null

    private val qrScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == QrScannerActivity.RESULT_QR_SCANNED) {
            val qrCode = result.data?.getStringExtra(QrScannerActivity.EXTRA_QR_CODE)
            val bookingId = pendingScanBookingId
            val type = pendingScanType
            if (qrCode.isNullOrBlank() || bookingId.isNullOrBlank() || type == null) {
                showToast(getString(R.string.qr_invalid))
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                when (type) {
                    ScanType.CHECK_IN -> handleCheckInFlow(bookingId, qrCode)
                    ScanType.CHECK_OUT -> handleCheckOutFlow(bookingId, qrCode)
                }
            }
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBookingsNewBinding {
        return FragmentBookingsNewBinding.inflate(inflater, container, false)
    }

    override fun getScrollableView(): View? {
        return try {
            binding.rvBookings
        } catch (e: IllegalStateException) {
            null
        }
    }

    override fun setupUI() {
        setupRecyclerView()
        setupPullToRefresh()
        setupFilterButton()
        setupSegmentedControl()
        loadUserBookings() // Use real API instead of sample data
    }

    private fun setupPullToRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary)
        binding.swipeRefresh.setOnRefreshListener {
            loadUserBookings()
        }
    }

    private fun setupFilterButton() {
        binding.buttonFilter.setOnClickListener {
            // Add haptic feedback
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            
            // Add subtle scale animation
            it.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            showSortBottomSheet()
        }
    }

    private fun showSortBottomSheet() {
        if (sortBottomSheetDialog?.isShowing == true) return
        val sheetBinding = BottomSheetBookingFiltersBinding.inflate(layoutInflater)

        sheetBinding.radioNewestFirst.isChecked = currentSortOption == BookingSortOption.NEWEST_FIRST
        sheetBinding.radioOldestFirst.isChecked = currentSortOption == BookingSortOption.OLDEST_FIRST

        // Setup date filter UI
        setupDateFilter(sheetBinding)
        
        populateSpotFilterChips(sheetBinding)

        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        fun handleSelection(option: BookingSortOption) {
            applySortOption(option)
        }

        sheetBinding.radioNewestFirst.setOnClickListener { 
            handleSelection(BookingSortOption.NEWEST_FIRST)
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        sheetBinding.radioOldestFirst.setOnClickListener { 
            handleSelection(BookingSortOption.OLDEST_FIRST)
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }
        
        // Reset All button
        sheetBinding.buttonResetAll.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            resetAllFilters()
            dialog.dismiss()
        }
        
        // Apply Filters button
        sheetBinding.buttonApplyFilters.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            dialog.dismiss()
        }

        dialog.setOnDismissListener { sortBottomSheetDialog = null }
        dialog.show()
        sortBottomSheetDialog = dialog
    }
    
    private fun resetAllFilters() {
        // Reset sort to default
        currentSortOption = BookingSortOption.NEWEST_FIRST
        
        // Reset date filters
        filterStartDate = null
        filterEndDate = null
        
        // Reset spot filter
        selectedSpotFilter = null
        
        // Refresh the view
        showBookingsForStatus(currentTab)
        
        showToast("Filters reset")
    }

    private fun applySortOption(option: BookingSortOption) {
        if (currentSortOption == option) return
        currentSortOption = option
        showBookingsForStatus(currentTab)
    }

    private fun populateSpotFilterChips(sheetBinding: BottomSheetBookingFiltersBinding) {
        val chipGroup = sheetBinding.chipGroupSpots
        chipGroup.setOnCheckedStateChangeListener(null)
        chipGroup.removeAllViews()

        val availableSpots = userBookings
            .map { getSpotLabel(it).trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })

        sheetBinding.textNoSpots.visibility = if (availableSpots.isEmpty()) View.VISIBLE else View.GONE

        val normalizedSelection = selectedSpotFilter?.lowercase(Locale.ROOT)
        val allChip = createSpotChip(getString(R.string.spot_filter_all), true).apply {
            id = View.generateViewId()
            tag = ALL_SPOTS_TAG
        }
        chipGroup.addView(allChip)

        availableSpots.forEach { spotName ->
            val chip = createSpotChip(spotName).apply {
                id = View.generateViewId()
                tag = spotName
            }
            chipGroup.addView(chip)
            if (normalizedSelection != null && spotName.lowercase(Locale.ROOT) == normalizedSelection) {
                chipGroup.check(chip.id)
            }
        }

        if (normalizedSelection == null || chipGroup.checkedChipId == View.NO_ID) {
            chipGroup.check(allChip.id)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: View.NO_ID
            val selectedChip = if (checkedId != View.NO_ID) group.findViewById<Chip>(checkedId) else null
            val tag = selectedChip?.tag as? String
            val newSelection = when (tag) {
                null, ALL_SPOTS_TAG -> null
                else -> tag
            }
            val currentNormalized = selectedSpotFilter?.lowercase(Locale.ROOT)
            val newNormalized = newSelection?.lowercase(Locale.ROOT)
            if (currentNormalized != newNormalized) {
                selectedSpotFilter = newSelection
                showBookingsForStatus(currentTab)
            }
        }
    }

    private fun createSpotChip(label: String, isAllChip: Boolean = false): Chip {
        val chip = Chip(requireContext())
        chip.text = label
        chip.isCheckable = true
        chip.isCheckedIconVisible = false
        chip.isClickable = true
        chip.setEnsureMinTouchTargetSize(false)
        chip.textSize = 14f
        chip.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        chip.chipStrokeWidth = dpToPx(1f)
        chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.segment_shell_border))
        chip.chipCornerRadius = dpToPx(20f)
        chip.rippleColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.filter_button_ripple))
        val checkedBackground = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val uncheckedBackground = ContextCompat.getColor(requireContext(), R.color.segment_shell_surface)
        val backgroundColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(checkedBackground, uncheckedBackground)
        )
        chip.chipBackgroundColor = backgroundColors
        val textColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(requireContext(), R.color.white), ContextCompat.getColor(requireContext(), R.color.text_primary))
        )
        chip.setTextColor(textColors)
        chip.chipStartPadding = dpToPx(12f)
        chip.chipEndPadding = dpToPx(12f)
        chip.tag = if (isAllChip) ALL_SPOTS_TAG else label
        return chip
    }

    private fun dpToPx(value: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
    }

    fun handleExternalNavigation(showPending: Boolean, highlightBookingId: String?) {
        pendingNavigationRequest = NavigationRequest(showPending, highlightBookingId)
        applyPendingNavigationIfReady()
    }

    private fun setupRecyclerView() {
        val statuses = listOf(BookingStatus.ACTIVE, BookingStatus.PENDING, BookingStatus.COMPLETED)
        currentTab = statuses.first()

        bookingsAdapter = BookingsAdapter(emptyList()) { booking ->
            // Handle booking click (e.g., show details)
            showBookingDetails(booking)
        }
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingsAdapter
        }
    }

    private fun getSegmentTitle(status: BookingStatus): String {
        return when (status) {
            BookingStatus.ACTIVE -> binding.segmentedControlContainer.textActive.text.toString()
            BookingStatus.PENDING -> binding.segmentedControlContainer.textPending.text.toString()
            BookingStatus.COMPLETED -> binding.segmentedControlContainer.textCompleted.text.toString()
            else -> "Unknown" // Handle other cases
        }
    }

    private fun filterBookingsByStatus(status: BookingStatus) {
        android.util.Log.d("BookingsFragment", "filterBookingsByStatus: status=$status, total bookings=${userBookings.size}")
        val filteredBookings: List<BackendBooking> = userBookings.filter {
            mapBackendStatus(it.status) == status
        }
        val spotFiltered = applySpotFilter(filteredBookings)
        val sortedBookings = sortBookings(spotFiltered)
        android.util.Log.d("BookingsFragment", "Filtered bookings count: ${filteredBookings.size}")
        
        // Convert to UI bookings before updating adapter
        val uiBookings = sortedBookings.map { convertToBooking(it) }
        updateAdapterWithBookings(uiBookings)

        // Update visibility
        binding.rvBookings.visibility = if (spotFiltered.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmptyState.visibility = if (spotFiltered.isEmpty()) View.VISIBLE else View.GONE
        android.util.Log.d("BookingsFragment", "RecyclerView visibility: ${binding.rvBookings.visibility}, EmptyState visibility: ${binding.layoutEmptyState.visibility}")
    }

    private fun handleSegmentSelection(newStatus: BookingStatus, userTriggered: Boolean = true) {
        android.util.Log.d("BookingsFragment", "handleSegmentSelection called with status: $newStatus")
        updateSegmentVisualState(newStatus)

        if (newStatus != currentTab) {
            if (userTriggered) {
                val rootView = binding.segmentedControlContainer.segmentRoot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rootView?.performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                    )
                } else {
                    rootView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                showToast("Switched to ${getSegmentTitle(newStatus)}")
            }
            currentTab = newStatus
            showBookingsForStatus(newStatus)
        }
    }

    private fun loadBookingsFromAPI() {
        android.util.Log.d("BookingsFragment", "loadBookingsFromAPI called")
        // Use real API; do not inject sample data
        loadUserBookings()
    }

    // Removed sample bookings generator

        private fun mapBackendBookingToUI(backendBooking: BackendBooking): Booking {
        // TODO: Implement proper mapping when all backend properties are available
        val loc = parkingLotCache[backendBooking.lotId] ?: (backendBooking.lotId ?: "")
        val spot = backendBooking.spotId ?: ""
        return Booking(
            id = backendBooking.id ?: "unknown",
            locationName = loc,
            spotName = spot,
            vehicleNumber = backendBooking.vehicleNumber ?: "",
            amount = backendBooking.amount?.let { "₹${String.format("%.2f", it)}" } ?: "",
            status = when (backendBooking.status?.lowercase(Locale.ROOT)) {
                "active" -> BookingStatus.ACTIVE
                "pending" -> BookingStatus.PENDING
                "completed" -> BookingStatus.COMPLETED
                else -> BookingStatus.PENDING
            },
            spotId = spot,
            locationAddress = "",
            startTime = "",
            endTime = "",
            duration = "",
            bookingDate = ""
        )
    }

    private fun onBookingClicked(booking: Booking) {
        showBookingDetails(booking)
    }

    override fun scrollToTop() {
        try {
            binding.rvBookings.smoothScrollToPosition(0)
        } catch (e: IllegalStateException) {
            // Handle case where fragment is not attached
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "My Bookings"
    }

    private fun setupSegmentedControl() {
        val container = binding.segmentedControlContainer
        val segments = listOf(
            container.segmentActive to BookingStatus.ACTIVE,
            container.segmentPending to BookingStatus.PENDING,
            container.segmentCompleted to BookingStatus.COMPLETED
        )

        segments.forEach { (segmentView, status) ->
            applySegmentRipple(segmentView)
            segmentView.setOnClickListener {
                handleSegmentSelection(status)
            }
        }

        container.segmentGroup.doOnLayout {
            positionSliderInstantly(getSegmentView(currentTab), currentTab)
        }

        setupSliderDragGesture()

        updateSegmentVisualState(currentTab)
        applyPendingNavigationIfReady()
    }

    private fun updateSegmentVisualState(selectedStatus: BookingStatus) {
        val container = binding.segmentedControlContainer
        val isActive = selectedStatus == BookingStatus.ACTIVE
        val isPending = selectedStatus == BookingStatus.PENDING
        val isCompleted = selectedStatus == BookingStatus.COMPLETED

        // Animate the previously selected segment out
        val segments = listOf(
            container.segmentActive to isActive,
            container.segmentPending to isPending,
            container.segmentCompleted to isCompleted
        )

        segments.forEach { (segment, shouldBeSelected) ->
            if (segment.isSelected != shouldBeSelected) {
                // Animate selection change
                if (shouldBeSelected) {
                    // Animate in
                    animateSegmentIn(segment)
                } else {
                    // Animate out
                    animateSegmentOut(segment)
                }
                segment.isSelected = shouldBeSelected
            }
        }

        animateSegmentSlider(getSegmentView(selectedStatus), selectedStatus)
    }

    private fun getSegmentView(status: BookingStatus): View {
        val container = binding.segmentedControlContainer
        return when (status) {
            BookingStatus.ACTIVE -> container.segmentActive
            BookingStatus.PENDING -> container.segmentPending
            BookingStatus.COMPLETED -> container.segmentCompleted
            else -> container.segmentActive
        }
    }

    private fun positionSliderInstantly(targetSegment: View, targetStatus: BookingStatus) {
        val container = binding.segmentedControlContainer
        val slider = container.segmentSlider ?: return
        val root = container.segmentRoot ?: return

        if (targetSegment.width == 0 || !root.isLaidOut) {
            root.doOnLayout { positionSliderInstantly(targetSegment, targetStatus) }
            return
        }

        val params = slider.layoutParams
        params.width = targetSegment.width
        slider.layoutParams = params
        slider.translationX = calculateSliderTargetX(targetSegment, root)
        slider.visibility = View.VISIBLE
        updateSegmentLabelsForSlider(selectedStatusOverride = targetStatus)
    }

    private fun animateSegmentSlider(targetSegment: View, targetStatus: BookingStatus) {
        if (isSliderDragging) return
        val container = binding.segmentedControlContainer
        val slider = container.segmentSlider ?: return
        val root = container.segmentRoot ?: return

        if (!targetSegment.isLaidOut || !root.isLaidOut || !slider.isLaidOut) {
            root.post { animateSegmentSlider(targetSegment, targetStatus) }
            return
        }

        val targetWidth = targetSegment.width.takeIf { it > 0 } ?: return
        val targetX = calculateSliderTargetX(targetSegment, root)

        if (slider.visibility != View.VISIBLE) {
            positionSliderInstantly(targetSegment, targetStatus)
            return
        }

        if (slider.translationX == targetX && slider.width == targetWidth) {
            updateSegmentLabelsForSlider(selectedStatusOverride = targetStatus)
            return
        }

        val widthAnimator = ValueAnimator.ofInt(slider.width, targetWidth).apply {
            addUpdateListener { animator ->
                val params = slider.layoutParams
                params.width = animator.animatedValue as Int
                slider.layoutParams = params
                updateSegmentLabelsForSlider(
                    selectedStatusOverride = targetStatus,
                    allowPostLayout = false
                )
            }
        }

        val translationAnimator = ObjectAnimator.ofFloat(
            slider,
            View.TRANSLATION_X,
            slider.translationX,
            targetX
        ).apply {
            addUpdateListener {
                updateSegmentLabelsForSlider(
                    selectedStatusOverride = targetStatus,
                    allowPostLayout = false
                )
            }
            doOnEnd {
                updateSegmentLabelsForSlider(
                    selectedStatusOverride = targetStatus,
                    allowPostLayout = false
                )
            }
        }

        AnimatorSet().apply {
            playTogether(widthAnimator, translationAnimator)
            duration = 280
            interpolator = OvershootInterpolator(0.55f)
            start()
        }
    }

    private fun updateSegmentLabelsForSlider(
        sliderCenterOverride: Float? = null,
        selectedStatusOverride: BookingStatus? = null,
        allowPostLayout: Boolean = true
    ) {
        val container = binding.segmentedControlContainer
        val slider = container.segmentSlider ?: return
        val root = container.segmentRoot ?: return

        val needsLayout = !root.isLaidOut ||
            slider.width == 0 ||
            container.segmentActive.width == 0 ||
            container.segmentPending.width == 0 ||
            container.segmentCompleted.width == 0

        if (needsLayout) {
            if (allowPostLayout) {
                root.doOnLayout {
                    updateSegmentLabelsForSlider(
                        sliderCenterOverride,
                        selectedStatusOverride,
                        false
                    )
                }
            }
            return
        }

        val sliderCenter = sliderCenterOverride ?: (slider.translationX + slider.width / 2f)
        val selection = selectedStatusOverride ?: currentTab

        val segments = listOf(
            Triple(BookingStatus.ACTIVE, container.segmentActive, container.textActive),
            Triple(BookingStatus.PENDING, container.segmentPending, container.textPending),
            Triple(BookingStatus.COMPLETED, container.segmentCompleted, container.textCompleted)
        )

        segments.forEach { (status, segment, label) ->
            val width = segment.width
            if (width == 0) return@forEach
            val segmentCenter = segment.left + width / 2f
            val distance = abs(sliderCenter - segmentCenter)
            val influenceRadius = (width * 0.9f).coerceAtLeast(1f)
            val emphasis = (1f - (distance / influenceRadius)).coerceIn(0f, 1f)
            val blendedColor = blendColors(unselectedLabelColor, selectedLabelColor, emphasis)
            if (label.currentTextColor != blendedColor) {
                label.setTextColor(blendedColor)
            }
            val scale = 0.94f + 0.06f * emphasis
            label.scaleX = scale
            label.scaleY = scale
            label.alpha = 0.6f + 0.4f * emphasis
            label.isSelected = status == selection
        }
    }

    @ColorInt
    private fun blendColors(@ColorInt fromColor: Int, @ColorInt toColor: Int, ratio: Float): Int {
        val clamped = ratio.coerceIn(0f, 1f)
        val inverse = 1f - clamped
        val a = (Color.alpha(fromColor) * inverse + Color.alpha(toColor) * clamped).roundToInt()
        val r = (Color.red(fromColor) * inverse + Color.red(toColor) * clamped).roundToInt()
        val g = (Color.green(fromColor) * inverse + Color.green(toColor) * clamped).roundToInt()
        val b = (Color.blue(fromColor) * inverse + Color.blue(toColor) * clamped).roundToInt()
        return Color.argb(a, r, g, b)
    }

    private fun calculateSliderTargetX(targetSegment: View, root: View): Float {
        val segmentLeft = targetSegment.left.toFloat()
        return segmentLeft.coerceIn(0f, (root.width - targetSegment.width).coerceAtLeast(0).toFloat())
    }

    private fun animateSegmentIn(segment: View) {
        // Subtle scale up and fade in for selected state
        val scaleX = ObjectAnimator.ofFloat(segment, "scaleX", 0.98f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(segment, "scaleY", 0.98f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(segment, "alpha", 0.7f, 1.0f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 200
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun animateSegmentOut(segment: View) {
        // Subtle scale down and fade out for unselected state
        val scaleX = ObjectAnimator.ofFloat(segment, "scaleX", 1.0f, 0.98f)
        val scaleY = ObjectAnimator.ofFloat(segment, "scaleY", 1.0f, 0.98f)
        val alpha = ObjectAnimator.ofFloat(segment, "alpha", 1.0f, 0.85f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 200
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun setupSliderDragGesture() {
        val container = binding.segmentedControlContainer
        container.segmentRoot?.setOnTouchListener { view, event ->
            val slider = container.segmentSlider ?: return@setOnTouchListener false
            if (!slider.isShown || slider.width == 0) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val sliderStart = slider.translationX
                    val sliderEnd = sliderStart + slider.width
                    val withinSlider = event.x in sliderStart..sliderEnd
                    if (withinSlider) {
                        isSliderDragging = true
                        sliderDragOffset = event.x - sliderStart
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        true
                    } else {
                        isSliderDragging = false
                        false
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isSliderDragging) return@setOnTouchListener false
                    val desiredTranslation = event.x - sliderDragOffset
                    slider.translationX = clampSliderTranslation(desiredTranslation, view, slider)
                    updateSegmentLabelsForSlider()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isSliderDragging) return@setOnTouchListener false
                    isSliderDragging = false
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    val status = determineNearestStatus(slider, view)
                    handleSegmentSelection(status)
                    true
                }
                else -> false
            }
        }
    }

    private fun clampSliderTranslation(desiredTranslation: Float, root: View, slider: View): Float {
        val min = 0f
        val max = (root.width - slider.width).coerceAtLeast(0).toFloat()
        return desiredTranslation.coerceIn(min, max)
    }

    private fun determineNearestStatus(slider: View, root: View): BookingStatus {
        val sliderCenter = slider.translationX + slider.width / 2f
        val container = binding.segmentedControlContainer
        val centers = listOf(
            BookingStatus.ACTIVE to container.segmentActive,
            BookingStatus.PENDING to container.segmentPending,
            BookingStatus.COMPLETED to container.segmentCompleted
        )
        return centers.minByOrNull { (_, view) ->
            abs(sliderCenter - (view.left + view.width / 2f))
        }?.first ?: currentTab
    }

    private fun applySegmentRipple(segment: View) {
        val rippleColor = RippleUtils.convertToRippleDrawableColor(
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.segment_ripple_active))
        )
        val cornerRadius = resources.getDimension(R.dimen.segmented_control_height) / 2f
        val shapeAppearance = ShapeAppearanceModel.builder()
            .setAllCornerSizes(cornerRadius)
            .build()
        val mask = MaterialShapeDrawable(shapeAppearance).apply {
            fillColor = ColorStateList.valueOf(Color.WHITE)
        }
        val ripple = RippleDrawable(rippleColor, ColorDrawable(Color.TRANSPARENT), mask)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            segment.foreground = ripple
        } else {
            segment.background = ripple
        }
    }
    
    /**
     * Handle segment selection from tap
     */
    override fun onDestroyView() {
        sortBottomSheetDialog?.dismiss()
        sortBottomSheetDialog = null
        toggleBackgroundBlur(false)
        super.onDestroyView()
    }

    private fun showBookingsForStatus(status: BookingStatus) {
        updateSegmentBadges()
        val filteredBookings = userBookings.filter { mapBackendStatus(it.status) == status }
        val spotFiltered = applySpotFilter(filteredBookings)
        val dateFiltered = applyDateFilter(spotFiltered)
        val sortedBookings = sortBookings(dateFiltered)
        
        if (sortedBookings.isEmpty()) {
            // Show empty state
            binding.rvBookings.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            
            // Update empty state text based on status
            when (status) {
                BookingStatus.ACTIVE -> {
                    binding.tvEmptyTitle.text = "No Active Bookings"
                    binding.tvEmptySubtitle.text = "Your active parking bookings will appear here"
                }
                BookingStatus.PENDING -> {
                    binding.tvEmptyTitle.text = "No Pending Bookings"
                    binding.tvEmptySubtitle.text = "Your pending bookings will appear here"
                }
                BookingStatus.COMPLETED -> {
                    binding.tvEmptyTitle.text = "No Completed Bookings"
                    binding.tvEmptySubtitle.text = "Your booking history will appear here"
                }
                else -> {
                    binding.tvEmptyTitle.text = "No Bookings"
                    binding.tvEmptySubtitle.text = "Your bookings will appear here"
                }
            }
        } else {
            // Show bookings list
            binding.rvBookings.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            
            // Convert to UI bookings before updating adapter
            val uiBookings = sortedBookings.map { convertToBooking(it) }
            updateAdapterWithBookings(uiBookings)
        }
    }

    private fun updateSegmentBadges() {
        val active = userBookings.count { mapBackendStatus(it.status) == BookingStatus.ACTIVE }
        val pending = userBookings.count { mapBackendStatus(it.status) == BookingStatus.PENDING }
        val completed = userBookings.count { mapBackendStatus(it.status) == BookingStatus.COMPLETED }

        val container = binding.segmentedControlContainer
        setBadgeState(container.badgeActive, active)
        setBadgeState(container.badgePending, pending)
        setBadgeState(container.badgeCompleted, completed)
    }

    private fun sortBookings(bookings: List<BackendBooking>): List<BackendBooking> {
        return when (currentSortOption) {
            BookingSortOption.NEWEST_FIRST -> bookings.sortedByDescending { getComparableTimestamp(it) }
            BookingSortOption.OLDEST_FIRST -> bookings.sortedBy { getComparableTimestamp(it) }
        }
    }

    private fun applySpotFilter(bookings: List<BackendBooking>): List<BackendBooking> {
        val selection = selectedSpotFilter?.takeUnless { it.isBlank() } ?: return bookings
        val normalized = selection.lowercase(Locale.ROOT)
        return bookings.filter {
            val label = getSpotLabel(it).trim().lowercase(Locale.ROOT)
            label == normalized
        }
    }

    private fun applyDateFilter(bookings: List<BackendBooking>): List<BackendBooking> {
        val startDate = filterStartDate
        val endDate = filterEndDate
        
        if (startDate == null && endDate == null) return bookings
        
        return bookings.filter { booking ->
            val bookingTime = booking.checkInTime?.time ?: booking.createdAt?.time ?: return@filter true
            
            when {
                startDate != null && endDate != null -> {
                    bookingTime >= startDate && bookingTime <= endDate + 86400000L // Add 24h to include end date
                }
                startDate != null -> bookingTime >= startDate
                endDate != null -> bookingTime <= endDate + 86400000L
                else -> true
            }
        }
    }

    private fun setupDateFilter(sheetBinding: BottomSheetBookingFiltersBinding) {
        // Update date display
        updateDateDisplay(sheetBinding)
        
        // Start date picker with haptic feedback and scale animation
        sheetBinding.cardStartDate.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateCardPress(view)
            showMinimalDatePicker(
                title = "Select Start Date",
                selectedDate = filterStartDate,
                maxDate = filterEndDate
            ) { selectedDate ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                filterStartDate = selectedDate
                updateDateDisplay(sheetBinding)
                animateDateSelected(sheetBinding.textStartDate)
                showBookingsForStatus(currentTab)
            }
        }
        
        // End date picker with haptic feedback and scale animation
        sheetBinding.cardEndDate.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateCardPress(view)
            showMinimalDatePicker(
                title = "Select End Date",
                selectedDate = filterEndDate,
                minDate = filterStartDate
            ) { selectedDate ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                filterEndDate = selectedDate
                updateDateDisplay(sheetBinding)
                animateDateSelected(sheetBinding.textEndDate)
                showBookingsForStatus(currentTab)
            }
        }
        
        // Clear date filter with haptic feedback
        sheetBinding.buttonClearDateFilter.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            filterStartDate = null
            filterEndDate = null
            updateDateDisplay(sheetBinding)
            animateClearFilter(sheetBinding)
            showBookingsForStatus(currentTab)
        }
    }

    private fun animateCardPress(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateDateSelected(textView: TextView) {
        textView.alpha = 0f
        textView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun animateClearFilter(sheetBinding: BottomSheetBookingFiltersBinding) {
        listOf(sheetBinding.textStartDate, sheetBinding.textEndDate).forEach { textView ->
            textView.animate()
                .alpha(0.5f)
                .setDuration(150)
                .withEndAction {
                    textView.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }

    private fun updateDateDisplay(sheetBinding: BottomSheetBookingFiltersBinding) {
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        sheetBinding.textStartDate.text = filterStartDate?.let { 
            dateFormat.format(java.util.Date(it)) 
        } ?: "Select Date"
        
        sheetBinding.textEndDate.text = filterEndDate?.let { 
            dateFormat.format(java.util.Date(it)) 
        } ?: "Select Date"
        
        // Show/hide clear button with animation
        val shouldShow = filterStartDate != null || filterEndDate != null
        if (shouldShow && sheetBinding.buttonClearDateFilter.visibility != View.VISIBLE) {
            sheetBinding.buttonClearDateFilter.alpha = 0f
            sheetBinding.buttonClearDateFilter.visibility = View.VISIBLE
            sheetBinding.buttonClearDateFilter.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        } else if (!shouldShow && sheetBinding.buttonClearDateFilter.visibility == View.VISIBLE) {
            sheetBinding.buttonClearDateFilter.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    sheetBinding.buttonClearDateFilter.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showMinimalDatePicker(
        title: String,
        selectedDate: Long? = null,
        minDate: Long? = null,
        maxDate: Long? = null,
        onDateSelected: (Long) -> Unit
    ) {
        val calendar = java.util.Calendar.getInstance()
        selectedDate?.let { calendar.timeInMillis = it }
        
        // Build constraints
        val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
        
        minDate?.let { 
            constraintsBuilder.setStart(it)
        }
        maxDate?.let { 
            constraintsBuilder.setEnd(it)
        }
        
        // Create date picker with custom theme, smooth animations, and haptic feedback
        val datePickerDialog = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setSelection(selectedDate ?: calendar.timeInMillis)
            .setTheme(R.style.CustomDatePickerTheme)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
        
        // Add haptic feedback on positive button (Confirm)
        datePickerDialog.addOnPositiveButtonClickListener { selection ->
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onDateSelected(selection)
        }
        
        // Add subtle haptic feedback on negative button (Cancel)
        datePickerDialog.addOnNegativeButtonClickListener {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
        
        // Add subtle haptic feedback on dismiss
        datePickerDialog.addOnDismissListener {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        
        datePickerDialog.show(parentFragmentManager, "DATE_PICKER")
        
        // Apply smooth backdrop animation and setup date selection haptics
        Handler(Looper.getMainLooper()).postDelayed({
            datePickerDialog.dialog?.window?.apply {
                setDimAmount(0.5f)
                attributes?.windowAnimations = R.style.DatePickerDialogAnimation
            }
            
            // Add haptic feedback for date cell selections
            setupDatePickerHaptics(datePickerDialog)
        }, 50)
    }
    
    private fun setupDatePickerHaptics(datePickerDialog: com.google.android.material.datepicker.MaterialDatePicker<Long>) {
        try {
            // Find the calendar view and add touch listeners for haptic feedback
            datePickerDialog.dialog?.findViewById<View>(com.google.android.material.R.id.month_grid)?.let { monthGrid ->
                if (monthGrid is ViewGroup) {
                    addHapticToDateCells(monthGrid)
                }
            }
        } catch (e: Exception) {
            android.util.Log.d("BookingsFragment", "Could not add haptics to date cells: ${e.message}")
        }
    }
    
    private fun addHapticToDateCells(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                addHapticToDateCells(child)
            } else {
                // Add subtle haptic feedback and scale animation on date cell touch
                child.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            // Haptic feedback
                            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            
                            // Subtle scale animation
                            v.animate()
                                .scaleX(1.08f)
                                .scaleY(1.08f)
                                .setDuration(75)
                                .setInterpolator(android.view.animation.DecelerateInterpolator())
                                .start()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // Scale back to normal
                            v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(75)
                                .setInterpolator(android.view.animation.AccelerateInterpolator())
                                .start()
                        }
                    }
                    false // Don't consume the event
                }
            }
        }
    }

    private fun getComparableTimestamp(booking: BackendBooking): Long {
        return booking.checkInTime?.time
            ?: booking.createdAt?.time
            ?: abs(booking.id?.hashCode() ?: 0).toLong()
    }

    private fun getSpotLabel(booking: BackendBooking): String {
        val spotId = booking.spotId?.takeIf { it.isNotBlank() } ?: return ""
        return parkingSpotCache[spotId] ?: spotId
    }

    private fun ensureSpotFilterIsValid() {
        val currentSelection = selectedSpotFilter?.lowercase(Locale.ROOT) ?: return
        val hasMatch = userBookings.any {
            val label = getSpotLabel(it).trim()
            label.isNotEmpty() && label.lowercase(Locale.ROOT) == currentSelection
        }
        if (!hasMatch) {
            selectedSpotFilter = null
        }
    }

    private fun setBadgeState(badge: TextView, @Suppress("UNUSED_PARAMETER") count: Int) {
        // Hide badge counts per latest design; leave the view gone regardless of data
        badge.visibility = View.GONE
    }

    private fun updateAdapterWithBookings(bookings: List<Booking>) {
        android.util.Log.d("BookingsFragment", "updateAdapterWithBookings called with ${bookings.size} bookings")
        bookingsAdapter.updateBookings(bookings)
        
        // Show bookings count in UI for now
        if (bookings.isNotEmpty()) {
            showToast("Found ${bookings.size} bookings")
        }

        pendingHighlightBookingId?.let { highlightId ->
            val index = bookings.indexOfFirst { it.id == highlightId }
            if (index >= 0) {
                binding.rvBookings.post {
                    binding.rvBookings.smoothScrollToPosition(index)
                }
                showToast("Showing your latest booking")
                pendingHighlightBookingId = null
            }
        }
    }

    private fun loadUserBookings() {
        if (!binding.swipeRefresh.isRefreshing) {
            binding.progressLoading.visibility = View.VISIBLE
        }
        
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to view your bookings")
            binding.progressLoading.visibility = View.GONE
            // Show empty state
            showBookingsForStatus(currentTab)
            return
        }
        
        lifecycleScope.launch {
            try {
                // Load parking lots and spots cache first
                if (!isCacheLoaded) {
                    loadParkingDataCache()
                }
                
                // Get user bookings from backend using real user ID
                val primary = ApiClient.apiService.getUserBookings(userId)
                val backendBookings = if (primary.isSuccessful) {
                    primary.body() ?: emptyList()
                } else if (primary.code() == 404) {
                    emptyList()
                } else {
                    emptyList()
                }
                if (backendBookings.isNotEmpty()) {
                    
                    // Convert backend bookings to UI bookings
                    userBookings.clear()
                    userBookings.addAll(backendBookings)
                    ensureSpotFilterIsValid()

                    backendBookings.forEach { booking ->
                        android.util.Log.d(
                            "BookingsFragment",
                            "Raw booking status from backend: '${booking.status}' mapped to ${mapBackendStatus(booking.status)}"
                        )
                    }
                    
                    // Update UI for current tab
                    showBookingsForStatus(currentTab)
                } else {
                    // No bookings found - show empty state
                    userBookings.clear()
                    selectedSpotFilter = null
                    showBookingsForStatus(currentTab)
                }
                
            } catch (e: Exception) {
                // Handle error - for development, show sample data
                showToast("Network error: ${e.message}")
                // do not load sample data automatically for production view
                userBookings.clear()
                showBookingsForStatus(currentTab)
            } finally {
                binding.progressLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyPendingNavigationIfReady() {
        val request = pendingNavigationRequest ?: return
        if (!isAdded || view == null) return

        pendingNavigationRequest = null

        val targetStatus = if (request.showPending) BookingStatus.PENDING else BookingStatus.ACTIVE
        pendingHighlightBookingId = request.highlightBookingId

        handleSegmentSelection(targetStatus, userTriggered = false)
    }
    
    private suspend fun loadParkingDataCache() {
        try {
            // ✅ Load ALL parking spots first (works now after JsonNull fix!)
            try {
                val allSpotsResponse = ApiClient.apiService.getParkingSpots()
                if (allSpotsResponse.isSuccessful) {
                    allSpotsResponse.body()?.forEach { spot ->
                        val spotName = spot.name ?: spot.zoneName ?: "Spot ${spot.id}"
                        parkingSpotCache[spot.id] = spotName
                        android.util.Log.d("BookingsFragment", "Cached spot: ${spot.id} -> $spotName")
                    }
                    android.util.Log.d("BookingsFragment", "Loaded ${parkingSpotCache.size} spots from /api/parking-spots")
                }
            } catch (e: Exception) {
                android.util.Log.e("BookingsFragment", "Error loading all parking spots: ${e.message}")
            }
            
            // Load parking lots
            val lotsResponse = ApiClient.apiService.getParkingLots()
            if (lotsResponse.isSuccessful) {
                lotsResponse.body()?.forEach { lot ->
                    parkingLotCache[lot.id] = lot.name
                    android.util.Log.d("BookingsFragment", "Cached lot: ${lot.id} -> ${lot.name}")
                    
                    // Also try to load spots for this lot (as fallback)
                    try {
                        val spotsForLot = ApiClient.apiService.getParkingSpotsByLot(lot.id)
                        if (spotsForLot.isSuccessful) {
                            spotsForLot.body()?.forEach { spot ->
                                // Only cache if not already cached from /api/parking-spots
                                if (!parkingSpotCache.containsKey(spot.id)) {
                                    val spotName = spot.name ?: spot.zoneName ?: "Spot ${spot.id}"
                                    parkingSpotCache[spot.id] = spotName
                                    android.util.Log.d("BookingsFragment", "Cached spot from lot: ${spot.id} -> ${spotName}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BookingsFragment", "Error loading spots for lot ${lot.id}: ${e.message}")
                    }
                }
            }
            
            // Avoid admin-only all-spots endpoint; rely on by-lot cache above
            
            isCacheLoaded = true
            android.util.Log.d("BookingsFragment", "Cache loaded: ${parkingLotCache.size} lots, ${parkingSpotCache.size} spots")
        } catch (e: Exception) {
            android.util.Log.e("BookingsFragment", "Error loading parking data cache: ${e.message}")
            // Continue without cache - will use fallback names
        }
    }

    // Removed sample bookings loader; rely on backend only

    private fun convertToBooking(backendBooking: BackendBooking): Booking {
        // Get parking location name from cache, fallback to generated name
        val parkingLocation = parkingLotCache[backendBooking.lotId]
            ?: (backendBooking.lotId ?: "Unknown Lot")
        
        // Get spot name from cache with debug logging
        val spotId = backendBooking.spotId ?: "Unknown"
        android.util.Log.d("BookingsFragment", "Looking up spot: $spotId in cache (${parkingSpotCache.size} entries)")
        
        val spotName = parkingSpotCache[spotId]
            ?: spotId
        
        // Format date and time from backend data
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        val bookingDate = backendBooking.createdAt?.let { dateFormat.format(it) } ?: "TBD"
        val startTime = backendBooking.checkInTime?.let { timeFormat.format(it) } ?: "TBD"
        val endTime = backendBooking.checkOutTime?.let { timeFormat.format(it) } ?: "TBD"
        
        // Calculate duration if both times are available
        val duration = if (backendBooking.checkInTime != null && backendBooking.checkOutTime != null) {
            val durationMillis = backendBooking.checkOutTime.time - backendBooking.checkInTime.time
            val hours = durationMillis / (1000 * 60 * 60)
            val minutes = (durationMillis % (1000 * 60 * 60)) / (1000 * 60)
            "${hours}h ${minutes}m"
        } else {
            "TBD"
        }
        
        return Booking(
            id = backendBooking.id ?: "Unknown",
            locationName = parkingLocation,
            spotName = spotName,
            vehicleNumber = backendBooking.vehicleNumber ?: "",
            amount = if (backendBooking.amount != null) "₹${String.format("%.2f", backendBooking.amount)}" else "",
            status = mapBackendStatus(backendBooking.status),
            spotId = backendBooking.spotId ?: "",
            locationAddress = "",
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            bookingDate = bookingDate
        )
    }

    private fun mapBackendStatus(backendStatus: String?): BookingStatus {
        val normalized = backendStatus?.trim()?.lowercase(Locale.ROOT)?.replace(' ', '_') ?: return BookingStatus.PENDING

        return when {
            normalized.isEmpty() -> BookingStatus.PENDING
            normalized in ACTIVE_STATUSES -> BookingStatus.ACTIVE
            normalized in PENDING_STATUSES -> BookingStatus.PENDING
            normalized in COMPLETED_STATUSES -> BookingStatus.COMPLETED

            // Substring-based fallbacks for unexpected variants
            normalized.contains("check_out") || normalized.contains("checkout") ||
                normalized.contains("complete") || normalized.contains("finish") ||
                normalized.contains("cancel") || normalized.contains("expire") ||
                normalized.contains("no_show") || normalized.contains("noshow") ||
                normalized.contains("auto") -> BookingStatus.COMPLETED

            normalized.contains("check_in") || normalized.contains("checkin") ||
                normalized.contains("in_progress") || normalized.contains("inprogress") ||
                normalized.contains("ongoing") || normalized.contains("running") ||
                normalized.contains("active") -> BookingStatus.ACTIVE

            normalized.contains("pending") || normalized.contains("await") ||
                normalized.contains("reserve") || normalized.contains("schedule") ||
                normalized.contains("confirm") || normalized.contains("book") ||
                normalized.contains("init") || normalized.contains("hold") -> BookingStatus.PENDING

            else -> {
                android.util.Log.w("BookingsFragment", "Unknown booking status '$backendStatus', defaulting to Pending")
                BookingStatus.PENDING
            }
        }
    }

    companion object {
        private const val ALL_SPOTS_TAG = "__ALL_SPOTS__"
        private val PENDING_STATUSES = setOf(
            "pending",
            "created",
            "booked",
            "reserved",
            "scheduled",
            "pending_confirmation",
            "pending-confirmation",
            "pending_payment",
            "pending-payment",
            "awaiting_payment",
            "awaiting-payment",
            "awaiting_checkin",
            "awaiting-checkin",
            "initiated",
            "confirmed"
        )

        private val ACTIVE_STATUSES = setOf(
            "active",
            "in_progress",
            "in-progress",
            "ongoing",
            "ongoing_session",
            "live",
            "checked_in",
            "checked-in"
        )

        private val COMPLETED_STATUSES = setOf(
            "completed",
            "finished",
            "cancelled",
            "canceled",
            "expired",
            "checked_out",
            "checked-out",
            "no_show",
            "no-show",
            "auto_completed",
            "auto-completed"
        )
    }

    private fun showBookingDetails(booking: Booking) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = BottomSheetBookingOverviewBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    window.setBackgroundBlurRadius(80)
                    window.setDimAmount(0f)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.setDimAmount(0f)
                }
                else -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window.setDimAmount(0.25f)
                }
            }
        }

        toggleBackgroundBlur(true)

        sheetBinding.apply {
            textLocationName.text = booking.locationName
            textLocationInitial.text = booking.locationName.firstOrNull()?.uppercaseChar()?.toString() ?: "B"

            textLocationAddress.text = when {
                booking.locationAddress.isNotBlank() && booking.locationAddress != "Address TBD" -> booking.locationAddress
                booking.spotId.isNotBlank() -> booking.spotId
                else -> getString(R.string.booking_details)
            }

            textSpotName.text = booking.spotName
            textVehicleNumber.text = booking.vehicleNumber
            textBookingId.text = "#${booking.id}"

            textCheckIn.text = getStringFormattedTime(booking.bookingDate, booking.startTime)
            textCheckOut.text = getStringFormattedTime(booking.bookingDate, booking.endTime)
            textBookingDate.text = booking.bookingDate

            textDuration.text = booking.duration
            textAmount.text = booking.amount

            val (statusLabel, statusBackgroundRes, statusTextColor) = when (booking.status) {
                BookingStatus.ACTIVE -> Triple("Active", R.drawable.status_outlined_active, R.color.booking_status_active_text)
                BookingStatus.PENDING -> Triple("Pending", R.drawable.status_outlined_pending, R.color.booking_status_pending_text)
                BookingStatus.COMPLETED -> Triple("Completed", R.drawable.status_outlined_completed, R.color.booking_status_completed_text)
            }
            textStatusChip.text = statusLabel
            textStatusChip.background = ContextCompat.getDrawable(requireContext(), statusBackgroundRes)
            textStatusChip.setTextColor(ContextCompat.getColor(requireContext(), statusTextColor))

            locationBadgeContainer.background = ContextCompat.getDrawable(requireContext(), statusBackgroundRes)
            textLocationInitial.setTextColor(ContextCompat.getColor(requireContext(), statusTextColor))
        }

        // Show extend button for ACTIVE bookings only
        if (booking.status == BookingStatus.ACTIVE) {
            sheetBinding.btnExtendBooking.visibility = View.VISIBLE
            sheetBinding.btnExtendBooking.setOnClickListener {
                val backendBooking = userBookings.firstOrNull { it.id == booking.id }
                if (backendBooking != null) {
                    showExtendBookingDialog(backendBooking)
                } else {
                    showToast("Booking details not found")
                }
            }
        } else {
            sheetBinding.btnExtendBooking.visibility = View.GONE
        }

        sheetBinding.buttonCloseSheet.setOnClickListener { dialog.dismiss() }
        sheetBinding.actionDirections.setOnClickListener {
            showToast("Directions action coming soon")
        }
        sheetBinding.actionShare.setOnClickListener {
            showToast("Share action coming soon")
        }
        sheetBinding.actionCancel.setOnClickListener {
            when (booking.status) {
                BookingStatus.PENDING -> {
                    // Confirm cancellation
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Cancel Booking")
                        .setMessage("Are you sure you want to cancel this booking? Any holds or promotions may be released.")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes, Cancel") { _, _ ->
                            // Disable action to prevent double taps
                            sheetBinding.actionCancel.isEnabled = false
                            sheetBinding.actionCancel.alpha = 0.6f

                            lifecycleScope.launch {
                                try {
                                    val result = bookingRepository.cancelBooking(booking.id)
                                    if (result.isSuccess) {
                                        showToast("Booking cancelled")
                                        dialog.dismiss()
                                        loadUserBookings()
                                    } else {
                                        showToast(result.exceptionOrNull()?.message ?: "Failed to cancel booking")
                                        sheetBinding.actionCancel.isEnabled = true
                                        sheetBinding.actionCancel.alpha = 1f
                                    }
                                } catch (e: Exception) {
                                    showToast(e.message ?: "Failed to cancel booking")
                                    sheetBinding.actionCancel.isEnabled = true
                                    sheetBinding.actionCancel.alpha = 1f
                                }
                            }
                        }
                        .show()
                }
                BookingStatus.ACTIVE -> {
                    showToast("Active bookings cannot be cancelled. Please check out.")
                }
                BookingStatus.COMPLETED -> {
                    showToast("This booking is already completed.")
                }
            }
        }

        // Configure primary QR scan action based on status
        when (booking.status) {
            BookingStatus.PENDING -> {
                sheetBinding.buttonScanQr.visibility = View.VISIBLE
                sheetBinding.buttonScanQr.text = getString(R.string.scan_to_check_in)
                sheetBinding.buttonScanQr.setOnClickListener {
                    pendingScanBookingId = booking.id
                    pendingScanType = ScanType.CHECK_IN
                    val intent = Intent(requireContext(), QrScannerActivity::class.java).apply {
                        putExtra(QrScannerActivity.EXTRA_BOOKING_ID, booking.id)
                        putExtra(QrScannerActivity.EXTRA_SCAN_TYPE, ScanType.CHECK_IN.name)
                    }
                    dialog.dismiss()
                    qrScanLauncher.launch(intent)
                }
            }
            BookingStatus.ACTIVE -> {
                sheetBinding.buttonScanQr.visibility = View.VISIBLE
                sheetBinding.buttonScanQr.text = getString(R.string.scan_to_check_out)
                sheetBinding.buttonScanQr.setOnClickListener {
                    pendingScanBookingId = booking.id
                    pendingScanType = ScanType.CHECK_OUT
                    val intent = Intent(requireContext(), QrScannerActivity::class.java).apply {
                        putExtra(QrScannerActivity.EXTRA_BOOKING_ID, booking.id)
                        putExtra(QrScannerActivity.EXTRA_SCAN_TYPE, ScanType.CHECK_OUT.name)
                    }
                    dialog.dismiss()
                    qrScanLauncher.launch(intent)
                }
            }
            BookingStatus.COMPLETED -> {
                sheetBinding.buttonScanQr.visibility = View.GONE
            }
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
        }
        dialog.behavior.isDraggable = true
        dialog.show()

        dialog.setOnDismissListener {
            toggleBackgroundBlur(false)
        }
        dialog.setOnCancelListener {
            toggleBackgroundBlur(false)
        }
    }

    private fun showExtendBookingDialog(booking: BackendBooking) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_extend_booking, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val tvCurrentEndTime = dialogView.findViewById<TextView>(R.id.tv_current_end_time)
        val datePicker = dialogView.findViewById<DatePicker>(R.id.date_picker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.time_picker)
        val tvAdditionalCharges = dialogView.findViewById<TextView>(R.id.tv_additional_charges)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnExtend = dialogView.findViewById<Button>(R.id.btn_extend)

        val currentEndTime = booking.checkOutTime ?: Date()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        tvCurrentEndTime.text = getString(R.string.current_end_time, dateFormat.format(currentEndTime))

        val calendar = Calendar.getInstance().apply { time = currentEndTime }
        datePicker.minDate = currentEndTime.time
        datePicker.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        try {
            timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = calendar.get(Calendar.MINUTE)
        } catch (_: Throwable) {
            // ignore for older APIs
        }

        fun updateCharges() {
            val newCal = Calendar.getInstance()
            val selHour = try { timePicker.hour } catch (_: Throwable) { calendar.get(Calendar.HOUR_OF_DAY) }
            val selMin = try { timePicker.minute } catch (_: Throwable) { calendar.get(Calendar.MINUTE) }
            newCal.set(datePicker.year, datePicker.month, datePicker.dayOfMonth, selHour, selMin)

            val diffMillis = (newCal.timeInMillis - currentEndTime.time).coerceAtLeast(0L)
            val durationHours = (diffMillis / (1000 * 60 * 60)).toInt()
            val additionalCharge = durationHours * 100 // Adjust per pricing rules
            tvAdditionalCharges.text = getString(R.string.additional_charges, additionalCharge)
        }

        // initialize charges
        updateCharges()

        datePicker.setOnDateChangedListener { _, _, _, _ -> updateCharges() }
        try {
            timePicker.setOnTimeChangedListener { _, _, _ -> updateCharges() }
        } catch (_: Throwable) { }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnExtend.setOnClickListener {
            val newCal = Calendar.getInstance()
            val selHour = try { timePicker.hour } catch (_: Throwable) { calendar.get(Calendar.HOUR_OF_DAY) }
            val selMin = try { timePicker.minute } catch (_: Throwable) { calendar.get(Calendar.MINUTE) }
            newCal.set(datePicker.year, datePicker.month, datePicker.dayOfMonth, selHour, selMin)

            if (newCal.timeInMillis <= currentEndTime.time) {
                showToast(getString(R.string.new_time_must_be_later))
                return@setOnClickListener
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val newCheckOutTime = isoFormat.format(Date(newCal.timeInMillis))

            showExtendConfirmation(booking.id ?: return@setOnClickListener, newCheckOutTime, dialog)
        }

        dialog.show()
    }

    private fun showExtendConfirmation(bookingId: String, newCheckOutTime: String, extendDialog: Dialog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_extend_title))
            .setMessage(getString(R.string.confirm_extend_message))
            .setPositiveButton(getString(R.string.extend_booking)) { _, _ ->
                lifecycleScope.launch {
                    extendBooking(bookingId, newCheckOutTime)
                    extendDialog.dismiss()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private suspend fun extendBooking(bookingId: String, newCheckOutTime: String) {
        showLoading(true)
        val result = bookingRepository.extendBooking(bookingId, newCheckOutTime)
        result.fold(
            onSuccess = {
                showLoading(false)
                showToast(getString(R.string.booking_extended_success))
                refreshCurrentTab()
            },
            onFailure = { error ->
                showLoading(false)
                val message = error.message ?: getString(R.string.extend_failed_default)
                when {
                    message.contains("Insufficient wallet balance", ignoreCase = true) -> {
                        showToast(getString(R.string.insufficient_wallet_balance))
                    }
                    message.contains("not available", ignoreCase = true) -> {
                        showToast(getString(R.string.spot_not_available_extended))
                    }
                    else -> showToast(message)
                }
            }
        )
    }

    private fun showLoading(show: Boolean) {
        binding.progressLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun refreshCurrentTab() {
        loadUserBookings()
    }

    private fun getStringFormattedTime(date: String, time: String): String {
        return if (date == "TBD" || time == "TBD") {
            "TBD"
        } else {
            "$date · $time"
        }
    }

    private fun toggleBackgroundBlur(show: Boolean) {
        val activityRoot = requireActivity().findViewById<ViewGroup>(android.R.id.content) ?: return
        val contentRoot = activityRoot.getChildAt(0)
        if (show) {
            if (blurOverlayView == null) {
                blurOverlayView = View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.booking_sheet_overlay))
                    alpha = 0f
                    isClickable = true
                    isFocusable = true
                }
                activityRoot.addView(blurOverlayView)
            }
            blurOverlayView?.bringToFront()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                contentRoot?.setRenderEffect(RenderEffect.createBlurEffect(32f, 32f, Shader.TileMode.CLAMP))
            }
            blurOverlayView?.animate()?.alpha(1f)?.setDuration(180L)?.start()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                contentRoot?.setRenderEffect(null)
            }
            blurOverlayView?.animate()?.alpha(0f)?.setDuration(140L)?.withEndAction {
                blurOverlayView?.let { overlay ->
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                    blurOverlayView = null
                }
            }?.start()
        }
    }

    private suspend fun handleCheckInFlow(bookingId: String, qrCode: String) {
        try {
            showToast(getString(R.string.validating_qr))
            val validation = bookingRepository.validateCheckInQr(bookingId, qrCode)
            if (validation.isFailure) {
                showToast(validation.exceptionOrNull()?.message ?: getString(R.string.qr_invalid))
                return
            }
            val result = validation.getOrNull()
            if (result != null) {
                // If penalty applies, prompt user; simplified proceed
                if (result.penalty > 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Penalty on Check-In")
                        .setMessage(result.message)
                        .setPositiveButton("Proceed") { _, _ ->
                            lifecycleScope.launch { finalizeCheckIn(bookingId, qrCode) }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    finalizeCheckIn(bookingId, qrCode)
                }
            }
        } catch (e: Exception) {
            showToast(e.message ?: "Check-in error")
        }
    }

    private suspend fun finalizeCheckIn(bookingId: String, qrCode: String) {
        showToast(getString(R.string.processing_check_in))
        val checkInRes = bookingRepository.checkIn(bookingId, qrCode)
        if (checkInRes.isSuccess) {
            showToast("Checked in successfully")
            loadUserBookings()
        } else {
            showToast(checkInRes.exceptionOrNull()?.message ?: "Check-in failed")
        }
    }

    private suspend fun handleCheckOutFlow(bookingId: String, qrCode: String) {
        try {
            showToast(getString(R.string.validating_qr))
            val validation = bookingRepository.validateCheckOutQr(bookingId, qrCode)
            if (validation.isFailure) {
                showToast(validation.exceptionOrNull()?.message ?: getString(R.string.qr_invalid))
                return
            }
            val result = validation.getOrNull()
            if (result != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Check-Out Charges")
                    .setMessage(result.message)
                    .setPositiveButton("Pay & Check-Out") { _, _ ->
                        lifecycleScope.launch { finalizeCheckOut(bookingId, qrCode) }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } catch (e: Exception) {
            showToast(e.message ?: "Check-out error")
        }
    }

    private suspend fun finalizeCheckOut(bookingId: String, qrCode: String) {
        showToast(getString(R.string.processing_check_out))
        val checkOutRes = bookingRepository.checkOut(bookingId, qrCode)
        if (checkOutRes.isSuccess) {
            showToast("Checked out successfully")
            loadUserBookings()
        } else {
            showToast(checkOutRes.exceptionOrNull()?.message ?: "Check-out failed")
        }
    }

    private fun getUserId(): String? {
        // Primary: legacy prefs set by classic login/registration
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        val legacyId = sharedPref.getString("user_id", null)
        if (!legacyId.isNullOrBlank()) return legacyId

        // Fallback: JWT-based auth storage
        return try {
            com.gridee.parking.utils.JwtTokenManager(requireContext()).getUserId()
        } catch (_: Exception) {
            null
        }
    }

    private fun announceForAccessibility(message: String) {
        binding.root.announceForAccessibility(message)
    }
}
