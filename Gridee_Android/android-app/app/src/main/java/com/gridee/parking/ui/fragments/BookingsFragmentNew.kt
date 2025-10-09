package com.gridee.parking.ui.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.Booking as BackendBooking
import com.gridee.parking.databinding.FragmentBookingsNewBinding
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.ui.adapters.BookingStatus
import com.gridee.parking.ui.adapters.BookingsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.components.BookingDetailsBottomSheetSimple
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragmentNew : BaseTabFragment<FragmentBookingsNewBinding>() {

    private lateinit var bookingsAdapter: BookingsAdapter
    private var userBookings = mutableListOf<Booking>()
    private var currentTab = BookingStatus.ACTIVE

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
        setupSegmentedControl()
        loadUserBookings()
        showBookingsForStatus(BookingStatus.ACTIVE)
    }

    private fun setupRecyclerView() {
        bookingsAdapter = BookingsAdapter(emptyList()) { booking ->
            // Handle booking click - could show details or other actions
            showToast("Booking: ${booking.id}")
        }
        
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingsAdapter
        }
    }

    private fun setupSegmentedControl() {
        // Set initial indicator width and position
        binding.segmentActive.post {
            val segmentWidth = binding.segmentActive.width - 8 // Account for margins
            val layoutParams = binding.segmentIndicator.layoutParams
            layoutParams.width = segmentWidth
            binding.segmentIndicator.layoutParams = layoutParams
            binding.segmentIndicator.x = 4f // Start with 4dp padding
        }

        // Set click listeners with enhanced touch feedback
        binding.segmentActive.setOnClickListener { view ->
            performTouchFeedback(view)
            if (currentTab != BookingStatus.ACTIVE) {
                selectSegment(BookingStatus.ACTIVE, 0)
            }
        }

        binding.segmentPending.setOnClickListener { view ->
            performTouchFeedback(view)
            if (currentTab != BookingStatus.PENDING) {
                selectSegment(BookingStatus.PENDING, 1)
            }
        }

        binding.segmentCompleted.setOnClickListener { view ->
            performTouchFeedback(view)
            if (currentTab != BookingStatus.COMPLETED) {
                selectSegment(BookingStatus.COMPLETED, 2)
            }
        }
    }

    private fun selectSegment(status: BookingStatus, index: Int) {
        currentTab = status
        
        // Animate indicator
        animateIndicatorToSegment(index)
        
        // Update text colors
        updateSegmentTextColors(status)
        
        // Show bookings for selected status
        showBookingsForStatus(status)
        
        // Haptic feedback
        performHapticFeedback()
    }

    private fun animateIndicatorToSegment(index: Int) {
        val targetSegment = when (index) {
            0 -> binding.segmentActive
            1 -> binding.segmentPending
            2 -> binding.segmentCompleted
            else -> binding.segmentActive
        }

        val segmentWidth = targetSegment.width - 8 // Account for margins
        val targetX = when (index) {
            0 -> 4f
            1 -> targetSegment.x + 4f
            2 -> targetSegment.x + 4f
            else -> 4f
        }

        // Animate position with smooth bouncy effect
        val positionAnimator = ObjectAnimator.ofFloat(binding.segmentIndicator, "x", targetX).apply {
            duration = 350
            interpolator = OvershootInterpolator(0.6f)
        }

        // Animate width with smooth effect
        val currentWidth = binding.segmentIndicator.width
        val widthAnimator = ValueAnimator.ofInt(currentWidth, segmentWidth).apply {
            duration = 350
            interpolator = OvershootInterpolator(0.3f)
            addUpdateListener { animation ->
                val layoutParams = binding.segmentIndicator.layoutParams
                layoutParams.width = animation.animatedValue as Int
                binding.segmentIndicator.layoutParams = layoutParams
            }
        }

        // Start animations together for smooth transition
        positionAnimator.start()
        widthAnimator.start()
    }

    private fun updateSegmentTextColors(selectedStatus: BookingStatus) {
        // Reset all to secondary color
        binding.tvSegmentActive.setTextColor(resources.getColor(R.color.text_secondary, null))
        binding.tvSegmentPending.setTextColor(resources.getColor(R.color.text_secondary, null))
        binding.tvSegmentCompleted.setTextColor(resources.getColor(R.color.text_secondary, null))

        // Set selected to primary color with bold
        when (selectedStatus) {
            BookingStatus.ACTIVE -> {
                binding.tvSegmentActive.setTextColor(resources.getColor(R.color.text_primary, null))
                binding.tvSegmentActive.textSize = 14f
            }
            BookingStatus.PENDING -> {
                binding.tvSegmentPending.setTextColor(resources.getColor(R.color.text_primary, null))
                binding.tvSegmentPending.textSize = 14f
            }
            BookingStatus.COMPLETED -> {
                binding.tvSegmentCompleted.setTextColor(resources.getColor(R.color.text_primary, null))
                binding.tvSegmentCompleted.textSize = 14f
            }
        }
    }

    private fun showBookingsForStatus(status: BookingStatus) {
        val filteredBookings = userBookings.filter { it.status == status }
        
        if (filteredBookings.isEmpty()) {
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
            }
        } else {
            // Show bookings list
            binding.rvBookings.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            bookingsAdapter.updateBookings(filteredBookings)
        }
    }

    private fun performHapticFeedback() {
        try {
            binding.root.performHapticFeedback(
                android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            // Haptic feedback not available
        }
    }

    private fun performTouchFeedback(view: View) {
        // Enhanced haptic feedback for touch
        try {
            view.performHapticFeedback(
                android.view.HapticFeedbackConstants.KEYBOARD_TAP,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } catch (e: Exception) {
            // Fallback to older haptic feedback
            try {
                view.performHapticFeedback(
                    android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } catch (e2: Exception) {
                // Haptic feedback not available
            }
        }
    }

    private fun loadUserBookings() {
        binding.progressLoading.visibility = View.VISIBLE
        
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
                // Get user bookings from backend using real user ID
                val response = ApiClient.apiService.getUserBookings(userId)
                
                if (response.isSuccessful) {
                    val backendBookings = response.body() ?: emptyList()
                    
                    // Convert backend bookings to UI bookings
                    userBookings.clear()
                    userBookings.addAll(backendBookings.map { convertToUIBooking(it) })
                    
                    // Update UI for current tab
                    showBookingsForStatus(currentTab)
                } else {
                    // Handle API error - for development, show sample data if no real bookings
                    showToast("Server error: ${response.code()} - ${response.message()}")
                    // For production, you might want to show empty state instead of sample data
                    if (response.code() == 404) {
                        // No bookings found - show empty state
                        userBookings.clear()
                        showBookingsForStatus(currentTab)
                    } else {
                        // Other server errors - show sample data for testing
                        loadSampleBookings()
                    }
                }
                
            } catch (e: Exception) {
                // Handle error - for development, show sample data
                showToast("Network error: ${e.message}")
                loadSampleBookings()
            } finally {
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private fun loadSampleBookings() {
        // NOTE: This is fallback sample data shown only when:
        // 1. User is not logged in, OR
        // 2. API call fails due to network/server errors
        // In normal operation, real user bookings from the backend are displayed
        
        // Create sample bookings for demonstration
        val sampleBookings = listOf(
            Booking(
                id = "BK001",
                vehicleNumber = "MH01AB1234",
                spotId = "A-12",
                spotName = "A-12",
                locationName = "Downtown Mall Parking",
                locationAddress = "123 Main Street",
                startTime = "09:00 AM",
                endTime = "06:00 PM",
                duration = "9h 0m",
                amount = "₹150.00",
                bookingDate = "Today",
                status = BookingStatus.ACTIVE
            ),
            Booking(
                id = "BK002",
                vehicleNumber = "MH01AB1234",
                spotId = "B-05",
                spotName = "B-05",
                locationName = "Office Complex",
                locationAddress = "456 Business District",
                startTime = "08:30 AM",
                endTime = "05:30 PM",
                duration = "9h 0m",
                amount = "₹200.00",
                bookingDate = "Yesterday",
                status = BookingStatus.COMPLETED
            ),
            Booking(
                id = "BK003",
                vehicleNumber = "MH01AB1234",
                spotId = "C-08",
                spotName = "C-08",
                locationName = "Shopping Center",
                locationAddress = "789 Retail Plaza",
                startTime = "02:00 PM",
                endTime = "11:00 PM",
                duration = "9h 0m",
                amount = "₹180.00",
                bookingDate = "Tomorrow",
                status = BookingStatus.PENDING
            )
        )
        
        userBookings.clear()
        userBookings.addAll(sampleBookings)
        showBookingsForStatus(currentTab)
    }

    private fun convertToUIBooking(backendBooking: BackendBooking): Booking {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        // Generate better parking location names based on lot ID
        val parkingLocation = when (backendBooking.lotId) {
            "1", "101" -> "City Mall Parking"
            "2", "102" -> "Airport Terminal"
            "3", "103" -> "Shopping Center"
            "4", "104" -> "Business District"
            "5", "105" -> "Metro Station"
            else -> "Parking Lot ${backendBooking.lotId}"
        }
        
        // Generate better spot names
        val spotName = when {
            backendBooking.spotId?.toIntOrNull() != null -> {
                val spotNum = backendBooking.spotId.toInt()
                when {
                    spotNum <= 20 -> "A-${spotNum}"
                    spotNum <= 40 -> "B-${spotNum - 20}"
                    spotNum <= 60 -> "C-${spotNum - 40}"
                    else -> "D-${spotNum - 60}"
                }
            }
            else -> backendBooking.spotId ?: "Unknown"
        }
        
        return Booking(
            id = backendBooking.id ?: "Unknown",
            vehicleNumber = backendBooking.vehicleNumber ?: "Vehicle not specified",
            spotId = backendBooking.spotId ?: "Unknown", // Keep original spot ID
            spotName = spotName, // Use the formatted spot name
            locationName = parkingLocation, // Use the formatted location name
            locationAddress = "Location for ${parkingLocation}", // Enhanced address
            startTime = if (backendBooking.checkInTime != null) timeFormat.format(backendBooking.checkInTime) else "Not started",
            endTime = if (backendBooking.checkOutTime != null) timeFormat.format(backendBooking.checkOutTime) else "TBD",
            duration = calculateDuration(backendBooking.checkInTime, backendBooking.checkOutTime),
            amount = "₹${String.format("%.2f", backendBooking.amount)}",
            bookingDate = if (backendBooking.createdAt != null) dateFormat.format(backendBooking.createdAt) else "Unknown",
            status = mapBackendStatus(backendBooking.status)
        )
    }

    private fun calculateDuration(startTime: Date?, endTime: Date?): String {
        if (startTime == null || endTime == null) return "N/A"
        
        val durationMs = endTime.time - startTime.time
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        
        return "${hours}h ${minutes}m"
    }

    private fun mapBackendStatus(backendStatus: String?): BookingStatus {
        return when (backendStatus?.lowercase()) {
            "active", "confirmed" -> BookingStatus.ACTIVE
            "pending", "created" -> BookingStatus.PENDING
            "completed", "finished", "cancelled" -> BookingStatus.COMPLETED
            else -> BookingStatus.PENDING
        }
    }

    private fun showBookingDetails(booking: Booking) {
        // Create and show bottom sheet with booking details
        val bottomSheet = BookingDetailsBottomSheetSimple.newInstance(booking)
        bottomSheet.show(parentFragmentManager, "BookingDetailsBottomSheet")
    }

    override fun scrollToTop() {
        try {
            binding.rvBookings.smoothScrollToPosition(0)
        } catch (e: Exception) {
            // Handle any exceptions
        }
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }
}
