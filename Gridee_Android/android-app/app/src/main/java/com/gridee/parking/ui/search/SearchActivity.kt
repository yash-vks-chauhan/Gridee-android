package com.gridee.parking.ui.search

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var isClosing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.sharedElementsUseOverlay = false
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureSharedElementTransition()
        setupInsets()
        setupInteractions()
        playEntranceAnimations()
        focusInput()
    }

    private fun configureSharedElementTransition() {
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())

        val containerColor = Color.TRANSPARENT

        val enterTransform = MaterialContainerTransform().apply {
            drawingViewId = binding.searchRoot.id
            duration = 420
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(containerColor)
            isElevationShadowEnabled = false
        }

        val returnTransform = MaterialContainerTransform().apply {
            drawingViewId = binding.searchRoot.id
            duration = 320
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(containerColor)
            isElevationShadowEnabled = false
        }

        window.sharedElementEnterTransition = enterTransform
        window.sharedElementReturnTransition = returnTransform
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.searchRoot) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val topPadding = systemBars.top + resources.getDimensionPixelSize(R.dimen.margin_large)
            binding.searchCard.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = topPadding
            }

            binding.searchContentScroll.updatePadding(
                binding.searchContentScroll.paddingLeft,
                binding.searchContentScroll.paddingTop,
                binding.searchContentScroll.paddingRight,
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.margin_large)
            )

            insets
        }
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { closeWithAnimation() }
        binding.searchScrim.setOnClickListener { closeWithAnimation() }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            binding.btnClear.isVisible = !text.isNullOrEmpty()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.searchCard.setOnClickListener {
            focusInput()
        }
    }

    private fun playEntranceAnimations() {
        binding.searchScrim.animate()
            .alpha(1f)
            .setDuration(220)
            .setStartDelay(100)
            .start()

        binding.searchContentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(320)
            .setStartDelay(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun focusInput() {
        binding.etSearch.requestFocus()
        binding.etSearch.postDelayed({
            showKeyboard()
        }, 250)
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        hideKeyboard()

        binding.searchContentContainer.animate()
            .alpha(0f)
            .translationY(16f)
            .setDuration(180)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction { finishAfterTransition() }
            .start()

        binding.searchScrim.animate()
            .alpha(0f)
            .setDuration(180)
            .start()
    }

    override fun onBackPressed() {
        closeWithAnimation()
    }
}
