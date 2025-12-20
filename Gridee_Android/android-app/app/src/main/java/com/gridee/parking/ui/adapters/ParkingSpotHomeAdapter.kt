package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.data.model.ParkingSpot
import com.gridee.parking.databinding.ItemParkingSpotHomeBinding

class ParkingSpotHomeAdapter(
    private val onItemClick: (ParkingSpot) -> Unit
) :
    ListAdapter<ParkingSpot, ParkingSpotHomeAdapter.ParkingSpotViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingSpotViewHolder {
        val binding = ItemParkingSpotHomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParkingSpotViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ParkingSpotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParkingSpotViewHolder(
        private val binding: ItemParkingSpotHomeBinding,
        private val onItemClick: (ParkingSpot) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(spot: ParkingSpot) {
            println("DEBUG ParkingSpotHomeAdapter.bind: id=${spot.id}, name=${spot.name}, available=${spot.available}, capacity=${spot.capacity}")
            
            val displayName = spot.name?.takeIf { it.isNotBlank() }
                ?: spot.zoneName?.takeIf { it.isNotBlank() }
                ?: spot.spotCode?.takeIf { it.isNotBlank() }
                ?: spot.id

            binding.tvSpotName.text = displayName

            // Clean minimalistic binding
            binding.tvSpotAvailability.text = "${spot.available}"
            
            // Optional subtitle if needed, or hide if same as name
            binding.tvSpotMeta.text = "Gridee Parking" // Or specific zone name if available
            
            // Hide Status (Constraint dummy)
            // binding.tvSpotStatus.visibility = View.GONE

            binding.root.setOnClickListener { onItemClick(spot) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ParkingSpot>() {
        override fun areItemsTheSame(oldItem: ParkingSpot, newItem: ParkingSpot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ParkingSpot, newItem: ParkingSpot): Boolean {
            return oldItem == newItem
        }
    }
}
