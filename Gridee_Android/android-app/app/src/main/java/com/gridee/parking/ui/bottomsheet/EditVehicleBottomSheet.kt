package com.gridee.parking.ui.bottomsheet

import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gridee.parking.R
import com.gridee.parking.databinding.BottomSheetEditVehicleBinding

class EditVehicleBottomSheet(
    private val vehicleNumber: String,
    private val onSave: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditVehicleBinding? = null
    private val binding get() = _binding!!
    private var dimView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
                behavior.isHideable = true
                
                // Apply spring animation to the bottom sheet
                applySpringAnimation(sheet)
            }
        }
        
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditVehicleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupListeners()
        applyBlurAndOpacityAnimation()
    }

    private fun setupUI() {
        // Pre-fill the current vehicle number
        binding.etVehicleNumber.setText(vehicleNumber)
        binding.etVehicleNumber.setSelection(vehicleNumber.length)
        
        // Don't auto-focus or show keyboard
        // Let the user tap on the input field to bring up the keyboard
        // This allows the modal to slide up smoothly first
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            val newVehicleNumber = binding.etVehicleNumber.text.toString().trim()
            
            if (newVehicleNumber.isEmpty()) {
                binding.tilVehicleNumber.error = "Vehicle number cannot be empty"
                return@setOnClickListener
            }
            
            if (newVehicleNumber == vehicleNumber) {
                dismiss()
                return@setOnClickListener
            }
            
            onSave(newVehicleNumber)
            dismissWithAnimation()
        }
        
        binding.btnCancel.setOnClickListener {
            dismissWithAnimation()
        }
        
        // Clear error when user types
        binding.etVehicleNumber.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tilVehicleNumber.error = null
            }
        }
    }

    private fun applySpringAnimation(view: View) {
        val springAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
            
            // Start from below the screen
            view.translationY = view.height.toFloat()
            start()
        }
    }

    private fun applyBlurAndOpacityAnimation() {
        // Get the dim background view
        dialog?.window?.let { window ->
            val decorView = window.decorView
            val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
            
            // Animate opacity
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Float
                    window.setDimAmount(0.6f * alpha)
                }
                start()
            }
        }
        
        // Animate the bottom sheet content
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun dismissWithAnimation() {
        // Animate the bottom sheet sliding down
        val springAnim = SpringAnimation(binding.root, DynamicAnimation.TRANSLATION_Y, binding.root.height.toFloat()).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            
            addEndListener { _, _, _, _ ->
                dismiss()
            }
            
            start()
        }
        
        // Animate opacity
        dialog?.window?.let { window ->
            ValueAnimator.ofFloat(1f, 0f).apply {
                duration = 250
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Float
                    window.setDimAmount(0.6f * alpha)
                }
                start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditVehicleBottomSheet"
    }
}
