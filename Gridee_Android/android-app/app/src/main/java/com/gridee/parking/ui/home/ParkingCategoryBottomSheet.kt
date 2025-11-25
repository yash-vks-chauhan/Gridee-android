package com.gridee.parking.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.databinding.BottomSheetParkingCategoriesBinding

class ParkingCategoryBottomSheet : BottomSheetDialogFragment() {

    var onCategorySelected: ((ParkingSpotCategory) -> Unit)? = null

    private var _binding: BottomSheetParkingCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ParkingCategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        dialog.window?.attributes?.windowAnimations = R.style.BottomSheetAnimation
        dialog.setCanceledOnTouchOutside(false)
        dialog.behavior.isDraggable = false
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetParkingCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = ParkingCategoryAdapter { category ->
            onCategorySelected?.invoke(category)
            dismissAllowingStateLoss()
        }
        binding.rvParkingCategories.adapter = adapter

        val selectedName = arguments?.getString(ARG_SELECTED_CATEGORY)
        val selectedCategory = ParkingSpotCategory.values()
            .firstOrNull { it.name == selectedName }
            ?: ParkingSpotCategory.default()

        adapter.submitCategories(ParkingSpotCategory.asList(), selectedCategory)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_SELECTED_CATEGORY = "arg_selected_category"
        const val TAG = "ParkingCategoryBottomSheet"

        fun newInstance(selected: ParkingSpotCategory?): ParkingCategoryBottomSheet {
            return ParkingCategoryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_CATEGORY, selected?.name)
                }
            }
        }
    }
}
