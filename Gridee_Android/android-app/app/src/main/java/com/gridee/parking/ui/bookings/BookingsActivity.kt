package com.gridee.parking.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.ui.adapters.BookingStatus
import com.gridee.parking.data.model.Booking as BackendBooking
import com.gridee.parking.databinding.ActivityBookingsBinding
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation
import com.gridee.parking.ui.components.BookingDetailsBottomSheetSimple
import java.text.SimpleDateFormat
import java.util.*

class BookingsActivity : BaseActivityWithBottomNav<ActivityBookingsBinding>() {

    private lateinit var viewModel: BookingsViewModel
    private lateinit var bookingAdapter: BookingAdapter
    private var allBookings = listOf<Booking>()
    private var currentFilter = "active"

    override fun getViewBinding(): ActivityBookingsBinding {
        return ActivityBookingsBinding.inflate(layoutInflater)
    }

    override fun getCurrentTab(): Int {
        return CustomBottomNavigation.TAB_BOOKINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[BookingsViewModel::class.java]
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Set initial tab selection
        selectFilter("active")
        
        loadBookings()
    }

    override fun setupUI() {
        // Setup scroll behavior for RecyclerView
        setupScrollBehaviorForView(binding.rvBookings)
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter { booking ->
            showBookingDetails(booking)
        }
        
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@BookingsActivity)
            adapter = bookingAdapter
        }
    }

    private fun setupObservers() {
        viewModel.bookings.observe(this) { backendBookings ->
            println("BookingsActivity: Observer called with ${backendBookings.size} bookings")
            allBookings = backendBookings.map { convertToUIBooking(it) }
            updateFilterCounts()
            filterBookings(currentFilter)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {        
        // FAB click listener - parking lot selection
        binding.fabBookParking.setOnClickListener {
            // Navigate to parking lot selection first
            val intent = Intent(this, com.gridee.parking.ui.booking.ParkingLotSelectionActivity::class.java)
            startActivity(intent)
        }

        // Filter tab click listeners (removed All and Cancelled tabs)
        binding.tabActive.setOnClickListener {
            selectFilter("active")
        }

        binding.tabPending.setOnClickListener {
            selectFilter("pending")
        }

        binding.tabCompleted.setOnClickListener {
            selectFilter("completed")
        }
    }

    private fun loadBookings() {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        
        if (userId != null && isLoggedIn) {
            viewModel.loadUserBookings()
        } else {
            showToast("Please log in to view your bookings")
            showEmptyState()
        }
    }

    private fun showBookingsList() {
        binding.rvBookings.visibility = View.VISIBLE
        binding.llEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.rvBookings.visibility = View.GONE
        binding.llEmptyState.visibility = View.VISIBLE
    }

    private fun showBookingDetails(booking: Booking) {
        // Create and show bottom sheet with booking details
        val bottomSheet = BookingDetailsBottomSheetSimple.newInstance(booking)
        bottomSheet.show(supportFragmentManager, "BookingDetailsBottomSheet")
    }

    private fun updateFilterCounts() {
        val activeCount = allBookings.count { it.status == BookingStatus.ACTIVE }
        val pendingCount = allBookings.count { it.status == BookingStatus.PENDING }
        val completedCount = allBookings.count { it.status == BookingStatus.COMPLETED }
        val cancelledCount = 0 // No cancelled status in current enum
        val totalCount = allBookings.size

        binding.tvTabActive.text = "Active($activeCount)"
        binding.tvTabPending.text = "Pending($pendingCount)"
        binding.tvTabCompleted.text = "Completed($completedCount)"
    }

    private fun selectFilter(filter: String) {
        currentFilter = filter
        
        // Reset all tab colors
        resetTabColors()
        
        // Highlight selected tab with modern styling
        when (filter) {
            "active" -> {
                binding.tabActive.setCardBackgroundColor(getColor(android.R.color.black))
                binding.tvTabActive.setTextColor(getColor(android.R.color.white))
            }
            "pending" -> {
                binding.tabPending.setCardBackgroundColor(getColor(android.R.color.black))
                binding.tvTabPending.setTextColor(getColor(android.R.color.white))
            }
            "completed" -> {
                binding.tabCompleted.setCardBackgroundColor(getColor(android.R.color.black))
                binding.tvTabCompleted.setTextColor(getColor(android.R.color.white))
            }
        }
        
        filterBookings(filter)
    }

    private fun resetTabColors() {
        val inactiveColor = getColor(R.color.light_gray)
        val inactiveTextColor = getColor(R.color.darker_gray)
        
        binding.tabActive.setCardBackgroundColor(inactiveColor)
        binding.tvTabActive.setTextColor(inactiveTextColor)
        
        binding.tabPending.setCardBackgroundColor(inactiveColor)
        binding.tvTabPending.setTextColor(inactiveTextColor)
        
        binding.tabCompleted.setCardBackgroundColor(inactiveColor)
        binding.tvTabCompleted.setTextColor(inactiveTextColor)
    }

    private fun filterBookings(filter: String) {
        val filteredBookings = when (filter) {
            "active" -> allBookings.filter { it.status == BookingStatus.ACTIVE }
            "pending" -> allBookings.filter { it.status == BookingStatus.PENDING }
            "completed" -> allBookings.filter { it.status == BookingStatus.COMPLETED }
            else -> allBookings.filter { it.status == BookingStatus.ACTIVE }
        }

        if (filteredBookings.isNotEmpty()) {
            showBookingsList()
            bookingAdapter.updateBookings(filteredBookings)
        } else {
            showEmptyState()
        }
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
            backendBooking.spotId.toIntOrNull() != null -> {
                val spotNum = backendBooking.spotId.toInt()
                when {
                    spotNum <= 20 -> "A-${spotNum}"
                    spotNum <= 40 -> "B-${spotNum - 20}"
                    spotNum <= 60 -> "C-${spotNum - 40}"
                    else -> "D-${spotNum - 60}"
                }
            }
            else -> backendBooking.spotId
        }
        
        return Booking(
            id = backendBooking.id ?: "Unknown",
            vehicleNumber = backendBooking.vehicleNumber ?: "Vehicle not specified",
            spotId = backendBooking.spotId,
            spotName = spotName,
            locationName = parkingLocation,
            locationAddress = "Location for ${parkingLocation}",
            startTime = if (backendBooking.checkInTime != null) timeFormat.format(backendBooking.checkInTime) else "Not started",
            endTime = if (backendBooking.checkOutTime != null) timeFormat.format(backendBooking.checkOutTime) else "TBD",
            duration = calculateDuration(backendBooking.checkInTime, backendBooking.checkOutTime),
            amount = "₹${String.format("%.2f", backendBooking.amount)}",
            bookingDate = if (backendBooking.createdAt != null) dateFormat.format(backendBooking.createdAt) else "Unknown",
            status = mapBackendStatus(backendBooking.status)
        )
    }

    private fun calculateDuration(checkIn: Date?, checkOut: Date?): String {
        if (checkIn == null) return "Not started"
        if (checkOut == null) return "Ongoing"
        
        val duration = checkOut.time - checkIn.time
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        
        return "${hours}h ${minutes}m"
    }

    private fun mapBackendStatus(status: String): BookingStatus {
        return when (status.uppercase()) {
            "ACTIVE" -> BookingStatus.ACTIVE
            "PENDING" -> BookingStatus.PENDING
            "COMPLETED" -> BookingStatus.COMPLETED
            else -> BookingStatus.PENDING
        }
    }
}
