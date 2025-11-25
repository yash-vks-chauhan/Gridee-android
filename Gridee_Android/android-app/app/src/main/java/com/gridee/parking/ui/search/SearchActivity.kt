package com.gridee.parking.ui.search

import android.content.Context
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
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var isClosing = false
    private var allowCloseActions = false

    companion object {
        const val EXTRA_INITIAL_QUERY = "extra_initial_query"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        runCatching { window.sharedElementsUseOverlay = false }
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupInteractions()
        applyInitialQuery()
        playEntranceAnimations()
        focusInput()
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
        binding.btnBack.setOnClickListener { handleCloseRequest() }
        binding.searchScrim.setOnClickListener { handleCloseRequest() }
        binding.searchScrim.isEnabled = false

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

    private fun applyInitialQuery() {
        val initialQuery = intent.getStringExtra(EXTRA_INITIAL_QUERY)
        if (!initialQuery.isNullOrBlank()) {
            binding.etSearch.setText(initialQuery)
            binding.etSearch.setSelection(initialQuery.length)
        }
    }

    private fun playEntranceAnimations() {
        allowCloseActions = false
        binding.searchScrim.alpha = 0f
        binding.searchContentContainer.alpha = 0f
        val offset = resources.getDimension(R.dimen.margin_medium)
        binding.searchContentContainer.translationY = offset

        // Smoother, more professional entrance animation
        binding.searchScrim.animate()
            .alpha(1f)
            .setDuration(250)
            .setStartDelay(0)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                binding.searchScrim.isEnabled = true
            }
            .start()

        binding.searchContentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(150)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
            .withEndAction {
                allowCloseActions = true
            }
            .start()
    }

    private fun focusInput() {
        binding.etSearch.requestFocus()
        binding.etSearch.postDelayed({
            showKeyboard()
        }, 200)
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

    private fun handleCloseRequest() {
        if (!allowCloseActions) return
        closeWithAnimation()
    }

    private fun closeWithAnimation() {
        if (isClosing) return
        isClosing = true
        hideKeyboard()

        // Smooth exit animation
        binding.searchContentContainer.animate()
            .alpha(0f)
            .translationY(12f)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .start()

        binding.searchScrim.animate()
            .alpha(0f)
            .setDuration(200)
            .setStartDelay(50)
            .withEndAction { finishAfterTransition() }
            .start()
    }

    override fun onBackPressed() {
        handleCloseRequest()
    }
}
