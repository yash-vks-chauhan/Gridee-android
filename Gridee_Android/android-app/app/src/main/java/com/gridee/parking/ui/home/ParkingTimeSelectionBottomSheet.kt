package com.gridee.parking.ui.home

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.databinding.BottomSheetParkingTimeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ParkingTimeSelectionBottomSheet : BottomSheetDialogFragment() {

    var onTimesConfirmed: ((Date, Date) -> Unit)? = null

    private var _binding: BottomSheetParkingTimeBinding? = null
    private val binding get() = _binding!!

    private val checkInCalendar = Calendar.getInstance()
    private val checkOutCalendar = Calendar.getInstance()

    private val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        val startMillis = arguments?.getLong(ARG_CHECK_IN_MILLIS, 0L) ?: 0L
        val endMillis = arguments?.getLong(ARG_CHECK_OUT_MILLIS, 0L) ?: 0L
        val now = System.currentTimeMillis()
        checkInCalendar.timeInMillis = if (startMillis > 0) startMillis else now
        val defaultEnd = if (endMillis > 0) endMillis else now + DEFAULT_DURATION_MILLIS
        checkOutCalendar.timeInMillis = defaultEnd
        ensureMinimumDuration()
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
        _binding = BottomSheetParkingTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardCheckIn.setOnClickListener { showDateTimePicker(true) }
        binding.cardCheckOut.setOnClickListener { showDateTimePicker(false) }
        binding.btnApplyTimer.setOnClickListener { applySelection() }
        updateSummaryViews()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun applySelection() {
        val diff = checkOutCalendar.timeInMillis - checkInCalendar.timeInMillis
        if (diff < MIN_DURATION_MILLIS) {
            Toast.makeText(
                requireContext(),
                getString(R.string.home_parking_time_invalid, MIN_DURATION_MINUTES),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        onTimesConfirmed?.invoke(checkInCalendar.time, checkOutCalendar.time)
        dismissAllowingStateLoss()
    }

    private fun showDateTimePicker(isCheckIn: Boolean) {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        val contentView = layoutInflater.inflate(R.layout.dialog_datetime_picker, null)
        dialog.setContentView(contentView)

        val titleView = contentView.findViewById<TextView>(R.id.tvDateTimeTitle)
        val previewDateView = contentView.findViewById<TextView>(R.id.tvPreviewDate)
        val previewTimeView = contentView.findViewById<TextView>(R.id.tvPreviewTime)
        val datePicker = contentView.findViewById<DatePicker>(R.id.customDatePicker)
        val timePicker = contentView.findViewById<TimePicker>(R.id.customTimePicker)
        val btnApply = contentView.findViewById<View>(R.id.btnApplyPicker)
        val btnDismiss = contentView.findViewById<View>(R.id.btnDismissPicker)

        titleView?.text = if (isCheckIn) {
            getString(R.string.datetime_picker_start_title)
        } else {
            getString(R.string.datetime_picker_end_title)
        }

        val workingCalendar = (if (isCheckIn) checkInCalendar else checkOutCalendar).clone() as Calendar

        datePicker?.init(
            workingCalendar.get(Calendar.YEAR),
            workingCalendar.get(Calendar.MONTH),
            workingCalendar.get(Calendar.DAY_OF_MONTH)
        ) { view, year, monthOfYear, dayOfMonth ->
            workingCalendar.set(Calendar.YEAR, year)
            workingCalendar.set(Calendar.MONTH, monthOfYear)
            workingCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updatePickerPreview(workingCalendar, previewDateView, previewTimeView)
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        timePicker?.apply {
            setIs24HourView(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = workingCalendar.get(Calendar.HOUR_OF_DAY)
                minute = workingCalendar.get(Calendar.MINUTE)
            } else {
                @Suppress("DEPRECATION")
                currentHour = workingCalendar.get(Calendar.HOUR_OF_DAY)
                @Suppress("DEPRECATION")
                currentMinute = workingCalendar.get(Calendar.MINUTE)
            }
            setOnTimeChangedListener { view, hourOfDay, minute ->
                workingCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                workingCalendar.set(Calendar.MINUTE, minute)
                updatePickerPreview(workingCalendar, previewDateView, previewTimeView)
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }

        updatePickerPreview(workingCalendar, previewDateView, previewTimeView)

        btnDismiss?.setOnClickListener { dialog.dismiss() }
        btnApply?.setOnClickListener {
            if (isCheckIn) {
                checkInCalendar.timeInMillis = workingCalendar.timeInMillis
                ensureMinimumDuration()
            } else {
                checkOutCalendar.timeInMillis = workingCalendar.timeInMillis
                ensureMinimumDuration()
            }
            updateSummaryViews()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateSummaryViews() {
        binding.tvCheckInValue.text = formatDateTime(checkInCalendar)
        binding.tvCheckOutValue.text = formatDateTime(checkOutCalendar)
        binding.tvTimeRange.text = formatRange()
        binding.tvTimerHint.isVisible = true
    }

    private fun formatDateTime(calendar: Calendar): String {
        val date = dateFormatter.format(calendar.time)
        val time = timeFormatter.format(calendar.time)
        return "$date • $time"
    }

    private fun formatRange(): String {
        val startDate = dateFormatter.format(checkInCalendar.time)
        val endDate = dateFormatter.format(checkOutCalendar.time)
        val startTime = timeFormatter.format(checkInCalendar.time)
        val endTime = timeFormatter.format(checkOutCalendar.time)
        return if (startDate == endDate) {
            "$startDate • $startTime – $endTime"
        } else {
            "$startDate • $startTime → $endDate • $endTime"
        }
    }

    private fun ensureMinimumDuration() {
        val diff = checkOutCalendar.timeInMillis - checkInCalendar.timeInMillis
        if (diff < MIN_DURATION_MILLIS) {
            checkOutCalendar.timeInMillis = checkInCalendar.timeInMillis + MIN_DURATION_MILLIS
        }
    }

    private fun updatePickerPreview(calendar: Calendar, dateView: TextView?, timeView: TextView?) {
        val dateText = dateFormatter.format(calendar.time)
        val timeText = timeFormatter.format(calendar.time)
        dateView?.text = dateText
        timeView?.text = timeText
    }

    companion object {
        private const val ARG_CHECK_IN_MILLIS = "arg_check_in_millis"
        private const val ARG_CHECK_OUT_MILLIS = "arg_check_out_millis"
        private const val MIN_DURATION_MINUTES = 60
        private val MIN_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(MIN_DURATION_MINUTES.toLong())
        private val DEFAULT_DURATION_MILLIS = TimeUnit.HOURS.toMillis(2)

        fun newInstance(
            checkInMillis: Long?,
            checkOutMillis: Long?
        ): ParkingTimeSelectionBottomSheet {
            return ParkingTimeSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    checkInMillis?.takeIf { it > 0 }?.let { putLong(ARG_CHECK_IN_MILLIS, it) }
                    checkOutMillis?.takeIf { it > 0 }?.let { putLong(ARG_CHECK_OUT_MILLIS, it) }
                }
            }
        }
    }
}
