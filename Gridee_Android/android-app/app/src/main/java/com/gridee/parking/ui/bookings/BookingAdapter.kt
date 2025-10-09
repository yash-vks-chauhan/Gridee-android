package com.gridee.parking.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.data.model.Booking
import com.gridee.parking.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings = listOf<Booking>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun updateBookings(newBookings: List<Booking>) {
        println("BookingAdapter: updateBookings called with ${newBookings.size} bookings")
        bookings = newBookings
        for (i in bookings.indices) {
            println("BookingAdapter: Booking $i - ID: ${bookings[i].id}, Status: ${bookings[i].status}")
        }
        notifyDataSetChanged()
        println("BookingAdapter: getItemCount after update: ${getItemCount()}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        println("BookingAdapter: onBindViewHolder called for position $position")
        try {
            val booking = bookings[position]
            println("BookingAdapter: Binding booking at position $position - ID: ${booking.id}, Status: ${booking.status}")
            holder.bind(booking)
            println("BookingAdapter: Successfully bound booking at position $position")
        } catch (e: Exception) {
            println("BookingAdapter: ERROR binding booking at position $position: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int {
        println("BookingAdapter: getItemCount called, returning ${bookings.size}")
        return bookings.size
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            println("BookingAdapter.bind: Starting bind for booking ID: ${booking.id}")
            
            try {
                binding.tvBookingId.text = "Booking #${booking.id?.take(8) ?: "Unknown"}"
                println("BookingAdapter.bind: Set booking ID text")
                
                binding.tvVehicleNumber.text = booking.vehicleNumber ?: "N/A"
                println("BookingAdapter.bind: Set vehicle number: ${booking.vehicleNumber}")
                
                binding.tvSpotId.text = "Spot: ${booking.spotId}"
                println("BookingAdapter.bind: Set spot ID: ${booking.spotId}")
                
                binding.tvAmount.text = "₹${String.format("%.2f", booking.amount)}"
                println("BookingAdapter.bind: Set amount: ${booking.amount}")
                
                // Format dates
                booking.createdAt?.let {
                    binding.tvBookingDate.text = dateFormat.format(it)
                    println("BookingAdapter.bind: Set created date: ${dateFormat.format(it)}")
                } ?: run {
                    binding.tvBookingDate.text = "Date N/A"
                    println("BookingAdapter.bind: Set default date text")
                }
                
                // Check-in and check-out times
                if (booking.checkInTime != null && booking.checkOutTime != null) {
                    binding.tvTimeSlot.text = "${timeFormat.format(booking.checkInTime)} - ${timeFormat.format(booking.checkOutTime)}"
                    println("BookingAdapter.bind: Set time slot: ${timeFormat.format(booking.checkInTime)} - ${timeFormat.format(booking.checkOutTime)}")
                } else {
                    binding.tvTimeSlot.text = "Time slot N/A"
                    println("BookingAdapter.bind: Set default time slot text")
                }
                
                // Status with color coding
                binding.tvStatus.text = booking.status.uppercase()
                println("BookingAdapter.bind: Set status: ${booking.status}")
                
                when (booking.status.lowercase()) {
                    "pending" -> {
                        binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_orange_dark))
                        binding.cardBooking.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
                    }
                    "active" -> {
                        binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                        binding.cardBooking.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
                    }
                    "completed" -> {
                        binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_blue_dark))
                        binding.cardBooking.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
                    }
                    "cancelled" -> {
                        binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                        binding.cardBooking.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
                    }
                    else -> {
                        binding.tvStatus.setTextColor(binding.root.context.getColor(android.R.color.darker_gray))
                        binding.cardBooking.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
                    }
                }
                println("BookingAdapter.bind: Set status colors")
                
                binding.root.setOnClickListener {
                    onBookingClick(booking)
                }
                println("BookingAdapter.bind: Set click listener")
                
                println("BookingAdapter.bind: Successfully completed bind for booking ID: ${booking.id}")
            } catch (e: Exception) {
                println("BookingAdapter.bind: ERROR in bind method for booking ID: ${booking.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
