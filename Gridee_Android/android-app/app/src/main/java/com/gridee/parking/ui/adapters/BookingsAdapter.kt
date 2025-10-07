package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.databinding.ItemBookingBinding

data class Booking(
    val id: String,
    val vehicleNumber: String,
    val spotId: String,
    val locationName: String,
    val locationAddress: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val amount: String,
    val status: BookingStatus,
    val bookingDate: String
)

enum class BookingStatus {
    ACTIVE,
    PENDING,
    COMPLETED
}

class BookingsAdapter(
    private var bookings: List<Booking>,
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingsAdapter.BookingViewHolder>() {

    inner class BookingViewHolder(private val binding: ItemBookingBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(booking: Booking) {
            binding.apply {
                tvBookingId.text = "Booking #${booking.id}"
                tvVehicleNumber.text = booking.vehicleNumber
                tvSpotId.text = "Spot: ${booking.spotId}"
                tvBookingDate.text = booking.bookingDate
                tvTimeSlot.text = "${booking.startTime} - ${booking.endTime}"
                tvAmount.text = booking.amount

                // Set status with appropriate styling
                when (booking.status) {
                    BookingStatus.ACTIVE -> {
                        tvStatus.text = "ACTIVE"
                        tvStatus.setBackgroundResource(R.drawable.status_active_bg)
                        tvStatus.setTextColor(binding.root.context.getColor(android.R.color.white))
                    }
                    BookingStatus.PENDING -> {
                        tvStatus.text = "PENDING"
                        tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                        tvStatus.setTextColor(binding.root.context.getColor(android.R.color.white))
                    }
                    BookingStatus.COMPLETED -> {
                        tvStatus.text = "COMPLETED"
                        tvStatus.setBackgroundResource(R.drawable.status_completed_bg)
                        tvStatus.setTextColor(binding.root.context.getColor(android.R.color.white))
                    }
                }

                cardBooking.setOnClickListener {
                    onBookingClick(booking)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }
}
