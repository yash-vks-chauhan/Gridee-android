package com.gridee.parking.ui.bookings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.data.model.Booking
import com.gridee.parking.databinding.ActivityBookingsBinding
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation

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
        viewModel.bookings.observe(this) { bookings ->
            println("BookingsActivity: Observer called with ${bookings.size} bookings")
            allBookings = bookings
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
        // TODO: Navigate to booking details screen
        showToast("Booking details - Coming Soon!")
    }

    private fun updateFilterCounts() {
        val activeCount = allBookings.count { it.status.equals("active", ignoreCase = true) }
        val pendingCount = allBookings.count { it.status.equals("pending", ignoreCase = true) }
        val completedCount = allBookings.count { it.status.equals("completed", ignoreCase = true) }
        val cancelledCount = allBookings.count { it.status.equals("cancelled", ignoreCase = true) }
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
            "active" -> allBookings.filter { it.status.equals("active", ignoreCase = true) }
            "pending" -> allBookings.filter { it.status.equals("pending", ignoreCase = true) }
            "completed" -> allBookings.filter { it.status.equals("completed", ignoreCase = true) }
            else -> allBookings.filter { it.status.equals("active", ignoreCase = true) }
        }

        if (filteredBookings.isNotEmpty()) {
            showBookingsList()
            bookingAdapter.updateBookings(filteredBookings)
        } else {
            showEmptyState()
        }
    }
}
