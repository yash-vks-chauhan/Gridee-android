package com.gridee.parking.ui.booking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.gridee.parking.databinding.ActivityBookingConfirmationBinding
import com.gridee.parking.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class BookingConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingConfirmationBinding
    private lateinit var viewModel: BookingConfirmationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BookingConfirmationViewModel::class.java]

        getBookingDataFromIntent()
        setupUI()
        setupClickListeners()
        setupObservers()
    }

    private fun getBookingDataFromIntent() {
        val bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        val transactionId = intent.getStringExtra("TRANSACTION_ID") ?: ""
        
        viewModel.loadBookingDetails(bookingId, transactionId)
    }

    private fun setupUI() {
        binding.tvTitle.text = "Booking Confirmed"
    }

    private fun setupClickListeners() {
        binding.btnDone.setOnClickListener {
            // Navigate back to main activity and show My Bookings
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SHOW_MY_BOOKINGS", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.btnViewBooking.setOnClickListener {
            // Navigate to booking details
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SHOW_MY_BOOKINGS", true)
            intent.putExtra("BOOKING_ID", viewModel.bookingDetails.value?.bookingId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.btnShareReceipt.setOnClickListener {
            shareBookingReceipt()
        }
    }

    private fun setupObservers() {
        viewModel.bookingDetails.observe(this) { details ->
            updateBookingDisplay(details)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
        }
    }

    private fun updateBookingDisplay(details: BookingConfirmationDetails) {
        // Update confirmation details
        binding.tvBookingId.text = "Booking ID: ${details.bookingId}"
        binding.tvTransactionId.text = "Transaction ID: ${details.transactionId}"
        
        // Update parking details
        binding.tvParkingSpotName.text = details.parkingSpotName
        binding.tvParkingAddress.text = details.parkingAddress
        binding.tvSelectedSpot.text = details.selectedSpot ?: "Any available spot"
        
        // Update time details
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        binding.tvParkingDate.text = dateFormat.format(Date(details.startTime))
        binding.tvStartTime.text = timeFormat.format(Date(details.startTime))
        binding.tvEndTime.text = timeFormat.format(Date(details.endTime))
        
        // Calculate and display duration
        val durationHours = (details.endTime - details.startTime) / (1000 * 60 * 60)
        val durationMinutes = ((details.endTime - details.startTime) % (1000 * 60 * 60)) / (1000 * 60)
        
        if (durationHours > 0) {
            binding.tvDuration.text = "${durationHours}h ${durationMinutes}m"
        } else {
            binding.tvDuration.text = "${durationMinutes}m"
        }
        
        // Update payment details
        binding.tvTotalAmount.text = "$${"%.2f".format(details.totalAmount)}"
        binding.tvPaymentMethod.text = details.paymentMethodDisplay
        binding.tvPaymentStatus.text = "Paid"
        
        // Update timestamp
        val timestampFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        binding.tvBookingTime.text = "Booked on ${timestampFormat.format(Date(details.timestamp))}"
    }

    private fun shareBookingReceipt() {
        val details = viewModel.bookingDetails.value ?: return
        
        val shareText = buildString {
            appendLine("🅿️ Parking Booking Confirmed")
            appendLine()
            appendLine("Booking ID: ${details.bookingId}")
            appendLine("Location: ${details.parkingSpotName}")
            appendLine("Address: ${details.parkingAddress}")
            appendLine()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            appendLine("Start: ${dateFormat.format(Date(details.startTime))}")
            appendLine("End: ${dateFormat.format(Date(details.endTime))}")
            appendLine()
            appendLine("Amount Paid: ${"%.2f".format(details.totalAmount)}")
            appendLine("Payment: ${details.paymentMethodDisplay}")
            appendLine()
            appendLine("Powered by Gridee Parking")
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Parking Booking Receipt")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share booking receipt"))
    }
}
