package com.gridee.parking.ui.activities

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.databinding.ActivityTransactionHistoryBinding
import com.gridee.parking.ui.adapters.Transaction
import com.gridee.parking.ui.adapters.TransactionType
import com.gridee.parking.ui.adapters.WalletTransactionGrouping
import com.gridee.parking.ui.adapters.WalletTransactionsAdapter

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var transactionsAdapter: WalletTransactionsAdapter
    private var allTransactions = mutableListOf<Transaction>()
    private var headerCollapseOffset = 0
    private val headerCollapseRange by lazy {
        resources.getDimensionPixelSize(R.dimen.transaction_header_collapse_range)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, R.color.background_primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        
        setupToolbar()
        setupRecyclerView()
        setupFilterButtons()
        loadAllTransactions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
            title = ""
            setDisplayShowTitleEnabled(false)
        }
        binding.toolbar.navigationIcon = null

        // Hide the toolbar as we have custom header
        binding.toolbar.visibility = View.GONE

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        transactionsAdapter = WalletTransactionsAdapter(emptyList())
        
        binding.rvAllTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
            adapter = transactionsAdapter
        }

        setupHeaderParallax()
    }

    private fun setupFilterButtons() {
        // Enable smooth scrolling for filter buttons
        binding.filterScrollView.isHorizontalScrollBarEnabled = false
        binding.filterScrollView.isSmoothScrollingEnabled = true
        
        binding.btnFilterDate.setOnClickListener {
            showDateFilterModal()
        }
        
        binding.btnFilterAmount.setOnClickListener {
            showAmountFilterModal()
        }
        
        binding.btnFilterPaymentMethod.setOnClickListener {
            showPaymentFilterModal()
        }
    }



    private fun setupHeaderParallax() {
        val headerContainer = binding.headerContainer
        val titleView = binding.tvHeaderTitle
        val countView = binding.tvTransactionCount
        val backButton = binding.btnBack
        val filterRow = binding.filterScrollView
        val filterButtons = listOf(
            binding.btnFilterDate,
            binding.btnFilterAmount,
            binding.btnFilterPaymentMethod
        )
        val headerCard = binding.cardHeader

        val initialPaddingTop = headerContainer.paddingTop
        val initialPaddingBottom = headerContainer.paddingBottom
        val paddingReduction = resources.getDimensionPixelSize(R.dimen.transaction_header_padding_reduction)
        val titleHorizontalShift = resources.getDimensionPixelSize(R.dimen.transaction_header_title_horizontal_shift).toFloat()
        val collapsedFilterSpacing = resources.getDimension(R.dimen.transaction_header_filter_collapsed_spacing)

        var titleShiftX = 0f
        var titleShiftY = 0f
        var countShiftY = 0f
        var countShiftX = 0f
        var filterShiftY = 0f
        var filtersStickyApplied = false
        val expandedTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.background_primary))
        val collapsedTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.background_primary))
        filterButtons.forEach { it.backgroundTintList = expandedTint }

        headerContainer.doOnLayout {
            val targetTitleY = backButton.y + (backButton.height - titleView.height) / 2f

            val availableWidth = headerContainer.width - headerContainer.paddingStart - headerContainer.paddingEnd
            val centeredTitleX = headerContainer.paddingStart + (availableWidth - titleView.width) / 2f
            val targetTitleX = centeredTitleX + titleHorizontalShift
            titleShiftY = targetTitleY - titleView.y
            titleShiftX = targetTitleX - titleView.x

            // Hide count view when collapsed (move it way up and out of sight)
            countShiftY = -countView.height * 3f
            countShiftX = 0f

            val targetFilterY = backButton.y + backButton.height + collapsedFilterSpacing
            filterShiftY = targetFilterY - filterRow.y

            titleView.pivotX = titleView.width / 2f
            titleView.pivotY = titleView.height.toFloat()

            titleView.translationZ = 10f // Higher elevation to appear above other elements
            countView.translationZ = 1f
            backButton.translationZ = 20f // Highest elevation to stay on top
            filterRow.translationZ = 5f // Medium elevation for filters
            headerCard.cardElevation = 0f
        }

        binding.rvAllTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) return
                headerCollapseOffset = (headerCollapseOffset + dy).coerceIn(0, headerCollapseRange)
                val progress = headerCollapseOffset.toFloat() / headerCollapseRange

                // Keep back button fixed - no translation or scaling
                backButton.translationY = 0f
                backButton.translationX = 0f
                backButton.scaleX = 1f
                backButton.scaleY = 1f

                // Animate title from bottom-left to right side of back button (same vertical level)
                val targetScale = (1f - 0.25f * progress).coerceAtLeast(0.75f)
                titleView.scaleX = targetScale
                titleView.scaleY = targetScale
                val paddingOffset = (paddingReduction * progress).coerceAtMost(paddingReduction.toFloat())
                titleView.translationY = titleShiftY * progress + paddingOffset
                titleView.translationX = titleShiftX * progress

                // Fade out and move count view
                countView.translationY = countShiftY * progress
                countView.translationX = countShiftX * progress
                countView.alpha = (1f - progress * 1.5f).coerceAtLeast(0f)

                // Move filters up to their sticky position below back button
                filterRow.translationY = filterShiftY * progress + paddingOffset

                // Update filter button styling for sticky state
                val shouldUseCollapsedTint = progress > 0.3f
                if (shouldUseCollapsedTint != filtersStickyApplied) {
                    val tint = if (shouldUseCollapsedTint) collapsedTint else expandedTint
                    filterButtons.forEach { it.backgroundTintList = tint }
                    filtersStickyApplied = shouldUseCollapsedTint
                }

                // Reduce padding as header collapses
                val paddingOffsetInt = (paddingOffset).toInt().coerceAtMost(paddingReduction)
                headerContainer.updatePadding(
                    top = (initialPaddingTop - paddingOffsetInt).coerceAtLeast(6), // Minimum padding
                    bottom = (initialPaddingBottom - paddingOffsetInt).coerceAtLeast(6) // Minimum padding
                )

                headerCard.setCardBackgroundColor(ContextCompat.getColor(this@TransactionHistoryActivity, R.color.background_primary))
            }
        })
    }

    private fun applyFilters() {
        renderTransactions(allTransactions)
    }

    private fun renderTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binding.rvAllTransactions.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvTransactionCount.text = "0 transactions"
            transactionsAdapter.updateItems(emptyList())
        } else {
            binding.rvAllTransactions.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvTransactionCount.text = "${transactions.size} transactions"
            val groupedItems = WalletTransactionGrouping.buildGroupedItems(transactions)
            transactionsAdapter.updateItems(groupedItems)
        }
    }



    private fun loadAllTransactions() {
        val userId = getUserId()
        if (userId == null) {
            Toast.makeText(this, "Please login to view transaction history", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getWalletTransactions(userId)
                if (response.isSuccessful) {
                    val transactions = response.body()
                    if (transactions != null && transactions.isNotEmpty()) {
                        allTransactions.clear()
                        allTransactions.addAll(transactions.mapNotNull { transaction ->
                            try {
                                convertToUITransaction(transaction)
                            } catch (e: Exception) {
                                null // Skip invalid transactions
                            }
                        })
                        
                        // Sort by timestamp (newest first)
                        allTransactions.sortByDescending { it.timestamp }
                        
                        applyFilters()
                        Toast.makeText(this@TransactionHistoryActivity, "Loaded ${allTransactions.size} transactions", Toast.LENGTH_SHORT).show()
                    } else {
                        showEmptyState()
                        Toast.makeText(this@TransactionHistoryActivity, "No transactions found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@TransactionHistoryActivity, "Failed to load transactions: ${response.code()}", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TransactionHistoryActivity, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun convertToUITransaction(walletTransaction: WalletTransaction): Transaction {
        val id = walletTransaction.id?.trim()
        val timestamp = walletTransaction.timestamp?.trim()
        
        if (id.isNullOrBlank() || timestamp.isNullOrBlank()) {
            throw IllegalArgumentException("Transaction missing required fields")
        }
        
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
        
        val parsedTimestamp = try {
            var parsedDate: Date? = null
            for (format in dateFormats) {
                try {
                    parsedDate = format.parse(timestamp)
                    break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            parsedDate ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val transactionType = walletTransaction.type?.uppercase()?.trim() ?: "CREDIT"
        val rawDescription = walletTransaction.description?.trim().orEmpty()
        val description = if (rawDescription.isBlank()) "Transaction" else rawDescription
        
        return Transaction(
            id = id,
            type = when (transactionType) {
                "CREDIT", "TOP_UP", "TOPUP" -> TransactionType.TOP_UP
                "DEBIT", "PAYMENT" -> TransactionType.PARKING_PAYMENT
                "REFUND" -> TransactionType.REFUND
                "BONUS" -> TransactionType.BONUS
                else -> TransactionType.TOP_UP
            },
            amount = walletTransaction.amount ?: 0.0,
            description = description,
            timestamp = parsedTimestamp,
            balanceAfter = walletTransaction.balanceAfter ?: 0.0,
            paymentMethod = null
        )
    }



    private fun showEmptyState() {
        allTransactions.clear()
        renderTransactions(emptyList())
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.rvAllTransactions.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.GONE
        } else if (binding.layoutEmptyState.visibility != View.VISIBLE) {
            binding.rvAllTransactions.visibility = View.VISIBLE
        }
    }

    private fun getUserId(): String? {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    private fun showDateFilterModal() {
        val modal = binding.dateFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.dateModalCloseButton
        
        // Get screen height for consistent positioning
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.8f // Start 80% of screen height below
        
        // Set initial position off screen
        modal.translationY = slideDistance
        modal.alpha = 0f
        closeButton.translationY = slideDistance - 44f // Position close button above modal
        closeButton.alpha = 0f
        overlay.alpha = 0f
        
        // Make modal, close button and overlay visible
        modal.visibility = View.VISIBLE
        closeButton.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        
        // Animate overlay fade in with blur effect
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f).apply {
            duration = 350
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Animate modal slide up with spring animation
        val slideUpAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate close button slide up with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, -44f).apply {
            spring = SpringForce(-44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate modal fade in
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f) // Slightly less overshoot for smoother feel
        }
        
        // Animate close button fade in
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f)
        }
        
        // Start animations
        overlayAnimator.start()
        slideUpAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
        
        // Set up close modal functionality
        overlay.setOnClickListener {
            hideDateFilterModal()
        }
        closeButton.setOnClickListener {
            hideDateFilterModal()
        }
    }
    
    private fun hideDateFilterModal() {
        val modal = binding.dateFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.dateModalCloseButton
        
        // Animate overlay fade out with slower, smoother timing and blur effect
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f).apply {
            duration = 800 // Much slower fade out for smooth effect
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Get screen height for proper slide down distance
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.9f // Slide further down for smoother exit
        
        // Animate modal slide down with smoother spring animation
        val slideDownAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, slideDistance).apply {
            spring = SpringForce(slideDistance).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY // Smoother bounce
                stiffness = SpringForce.STIFFNESS_LOW // Slower, more graceful movement
            }
        }
        
        // Animate close button slide down with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, slideDistance - 44f).apply {
            spring = SpringForce(slideDistance - 44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
        }
        
        // Animate modal fade out with smoother curve
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 1f, 0f).apply {
            duration = 400 // Longer duration for smoother fade
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50 // Small delay to let slide start first
        }
        
        // Animate close button fade out
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 1f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50
        }
        
        // Hide modal, close button and overlay after animations complete
        slideDownAnimation.addEndListener { _, _, _, _ ->
            modal.visibility = View.GONE
            closeButton.visibility = View.GONE
            overlay.visibility = View.GONE
            // Reset positions for next time
            modal.translationY = slideDistance
            modal.alpha = 0f
            closeButton.translationY = slideDistance - 44f
            closeButton.alpha = 0f
            overlay.alpha = 0f
        }
        
        // Start animations simultaneously for smooth effect
        overlayAnimator.start()
        slideDownAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
    }

    private fun showAmountFilterModal() {
        val modal = binding.amountFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.amountModalCloseButton
        
        // Get screen height for consistent positioning
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.8f // Start 80% of screen height below
        
        // Set initial position off screen
        modal.translationY = slideDistance
        modal.alpha = 0f
        closeButton.translationY = slideDistance - 44f // Position close button above modal
        closeButton.alpha = 0f
        overlay.alpha = 0f
        
        // Make modal, close button and overlay visible
        modal.visibility = View.VISIBLE
        closeButton.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        
        // Animate overlay fade in with blur effect
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f).apply {
            duration = 350
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Animate modal slide up with spring animation
        val slideUpAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate close button slide up with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, -44f).apply {
            spring = SpringForce(-44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate modal fade in
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f) // Slightly less overshoot for smoother feel
        }
        
        // Animate close button fade in
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f)
        }
        
        // Start animations
        overlayAnimator.start()
        slideUpAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
        
        // Set up close modal functionality
        overlay.setOnClickListener {
            hideAmountFilterModal()
        }
        closeButton.setOnClickListener {
            hideAmountFilterModal()
        }
    }
    
    private fun hideAmountFilterModal() {
        val modal = binding.amountFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.amountModalCloseButton
        
        // Animate overlay fade out with faster timing
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Get screen height for proper slide down distance
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.8f // Slide down 80% of screen height
        
        // Animate modal slide down with spring animation - more bouncy and natural
        val slideDownAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, slideDistance).apply {
            spring = SpringForce(slideDistance).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY // More spring bounce
                stiffness = SpringForce.STIFFNESS_MEDIUM // Good responsiveness
            }
        }
        
        // Animate close button slide down with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, slideDistance - 44f).apply {
            spring = SpringForce(slideDistance - 44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
        }
        
        // Animate modal fade out with smoother curve
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 1f, 0f).apply {
            duration = 400 // Longer duration for smoother fade
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50 // Small delay to let slide start first
        }
        
        // Animate close button fade out
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 1f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50
        }
        
        // Hide modal, close button and overlay after animations complete
        slideDownAnimation.addEndListener { _, _, _, _ ->
            modal.visibility = View.GONE
            closeButton.visibility = View.GONE
            overlay.visibility = View.GONE
            // Reset positions for next time
            modal.translationY = slideDistance
            modal.alpha = 0f
            closeButton.translationY = slideDistance - 44f
            closeButton.alpha = 0f
            overlay.alpha = 0f
        }
        
        // Start animations simultaneously for smooth effect
        overlayAnimator.start()
        slideDownAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
    }

    private fun showPaymentFilterModal() {
        val modal = binding.paymentFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.paymentModalCloseButton
        
        // Get screen height for consistent positioning
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.8f // Start 80% of screen height below
        
        // Set initial position off screen
        modal.translationY = slideDistance
        modal.alpha = 0f
        closeButton.translationY = slideDistance - 44f // Position close button above modal
        closeButton.alpha = 0f
        overlay.alpha = 0f
        
        // Make modal, close button and overlay visible
        modal.visibility = View.VISIBLE
        closeButton.visibility = View.VISIBLE
        overlay.visibility = View.VISIBLE
        
        // Animate overlay fade in with blur effect
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f).apply {
            duration = 350
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Animate modal slide up with spring animation
        val slideUpAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate close button slide up with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, -44f).apply {
            spring = SpringForce(-44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        }
        
        // Animate modal fade in
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f) // Slightly less overshoot for smoother feel
        }
        
        // Animate close button fade in
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 0f, 1f).apply {
            duration = 400
            interpolator = OvershootInterpolator(0.6f)
        }
        
        // Start animations
        overlayAnimator.start()
        slideUpAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
        
        // Set up close modal functionality
        overlay.setOnClickListener {
            hidePaymentFilterModal()
        }
        closeButton.setOnClickListener {
            hidePaymentFilterModal()
        }
    }
    
    private fun hidePaymentFilterModal() {
        val modal = binding.paymentFilterModal
        val overlay = binding.modalOverlay
        val closeButton = binding.paymentModalCloseButton
        
        // Animate overlay fade out with faster timing
        val overlayAnimator = ObjectAnimator.ofFloat(overlay, "alpha", 1f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Get screen height for proper slide down distance
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val slideDistance = screenHeight * 0.8f // Slide down 80% of screen height
        
        // Animate modal slide down with spring animation - more bouncy and natural
        val slideDownAnimation = SpringAnimation(modal, SpringAnimation.TRANSLATION_Y, slideDistance).apply {
            spring = SpringForce(slideDistance).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY // More spring bounce
                stiffness = SpringForce.STIFFNESS_MEDIUM // Good responsiveness
            }
        }
        
        // Animate close button slide down with spring animation
        val closeButtonSlideAnimation = SpringAnimation(closeButton, SpringAnimation.TRANSLATION_Y, slideDistance - 44f).apply {
            spring = SpringForce(slideDistance - 44f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
        }
        
        // Animate modal fade out with smoother curve
        val modalAlphaAnimator = ObjectAnimator.ofFloat(modal, "alpha", 1f, 0f).apply {
            duration = 400 // Longer duration for smoother fade
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50 // Small delay to let slide start first
        }
        
        // Animate close button fade out
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(closeButton, "alpha", 1f, 0f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 50
        }
        
        // Hide modal, close button and overlay after animations complete
        slideDownAnimation.addEndListener { _, _, _, _ ->
            modal.visibility = View.GONE
            closeButton.visibility = View.GONE
            overlay.visibility = View.GONE
            // Reset positions for next time
            modal.translationY = slideDistance
            modal.alpha = 0f
            closeButton.translationY = slideDistance - 44f
            closeButton.alpha = 0f
            overlay.alpha = 0f
        }
        
        // Start animations simultaneously for smooth effect
        overlayAnimator.start()
        slideDownAnimation.start()
        closeButtonSlideAnimation.start()
        modalAlphaAnimator.start()
        closeButtonAlphaAnimator.start()
    }
}
