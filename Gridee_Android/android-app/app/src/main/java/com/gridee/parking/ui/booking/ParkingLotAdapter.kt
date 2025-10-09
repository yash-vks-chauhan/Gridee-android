package com.gridee.parking.ui.booking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.data.model.ParkingLot
import com.gridee.parking.databinding.ItemParkingLotBinding

class ParkingLotAdapter(
    private val onItemClick: (ParkingLot) -> Unit
) : ListAdapter<ParkingLot, ParkingLotAdapter.ParkingLotViewHolder>(ParkingLotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingLotViewHolder {
        val binding = ItemParkingLotBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ParkingLotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParkingLotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ParkingLotViewHolder(
        private val binding: ItemParkingLotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parkingLot: ParkingLot) {
            binding.apply {
                tvLotName.text = parkingLot.name
                tvLotAddress.text = parkingLot.address
                tvAvailability.text = "${parkingLot.availableSpots}/${parkingLot.totalSpots} spots available"
                
                // Set availability color
                if (parkingLot.availableSpots > 0) {
                    tvAvailability.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            itemView.context, 
                            com.gridee.parking.R.color.primary_green
                        )
                    )
                } else {
                    tvAvailability.setTextColor(
                        androidx.core.content.ContextCompat.getColor(
                            itemView.context, 
                            com.gridee.parking.R.color.error
                        )
                    )
                }
                
                // Click listener
                root.setOnClickListener {
                    onItemClick(parkingLot)
                }
            }
        }
    }
}

class ParkingLotDiffCallback : DiffUtil.ItemCallback<ParkingLot>() {
    override fun areItemsTheSame(oldItem: ParkingLot, newItem: ParkingLot): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ParkingLot, newItem: ParkingLot): Boolean {
        return oldItem == newItem
    }
}
