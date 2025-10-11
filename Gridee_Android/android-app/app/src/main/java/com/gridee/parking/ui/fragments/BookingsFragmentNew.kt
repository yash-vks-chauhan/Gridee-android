package com.gridee.parking.ui.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.animation.doOnEnd
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
import com.gridee.parking.ui.components.EnhancedSegmentedControlGestureHandler
import com.gridee.parking.databinding.BottomSheetBookingOverviewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingsFragmentNew : BaseTabFragment<FragmentBookingsNewBinding>() {

    private lateinit var bookingsAdapter: BookingsAdapter
    private var userBookings = mutableListOf<BackendBooking>()
    private var currentTab = BookingStatus.ACTIVE
    private lateinit var gestureHandler: EnhancedSegmentedControlGestureHandler
    
    // Cache for parking lot and spot names
    private val parkingLotCache = mutableMapOf<String, String>() // lotId -> name
    private val parkingSpotCache = mutableMapOf<String, String>() // spotId -> name
    private var isCacheLoaded = false

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
        loadUserBookings() // Use real API instead of sample data
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

    private fun updateSegmentTabs(selectedStatus: BookingStatus) {
        when (selectedStatus) {
            BookingStatus.ACTIVE -> {
                binding.segmentedControlContainer.textActive.setTextColor(resources.getColor(R.color.bulky_glass_text_selected, null))
                binding.segmentedControlContainer.textPending.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textCompleted.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
            }
            BookingStatus.PENDING -> {
                binding.segmentedControlContainer.textActive.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textPending.setTextColor(resources.getColor(R.color.bulky_glass_text_selected, null))
                binding.segmentedControlContainer.textCompleted.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
            }
            BookingStatus.COMPLETED -> {
                binding.segmentedControlContainer.textActive.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textPending.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textCompleted.setTextColor(resources.getColor(R.color.bulky_glass_text_selected, null))
            }
            else -> {
                // Handle other cases (CONFIRMED, CANCELLED, EXPIRED)
                binding.segmentedControlContainer.textActive.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textPending.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
                binding.segmentedControlContainer.textCompleted.setTextColor(resources.getColor(R.color.bulky_glass_text_unselected, null))
            }
        }

        currentTab = selectedStatus
    }

    private fun animateTextColor(textView: TextView, isSelected: Boolean) {
        val fromColor = textView.currentTextColor
        val toColor = if (isSelected) {
            ValueAnimator.ofArgb(fromColor, resources.getColor(R.color.white, null))
        } else {
            ValueAnimator.ofArgb(fromColor, resources.getColor(R.color.bulky_glass_text_unselected, null))
        }
        toColor.duration = 240
        toColor.addUpdateListener { animation ->
            textView.setTextColor(animation.animatedValue as Int)
        }
        toColor.start()
    }

    private fun animateTextScale(textView: TextView, isSelected: Boolean) {
        ObjectAnimator.ofFloat(textView, "scaleX", if (isSelected) 1.05f else 1.0f).apply {
            duration = 180
            start()
        }
        ObjectAnimator.ofFloat(textView, "scaleY", if (isSelected) 1.05f else 1.0f).apply {
            duration = 180
            start()
        }
    }

    private fun animateTextAlpha(textView: TextView, isSelected: Boolean) {
        ObjectAnimator.ofFloat(textView, "alpha", if (isSelected) 1.0f else 0.7f).apply {
            duration = 160
            start()
        }
        ObjectAnimator.ofFloat(textView, "alpha", if (isSelected) 1.0f else 0.7f).apply {
            duration = 160
            start()
        }
    }

    private fun filterBookingsByStatus(status: BookingStatus) {
        android.util.Log.d("BookingsFragment", "filterBookingsByStatus: status=$status, total bookings=${userBookings.size}")
        val statusString = when (status) {
            BookingStatus.ACTIVE -> "active"
            BookingStatus.PENDING -> "pending"
            BookingStatus.COMPLETED -> "completed"
        }
        val filteredBookings: List<BackendBooking> = userBookings.filter { it.status.lowercase() == statusString }
        android.util.Log.d("BookingsFragment", "Filtered bookings count: ${filteredBookings.size}")
        
        // Convert to UI bookings before updating adapter
        val uiBookings = filteredBookings.map { convertToBooking(it) }
        updateAdapterWithBookings(uiBookings)

        // Update visibility
        binding.rvBookings.visibility = if (filteredBookings.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmptyState.visibility = if (filteredBookings.isEmpty()) View.VISIBLE else View.GONE
        android.util.Log.d("BookingsFragment", "RecyclerView visibility: ${binding.rvBookings.visibility}, EmptyState visibility: ${binding.layoutEmptyState.visibility}")
    }

    private fun handleSegmentSelection(index: Int) {
        android.util.Log.d("BookingsFragment", "handleSegmentSelection called with index: $index")
        val newStatus = when (index) {
            0 -> BookingStatus.ACTIVE
            1 -> BookingStatus.PENDING
            2 -> BookingStatus.COMPLETED
            else -> BookingStatus.ACTIVE
        }
        
        updateSegmentTextColors(newStatus)

        android.util.Log.d("BookingsFragment", "Switching from ${currentTab} to ${newStatus}")
        if (newStatus != currentTab) {
            showToast("Switched to ${getSegmentTitle(newStatus)}")
            currentTab = newStatus
            showBookingsForStatus(newStatus) // Use the correct method that handles empty states
        }
    }

    private fun loadBookingsFromAPI() {
        android.util.Log.d("BookingsFragment", "loadBookingsFromAPI called")
        // TODO: Implement API loading when backend methods are available
        // For now, load sample data to demonstrate the enhanced segmented control
        // Load sample data for demonstration
        loadSampleBookings()
        filterBookingsByStatus(currentTab)
    }

    private fun generateSampleBookings(): List<Booking> {
        return listOf(
            Booking(
                id = "1",
                locationName = "Main Parking Lot",
                spotName = "A1",
                vehicleNumber = "ABC123",
                amount = "₹50.00",
                status = BookingStatus.ACTIVE,
                spotId = "spot_1",
                locationAddress = "123 Main St",
                startTime = "10:00 AM",
                endTime = "12:00 PM",
                duration = "2h 0m",
                bookingDate = "Today"
            ),
            Booking(
                id = "2", 
                locationName = "Side Parking",
                spotName = "B2",
                vehicleNumber = "XYZ789",
                amount = "₹30.00",
                status = BookingStatus.PENDING,
                spotId = "spot_2",
                locationAddress = "456 Side St",
                startTime = "2:00 PM",
                endTime = "5:00 PM",
                duration = "3h 0m",
                bookingDate = "Tomorrow"
            ),
            Booking(
                id = "3",
                locationName = "Back Parking",
                spotName = "C3",
                vehicleNumber = "PQR456",
                amount = "₹25.00",
                status = BookingStatus.COMPLETED,
                spotId = "spot_3",
                locationAddress = "789 Back St",
                startTime = "9:00 AM",
                endTime = "10:00 AM",
                duration = "1h 0m",
                bookingDate = "Yesterday"
            )
        )
    }

        private fun mapBackendBookingToUI(backendBooking: BackendBooking): Booking {
        // TODO: Implement proper mapping when all backend properties are available
        return Booking(
            id = backendBooking.id ?: "unknown",
            locationName = "Sample Location",
            spotName = backendBooking.spotId,
            vehicleNumber = backendBooking.vehicleNumber ?: "N/A",
            amount = "₹${backendBooking.amount}",
            status = when (backendBooking.status) {
                "active" -> BookingStatus.ACTIVE
                "pending" -> BookingStatus.PENDING
                "completed" -> BookingStatus.COMPLETED
                else -> BookingStatus.PENDING
            },
            spotId = backendBooking.spotId,
            locationAddress = "Address TBD",
            startTime = "TBD",
            endTime = "TBD",
            duration = "TBD",
            bookingDate = "TBD"
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
        // Wait for views to be laid out before setting up gesture handler
        binding.segmentedControlContainer.segmentContainer.post {
            setupEnhancedGestureHandler()
            initializeSegmentedControlState()
        }
    }
    
    /**
     * Setup the enhanced gesture handler with tap and drag support
     */
    private fun setupEnhancedGestureHandler() {
        val segments = listOf(
            binding.segmentedControlContainer.segmentActive,
            binding.segmentedControlContainer.segmentPending,
            binding.segmentedControlContainer.segmentCompleted
        )
        
        gestureHandler = EnhancedSegmentedControlGestureHandler(
            context = requireContext(),
            containerView = binding.segmentedControlContainer.segmentContainer,
            indicatorView = binding.segmentedControlContainer.segmentIndicator,
            segments = segments,
            onSelectionChanged = { index ->
                handleSegmentSelection(index)
            },
            onAccessibilityAnnouncement = { message ->
                announceForAccessibility(message)
            }
        )
        
        // Set initial selection to active (index 0) to match layout and currentTab
        gestureHandler.setSelectedIndex(0, animated = false)
    }
    
    /**
     * Initialize segmented control visual state
     */
    private fun initializeSegmentedControlState() {
        val indicatorMargin = resources.displayMetrics.density * 4f
        val indicator = binding.segmentedControlContainer.segmentIndicator
        val selectedSegment = when (currentTab) {
            BookingStatus.ACTIVE -> binding.segmentedControlContainer.segmentActive
            BookingStatus.PENDING -> binding.segmentedControlContainer.segmentPending
            BookingStatus.COMPLETED -> binding.segmentedControlContainer.segmentCompleted
        }

        if (selectedSegment.width == 0) {
            indicator.post { initializeSegmentedControlState() }
            return
        }

        val targetWidth = (selectedSegment.width - indicatorMargin * 2).coerceAtLeast(0f)
        val layoutParams = indicator.layoutParams
        val targetWidthInt = targetWidth.toInt()
        if (layoutParams.width != targetWidthInt) {
            layoutParams.width = targetWidthInt
            indicator.layoutParams = layoutParams
        }

        indicator.x = selectedSegment.x + indicatorMargin
        updateSegmentTextColors(currentTab)
    }
    
    /**
     * Handle segment selection from gesture handler
     */
    override fun onDestroyView() {
        if (::gestureHandler.isInitialized) {
            gestureHandler.cleanup()
        }
        super.onDestroyView()
    }

    private fun updateSegmentTextColors(selectedStatus: BookingStatus) {
        // Enhanced cross-fade animation for text color transitions (140ms as specified)
        val fadeDuration = 140L
        
        // Reset all to unselected color with fade animation
        animateTextColor(binding.segmentedControlContainer.textActive, resources.getColor(R.color.bulky_glass_text_unselected, null), fadeDuration)
        animateTextColor(binding.segmentedControlContainer.textPending, resources.getColor(R.color.bulky_glass_text_unselected, null), fadeDuration)
        animateTextColor(binding.segmentedControlContainer.textCompleted, resources.getColor(R.color.bulky_glass_text_unselected, null), fadeDuration)

        // Set selected to primary color with fade animation
        when (selectedStatus) {
            BookingStatus.ACTIVE -> {
                animateTextColor(binding.segmentedControlContainer.textActive, resources.getColor(R.color.bulky_glass_text_selected, null), fadeDuration)
                // Optional subtle scale effect for selected text
                animateTextScale(binding.segmentedControlContainer.textActive, 1.02f, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textPending, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textCompleted, fadeDuration)
            }
            BookingStatus.PENDING -> {
                animateTextColor(binding.segmentedControlContainer.textPending, resources.getColor(R.color.bulky_glass_text_selected, null), fadeDuration)
                animateTextScale(binding.segmentedControlContainer.textPending, 1.02f, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textActive, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textCompleted, fadeDuration)
            }
            BookingStatus.COMPLETED -> {
                animateTextColor(binding.segmentedControlContainer.textCompleted, resources.getColor(R.color.bulky_glass_text_selected, null), fadeDuration)
                animateTextScale(binding.segmentedControlContainer.textCompleted, 1.02f, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textActive, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textPending, fadeDuration)
            }
            else -> {
                // Handle other booking statuses (CONFIRMED, CANCELLED, EXPIRED)
                resetTextScale(binding.segmentedControlContainer.textActive, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textPending, fadeDuration)
                resetTextScale(binding.segmentedControlContainer.textCompleted, fadeDuration)
            }
        }
    }

    private fun animateTextColor(textView: TextView, targetColor: Int, duration: Long) {
        val currentColor = textView.currentTextColor
        val colorAnimator = ValueAnimator.ofArgb(currentColor, targetColor).apply {
            this.duration = duration
            addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
        }
        colorAnimator.start()
    }

    private fun animateTextScale(textView: TextView, targetScale: Float, duration: Long) {
        ObjectAnimator.ofFloat(textView, "scaleX", targetScale).apply {
            this.duration = duration
            start()
        }
        ObjectAnimator.ofFloat(textView, "scaleY", targetScale).apply {
            this.duration = duration
            start()
        }
    }

    private fun resetTextScale(textView: TextView, duration: Long) {
        ObjectAnimator.ofFloat(textView, "scaleX", 1.0f).apply {
            this.duration = duration
            start()
        }
        ObjectAnimator.ofFloat(textView, "scaleY", 1.0f).apply {
            this.duration = duration
            start()
        }
    }

    private fun showBookingsForStatus(status: BookingStatus) {
        val statusString = when (status) {
            BookingStatus.ACTIVE -> "active"
            BookingStatus.PENDING -> "pending"
            BookingStatus.COMPLETED -> "completed"
        }
        val filteredBookings = userBookings.filter { it.status.lowercase() == statusString }
        
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
            val uiBookings = filteredBookings.map { convertToBooking(it) }
            updateAdapterWithBookings(uiBookings)
        }
    }

    private fun updateAdapterWithBookings(bookings: List<Booking>) {
        android.util.Log.d("BookingsFragment", "updateAdapterWithBookings called with ${bookings.size} bookings")
        bookingsAdapter.updateBookings(bookings)
        
        // Show bookings count in UI for now
        if (bookings.isNotEmpty()) {
            showToast("Found ${bookings.size} bookings")
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
                // Load parking lots and spots cache first
                if (!isCacheLoaded) {
                    loadParkingDataCache()
                }
                
                // Get user bookings from backend using real user ID
                val response = ApiClient.apiService.getUserBookings(userId)
                
                if (response.isSuccessful) {
                    val backendBookings = response.body() ?: emptyList()
                    
                    // Convert backend bookings to UI bookings
                    userBookings.clear()
                    userBookings.addAll(backendBookings)
                    
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
    
    private suspend fun loadParkingDataCache() {
        try {
            // Load parking lots
            val lotsResponse = ApiClient.apiService.getParkingLots()
            if (lotsResponse.isSuccessful) {
                lotsResponse.body()?.forEach { lot ->
                    parkingLotCache[lot.id] = lot.name
                    android.util.Log.d("BookingsFragment", "Cached lot: ${lot.id} -> ${lot.name}")
                    
                    // Also try to load spots for this lot
                    try {
                        val spotsForLot = ApiClient.apiService.getParkingSpotsByLot(lot.id)
                        if (spotsForLot.isSuccessful) {
                            spotsForLot.body()?.forEach { spot ->
                                val spotName = spot.name ?: spot.zoneName ?: "Spot ${spot.id}"
                                parkingSpotCache[spot.id] = spotName
                                android.util.Log.d("BookingsFragment", "Cached spot from lot: ${spot.id} -> ${spotName}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BookingsFragment", "Error loading spots for lot ${lot.id}: ${e.message}")
                    }
                }
            }
            
            // Also load all parking spots directly
            val spotsResponse = ApiClient.apiService.getParkingSpots()
            if (spotsResponse.isSuccessful) {
                spotsResponse.body()?.forEach { spot ->
                    val spotName = spot.name ?: spot.zoneName ?: "Spot ${spot.id}"
                    parkingSpotCache[spot.id] = spotName
                    android.util.Log.d("BookingsFragment", "Cached spot: ${spot.id} -> ${spotName}")
                }
            }
            
            isCacheLoaded = true
            android.util.Log.d("BookingsFragment", "Cache loaded: ${parkingLotCache.size} lots, ${parkingSpotCache.size} spots")
        } catch (e: Exception) {
            android.util.Log.e("BookingsFragment", "Error loading parking data cache: ${e.message}")
            // Continue without cache - will use fallback names
        }
    }

    private fun loadSampleBookings() {
        // NOTE: This is fallback sample data shown only when:
        // 1. User is not logged in, OR
        // 2. API call fails due to network/server errors
        // In normal operation, real user bookings from the backend are displayed
        
        // Populate cache with sample location and spot names
        if (!isCacheLoaded) {
            parkingLotCache["Downtown Mall Parking"] = "Phoenix Mall"
            parkingLotCache["Office Complex"] = "Tech Park Business Center"
            parkingLotCache["Shopping Center"] = "City Square Mall"
            
            parkingSpotCache["A-12"] = "North Wing - A12"
            parkingSpotCache["B-05"] = "South Wing - B05"
            parkingSpotCache["C-08"] = "East Wing - C08"
            
            isCacheLoaded = true
        }
        
        // Create sample date/time data
        val calendar = Calendar.getInstance()
        val today = calendar.time
        
        // Create active booking (started 1 hour ago, ends in 1 hour)
        calendar.add(Calendar.HOUR, -1)
        val activeStartTime = calendar.time
        calendar.add(Calendar.HOUR, 2)
        val activeEndTime = calendar.time
        
        // Create completed booking (yesterday)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        val completedStartTime = calendar.time
        calendar.add(Calendar.HOUR, 2)
        val completedEndTime = calendar.time
        val completedCreatedAt = completedStartTime
        
        // Create pending booking (tomorrow)
        calendar.time = today
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 14)
        calendar.set(Calendar.MINUTE, 0)
        val pendingStartTime = calendar.time
        calendar.add(Calendar.HOUR, 3)
        val pendingEndTime = calendar.time
        val pendingCreatedAt = today
        
        // Create sample bookings for demonstration using Booking model
        val sampleBookings = listOf(
            BackendBooking(
                id = "BK001",
                userId = "user123",
                lotId = "Downtown Mall Parking",
                spotId = "A-12",
                status = "active",
                amount = 150.0,
                vehicleNumber = "MH01AB1234",
                checkInTime = activeStartTime,
                checkOutTime = activeEndTime,
                createdAt = today
            ),
            BackendBooking(
                id = "BK002",
                userId = "user123",
                lotId = "Office Complex",
                spotId = "B-05",
                status = "completed",
                amount = 200.0,
                vehicleNumber = "MH01AB1234",
                checkInTime = completedStartTime,
                checkOutTime = completedEndTime,
                createdAt = completedCreatedAt
            ),
            BackendBooking(
                id = "BK003",
                userId = "user123",
                lotId = "Shopping Center",
                spotId = "C-08",
                status = "pending",
                amount = 180.0,
                vehicleNumber = "MH01AB1234",
                checkInTime = pendingStartTime,
                checkOutTime = pendingEndTime,
                createdAt = pendingCreatedAt
            )
        )
        
        userBookings.clear()
        userBookings.addAll(sampleBookings)
        showBookingsForStatus(currentTab)
    }

    private fun convertToBooking(backendBooking: BackendBooking): Booking {
        // Get parking location name from cache, fallback to generated name
        val parkingLocation = parkingLotCache[backendBooking.lotId] ?: when (backendBooking.lotId) {
            "1", "101" -> "City Mall Parking"
            "2", "102" -> "Airport Terminal"
            "3", "103" -> "Shopping Center"
            "4", "104" -> "Business District"
            "5", "105" -> "Metro Station"
            else -> "Parking Lot ${backendBooking.lotId}"
        }
        
        // Get spot name from cache with debug logging
        val spotId = backendBooking.spotId ?: "Unknown"
        android.util.Log.d("BookingsFragment", "Looking up spot: $spotId in cache (${parkingSpotCache.size} entries)")
        
        val spotName = parkingSpotCache[spotId]?.also { 
            android.util.Log.d("BookingsFragment", "Found spot name: $it for ID: $spotId")
        } ?: run {
            android.util.Log.d("BookingsFragment", "Spot $spotId not found in cache, using fallback")
            // Fallback to generated name
            when {
                spotId.toIntOrNull() != null -> {
                    val spotNum = spotId.toInt()
                    when {
                        spotNum <= 20 -> "A-${spotNum}"
                        spotNum <= 40 -> "B-${spotNum - 20}"
                        spotNum <= 60 -> "C-${spotNum - 40}"
                        else -> "D-${spotNum - 60}"
                    }
                }
                else -> spotId
            }
        }
        
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
            vehicleNumber = backendBooking.vehicleNumber ?: "Vehicle not specified",
            amount = "₹${String.format("%.2f", backendBooking.amount)}",
            status = mapBackendStatus(backendBooking.status),
            spotId = backendBooking.spotId ?: "Unknown",
            locationAddress = "Address TBD",
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            bookingDate = bookingDate
        )
    }

    private fun mapBackendStatus(backendStatus: String?): BookingStatus {
        return when (backendStatus?.lowercase()) {
            "active" -> BookingStatus.ACTIVE
            "pending", "created" -> BookingStatus.PENDING
            "completed", "finished", "cancelled", "confirmed", "expired" -> BookingStatus.COMPLETED
            else -> BookingStatus.PENDING
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = BottomSheetBookingOverviewBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

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

        sheetBinding.buttonCloseSheet.setOnClickListener { dialog.dismiss() }
        sheetBinding.buttonClose.setOnClickListener { dialog.dismiss() }
        sheetBinding.buttonCancelBooking.setOnClickListener {
            showToast("Cancel booking action coming soon")
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
        }
        dialog.behavior.isDraggable = true
        dialog.window?.setDimAmount(0.55f)
        dialog.show()
    }

    private fun getStringFormattedTime(date: String, time: String): String {
        return if (date == "TBD" || time == "TBD") {
            "TBD"
        } else {
            "$date · $time"
        }
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    private fun announceForAccessibility(message: String) {
        binding.root.announceForAccessibility(message)
    }
}
