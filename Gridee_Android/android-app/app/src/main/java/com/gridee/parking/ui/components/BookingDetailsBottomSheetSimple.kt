package com.gridee.parking.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.ui.adapters.BookingStatus

class BookingDetailsBottomSheetSimple : BottomSheetDialogFragment() {
    
    private lateinit var booking: Booking
    
    companion object {
        fun newInstance(booking: Booking): BookingDetailsBottomSheetSimple {
            return BookingDetailsBottomSheetSimple().apply {
                arguments = Bundle().apply {
                    putSerializable("booking", booking)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        booking = arguments?.getSerializable("booking") as? Booking 
            ?: throw IllegalArgumentException("Booking is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View? {
        // Temporarily using a simple TextView until the layout issue is resolved
        val textView = TextView(requireContext()).apply {
            text = """
                Booking Details
                
                ID: ${booking.id}
                Location: ${booking.locationName}
                Spot: ${booking.spotName}
                Vehicle: ${booking.vehicleNumber}
                
                Check-in: ${booking.startTime}
                Check-out: ${booking.endTime}
                Duration: ${booking.duration}
                Amount: ${booking.amount}
                
                Status: ${booking.status}
                Date: ${booking.bookingDate}
                
                ${formatBackendData(booking)}
            """.trimIndent()
            
            setPadding(48, 48, 48, 48)
            textSize = 14f
        }
        
        return textView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // For now, we'll just show a simple text view
        // When the layout is fixed, we'll uncomment the proper functionality
    }

    private fun formatBackendData(booking: Booking): String {
        return """
Raw Booking Data:
{
  "id": "${booking.id}",
  "vehicleNumber": "${booking.vehicleNumber}",
  "spotId": "${booking.spotId}",
  "spotName": "${booking.spotName}",
  "locationName": "${booking.locationName}",
  "locationAddress": "${booking.locationAddress}",
  "startTime": "${booking.startTime}",
  "endTime": "${booking.endTime}",
  "duration": "${booking.duration}",
  "amount": "${booking.amount}",
  "status": "${booking.status}",
  "bookingDate": "${booking.bookingDate}"
}
        """.trimIndent()
    }

    private fun extractLocationFromSpotName(spotName: String): String {
        return if (spotName.contains(" at ", ignoreCase = true)) {
            spotName.substringAfter(" at ").trim()
        } else {
            "Parking Location"
        }
    }

    private fun isMotorcycle(vehicleNumber: String): Boolean {
        return vehicleNumber.contains("bike", ignoreCase = true) || 
               vehicleNumber.length <= 6
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Temporarily disabled
        // _binding = null
    }

    // Keep the interface for compatibility
    fun setOnBookingActionListener(listener: (BookingAction, Booking) -> Unit) {
        // Simplified for now
    }
    
    enum class BookingAction {
        CANCEL, EXTEND, SUPPORT, DIRECTIONS
    }
}
