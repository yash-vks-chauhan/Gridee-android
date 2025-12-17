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

class ParkingSpotHomeAdapter :
    ListAdapter<ParkingSpot, ParkingSpotHomeAdapter.ParkingSpotViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingSpotViewHolder {
        val binding = ItemParkingSpotHomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ParkingSpotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParkingSpotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParkingSpotViewHolder(
        private val binding: ItemParkingSpotHomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(spot: ParkingSpot) {
            println("DEBUG ParkingSpotHomeAdapter.bind: id=${spot.id}, name=${spot.name}, available=${spot.available}, capacity=${spot.capacity}")
            
            val displayName = spot.name?.takeIf { it.isNotBlank() }
                ?: spot.zoneName?.takeIf { it.isNotBlank() }
                ?: spot.spotCode?.takeIf { it.isNotBlank() }
                ?: spot.id

            binding.tvSpotName.text = displayName

            val resolvedStatus = spot.status.ifBlank {
                if (spot.available > 0) "available" else "unavailable"
            }
            binding.tvSpotStatus.text = resolvedStatus.uppercase()

            val capacity = if (spot.capacity > 0) spot.capacity else spot.available
            val idLabel = spot.spotCode?.takeIf { it.isNotBlank() } ?: spot.id
            binding.tvSpotMeta.text = if (capacity > 0) {
                "ID: $idLabel • Capacity: $capacity"
            } else {
                "ID: $idLabel"
            }

            binding.tvSpotAvailability.text = if (capacity > 0) {
                "${spot.available}/$capacity available"
            } else {
                "${spot.available} available"
            }

            val context = binding.root.context
            val isAvailable = spot.available > 0
            val accentColor = if (isAvailable) R.color.primary_green else R.color.error
            binding.tvSpotStatus.setTextColor(ContextCompat.getColor(context, accentColor))
            binding.tvSpotAvailability.setTextColor(ContextCompat.getColor(context, accentColor))
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

