package com.gridee.parking.ui.profile

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.databinding.ActivityVehiclesBinding
import com.gridee.parking.ui.base.BaseActivity

class VehiclesActivity : BaseActivity<ActivityVehiclesBinding>() {

    override fun getViewBinding(): ActivityVehiclesBinding {
        return ActivityVehiclesBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Basic RecyclerView setup to match expected binding members
        binding.rvVehicles.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnAddVehicle.setOnClickListener {
            showToast("Add vehicle clicked")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this as Context, message, Toast.LENGTH_SHORT).show()
    }
}
