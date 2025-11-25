package com.gridee.parking.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.databinding.BottomSheetParkingSummaryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingSelectionSummaryBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        dialog.window?.attributes?.windowAnimations = com.gridee.parking.R.style.BottomSheetAnimation
        return dialog
    }

    var onApplySelection: (() -> Unit)? = null
    var onModifyTimer: (() -> Unit)? = null
    var onModifyCategory: (() -> Unit)? = null

    private var _binding: BottomSheetParkingSummaryBinding? = null
    private val binding get() = _binding!!

    private val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetParkingSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categoryTitle = arguments?.getString(ARG_CATEGORY_TITLE).orEmpty()
        val categorySubtitle = arguments?.getString(ARG_CATEGORY_SUBTITLE).orEmpty()
        val startMillis = arguments?.getLong(ARG_CHECK_IN_MILLIS) ?: 0L
        val endMillis = arguments?.getLong(ARG_CHECK_OUT_MILLIS) ?: 0L

        binding.tvSummaryCategoryValue.text = categoryTitle
        binding.tvSummaryCategorySubtitle.text = categorySubtitle
        binding.tvSummaryTimeValue.text = formatRange(startMillis, endMillis)

        binding.btnApplySelection.setOnClickListener {
            onApplySelection?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnModifyTimer.setOnClickListener {
            onModifyTimer?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnModifyCategory.setOnClickListener {
            onModifyCategory?.invoke()
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun formatRange(startMillis: Long, endMillis: Long): String {
        if (startMillis <= 0L || endMillis <= 0L) return ""
        val startDate = Date(startMillis)
        val endDate = Date(endMillis)
        val startDateLabel = dateFormatter.format(startDate)
        val endDateLabel = dateFormatter.format(endDate)
        val startTimeLabel = timeFormatter.format(startDate)
        val endTimeLabel = timeFormatter.format(endDate)
        return if (startDateLabel == endDateLabel) {
            "$startDateLabel • $startTimeLabel – $endTimeLabel"
        } else {
            "$startDateLabel • $startTimeLabel → $endDateLabel • $endTimeLabel"
        }
    }

    companion object {
        private const val ARG_CATEGORY_TITLE = "arg_category_title"
        private const val ARG_CATEGORY_SUBTITLE = "arg_category_subtitle"
        private const val ARG_CHECK_IN_MILLIS = "arg_summary_check_in"
        private const val ARG_CHECK_OUT_MILLIS = "arg_summary_check_out"

        const val TAG = "ParkingSelectionSummary"

        fun newInstance(
            categoryTitle: String,
            categorySubtitle: String,
            checkInMillis: Long,
            checkOutMillis: Long
        ): ParkingSelectionSummaryBottomSheet {
            return ParkingSelectionSummaryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_TITLE, categoryTitle)
                    putString(ARG_CATEGORY_SUBTITLE, categorySubtitle)
                    putLong(ARG_CHECK_IN_MILLIS, checkInMillis)
                    putLong(ARG_CHECK_OUT_MILLIS, checkOutMillis)
                }
            }
        }
    }
}
