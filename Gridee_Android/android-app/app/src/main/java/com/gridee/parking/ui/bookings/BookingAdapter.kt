package com.gridee.parking.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.ui.adapters.Booking
import com.gridee.parking.databinding.ItemBookingBinding
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private var bookings = listOf<Booking>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                // Set parking spot name
                tvParkingSpot.text = booking.spotName
                
                // Set booking date
                tvBookingDate.text = booking.bookingDate
                
                // Set check-in and check-out times
                tvCheckInTime.text = booking.startTime
                tvCheckOutTime.text = booking.endTime
                
                // Set amount/price
                tvAmount.text = booking.amount
                
                // Set status (you can customize based on booking.status if available)
                tvStatus.text = "ACTIVE"
            }
        }
    }
}
