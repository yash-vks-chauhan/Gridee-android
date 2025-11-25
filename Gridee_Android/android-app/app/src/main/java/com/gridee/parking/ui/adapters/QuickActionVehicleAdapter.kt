package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.databinding.ItemVehicleQuickActionBinding

class QuickActionVehicleAdapter(
    private val onVehicleClick: (String) -> Unit,
    private val onEditClick: (String) -> Unit
) : RecyclerView.Adapter<QuickActionVehicleAdapter.VehicleViewHolder>() {

    private var vehicles = listOf<String>()

    fun updateVehicles(newVehicles: List<String>) {
        vehicles = newVehicles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleQuickActionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(vehicles[position])
    }

    override fun getItemCount(): Int = vehicles.size

    inner class VehicleViewHolder(
        private val binding: ItemVehicleQuickActionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vehicleNumber: String) {
            binding.tvVehicleNumber.text = vehicleNumber
            // For now, we don't have vehicle names in the simple string list, so we hide it or show a default
            binding.tvVehicleName.text = "Personal Vehicle" 
            
            binding.root.setOnClickListener {
                onVehicleClick(vehicleNumber)
            }
            
            binding.btnEditVehicle.setOnClickListener {
                onEditClick(vehicleNumber)
            }
        }
    }
}
