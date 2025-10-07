package com.gridee.parking.ui.fragments

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.Booking as BackendBooking
import com.gridee.parking.databinding.FragmentBookingsBinding
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.ui.adapters.BookingStatus
import com.gridee.parking.ui.adapters.BookingsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragment : BaseTabFragment<FragmentBookingsBinding>() {

    private lateinit var bookingsAdapter: BookingsAdapter
    private var currentTab = BookingStatus.ACTIVE
    private var userBookings = mutableListOf<Booking>()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBookingsBinding {
        return FragmentBookingsBinding.inflate(inflater, container, false)
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
        setupTabs()
        loadUserBookings()
        showBookingsForStatus(BookingStatus.ACTIVE)
    }

    private fun setupRecyclerView() {
        bookingsAdapter = BookingsAdapter(emptyList()) { booking ->
            showBookingDetails(booking)
        }
        
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingsAdapter
        }
    }

    private fun loadUserBookings() {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to view bookings")
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getUserBookings(userId)
                if (response.isSuccessful) {
                    val backendBookings = response.body()
                    if (backendBookings != null) {
                        userBookings.clear()
                        userBookings.addAll(backendBookings.map { convertToUIBooking(it) })
                        refreshCurrentTab()
                    }
                } else {
                    showToast("Failed to load bookings")
                }
            } catch (e: Exception) {
                showToast("Error loading bookings: ${e.message}")
            }
        }
    }

    private fun convertToUIBooking(backendBooking: BackendBooking): Booking {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        // Format dates for display
        val checkInTimeStr = backendBooking.checkInTime?.let { 
            dateFormat.format(it) 
        } ?: "TBD"
        
        val checkOutTimeStr = backendBooking.checkOutTime?.let { 
            dateFormat.format(it) 
        } ?: "TBD"
        
        val bookingDateStr = backendBooking.createdAt?.let {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
        } ?: "Unknown"

        val status = when (backendBooking.status.uppercase()) {
            "ACTIVE" -> BookingStatus.ACTIVE
            "PENDING" -> BookingStatus.PENDING
            "COMPLETED" -> BookingStatus.COMPLETED
            "CANCELLED" -> BookingStatus.COMPLETED // Show cancelled as completed
            else -> BookingStatus.PENDING
        }

        // Calculate duration if both times are available
        val duration = if (backendBooking.checkInTime != null && backendBooking.checkOutTime != null) {
            val diffInMillis = backendBooking.checkOutTime!!.time - backendBooking.checkInTime!!.time
            val hours = diffInMillis / (1000 * 60 * 60)
            val minutes = (diffInMillis % (1000 * 60 * 60)) / (1000 * 60)
            "${hours}h ${minutes}m"
        } else {
            "TBD"
        }

        return Booking(
            id = backendBooking.id ?: "Unknown",
            vehicleNumber = backendBooking.vehicleNumber ?: "N/A",
            spotId = backendBooking.spotId,
            locationName = "Parking Lot", // Default since backend doesn't have this
            locationAddress = "Location Address", // Default since backend doesn't have this
            startTime = checkInTimeStr,
            endTime = checkOutTimeStr,
            duration = duration,
            amount = "$${String.format("%.2f", backendBooking.amount)}",
            status = status,
            bookingDate = bookingDateStr
        )
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    private fun refreshCurrentTab() {
        showBookingsForStatus(currentTab)
    }

    private fun showBookingDetails(booking: Booking) {
        // TODO: Navigate to booking details activity or show details dialog
        /*
        val intent = Intent(requireContext(), BookingDetailsActivity::class.java)
        intent.putExtra("booking_id", booking.id)
        startActivity(intent)
        */
        
        showToast("Booking details: ${booking.id}")
    }

    private fun setupTabs() {
        binding.tabActive.setOnClickListener {
            if (currentTab != BookingStatus.ACTIVE) {
                selectTab(BookingStatus.ACTIVE)
            }
        }

        binding.tabPending.setOnClickListener {
            if (currentTab != BookingStatus.PENDING) {
                selectTab(BookingStatus.PENDING)
            }
        }

        binding.tabCompleted.setOnClickListener {
            if (currentTab != BookingStatus.COMPLETED) {
                selectTab(BookingStatus.COMPLETED)
            }
        }
    }

    private fun selectTab(status: BookingStatus) {
        currentTab = status
        
        // Update tab styling
        updateTabAppearance(status)
        
        // Move indicator
        animateIndicator(status)
        
        // Show bookings for selected status
        showBookingsForStatus(status)
        
        // Haptic feedback
        performHapticFeedback()
    }

    private fun updateTabAppearance(selectedStatus: BookingStatus) {
        // Reset all tabs
        binding.tabActive.apply {
            setTextColor(requireContext().getColor(R.color.text_secondary))
            typeface = android.graphics.Typeface.DEFAULT
        }
        binding.tabPending.apply {
            setTextColor(requireContext().getColor(R.color.text_secondary))
            typeface = android.graphics.Typeface.DEFAULT
        }
        binding.tabCompleted.apply {
            setTextColor(requireContext().getColor(R.color.text_secondary))
            typeface = android.graphics.Typeface.DEFAULT
        }

        // Highlight selected tab
        val selectedTab = when (selectedStatus) {
            BookingStatus.ACTIVE -> binding.tabActive
            BookingStatus.PENDING -> binding.tabPending
            BookingStatus.COMPLETED -> binding.tabCompleted
        }
        
        selectedTab.apply {
            setTextColor(requireContext().getColor(R.color.primary_blue))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    private fun animateIndicator(status: BookingStatus) {
        val targetTab = when (status) {
            BookingStatus.ACTIVE -> binding.tabActive
            BookingStatus.PENDING -> binding.tabPending
            BookingStatus.COMPLETED -> binding.tabCompleted
        }

        val targetX = targetTab.x
        val targetWidth = targetTab.width

        // Animate indicator position
        ObjectAnimator.ofFloat(binding.tabIndicator, "x", targetX).apply {
            duration = 200
            start()
        }

        // Animate indicator width
        val layoutParams = binding.tabIndicator.layoutParams
        ObjectAnimator.ofInt(layoutParams.width, targetWidth).apply {
            duration = 200
            addUpdateListener { animation ->
                layoutParams.width = animation.animatedValue as Int
                binding.tabIndicator.layoutParams = layoutParams
            }
            start()
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

    // Function to update booking status (e.g., when payment is completed)
    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to update booking")
            return
        }

        lifecycleScope.launch {
            try {
                // Use confirmBooking for active status, others might need different endpoints
                val response = when (newStatus) {
                    BookingStatus.ACTIVE -> {
                        ApiClient.apiService.confirmBooking(userId, bookingId)
                    }
                    else -> {
                        // For other status updates, we might need to add more endpoints
                        showToast("Status update not supported yet")
                        return@launch
                    }
                }
                
                if (response.isSuccessful) {
                    // Update local booking
                    val bookingIndex = userBookings.indexOfFirst { it.id == bookingId }
                    if (bookingIndex != -1) {
                        val updatedBooking = userBookings[bookingIndex].copy(status = newStatus)
                        userBookings[bookingIndex] = updatedBooking
                        refreshCurrentTab()
                    }
                    showToast("Booking status updated successfully")
                } else {
                    showToast("Failed to update booking status")
                }
            } catch (e: Exception) {
                showToast("Error updating booking: ${e.message}")
            }
        }
    }

    // Function to cancel a booking
    fun cancelBooking(bookingId: String) {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to cancel booking")
            return
        }

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.cancelBooking(userId, bookingId)
                
                if (response.isSuccessful) {
                    // Remove from local list
                    userBookings.removeIf { it.id == bookingId }
                    refreshCurrentTab()
                    showToast("Booking cancelled successfully")
                } else {
                    showToast("Failed to cancel booking")
                }
            } catch (e: Exception) {
                showToast("Error cancelling booking: ${e.message}")
            }
        }
    }

    companion object {
        fun newInstance(): BookingsFragment {
            return BookingsFragment()
        }
    }
}
