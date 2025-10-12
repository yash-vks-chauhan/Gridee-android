package com.gridee.parking.ui.activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
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
import com.gridee.parking.ui.bottomsheet.PaymentMethodFilterBottomSheet
import com.gridee.parking.ui.bottomsheet.PaymentMethodOption
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var transactionsAdapter: WalletTransactionsAdapter
    private var allTransactions = mutableListOf<Transaction>()
    private var paymentMethodFilter = PaymentMethodOption.ALL
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
        setupFilters()
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

    private fun setupFilters() {
        updatePaymentMethodFilterLabel()
        binding.btnFilterPaymentMethod.setOnClickListener {
            val sheet = PaymentMethodFilterBottomSheet(paymentMethodFilter) { selection ->
                paymentMethodFilter = selection
                applyFilters()
            }
            sheet.show(supportFragmentManager, PaymentMethodFilterBottomSheet.TAG)
        }
    }

    private fun setupHeaderParallax() {
        val headerContainer = binding.headerContainer
        val titleView = binding.tvHeaderTitle
        val countView = binding.tvTransactionCount
        val backButton = binding.btnBack
        val filterRow = binding.filterScrollView
        val filterButtons = listOf(
            binding.btnFilterPaymentMethod,
            binding.btnFilterDate,
            binding.btnFilterAmount
        )
        val headerCard = binding.cardHeader

        val initialPaddingTop = headerContainer.paddingTop
        val initialPaddingBottom = headerContainer.paddingBottom
        val paddingReduction = resources.getDimensionPixelSize(R.dimen.transaction_header_padding_reduction)
        val titleHorizontalShift = resources.getDimensionPixelSize(R.dimen.transaction_header_title_horizontal_shift).toFloat()
        val countAdditionalSpacing = resources.getDimensionPixelSize(R.dimen.transaction_header_count_spacing).toFloat()
        val filterCollapsedSpacing = resources.getDimensionPixelSize(R.dimen.transaction_header_filter_collapsed_spacing).toFloat()

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
            val targetTitleX = backButton.x + backButton.width + titleHorizontalShift
            titleShiftY = targetTitleY - titleView.y
            titleShiftX = targetTitleX - titleView.x

            val targetCountY = targetTitleY + titleView.height + countAdditionalSpacing
            countShiftY = targetCountY - countView.y
            countShiftX = targetTitleX - countView.x

            val targetFilterY = backButton.y + backButton.height + filterCollapsedSpacing
            filterShiftY = targetFilterY - filterRow.y

            titleView.translationZ = 2f
            countView.translationZ = 1f
        }

        binding.rvAllTransactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) return
                headerCollapseOffset = (headerCollapseOffset + dy).coerceIn(0, headerCollapseRange)
                val progress = headerCollapseOffset.toFloat() / headerCollapseRange

                val targetScale = (1f - 0.18f * progress).coerceAtLeast(0.8f)
                titleView.scaleX = targetScale
                titleView.scaleY = targetScale
                titleView.translationY = titleShiftY * progress
                titleView.translationX = titleShiftX * progress

                countView.translationY = countShiftY * progress
                countView.translationX = countShiftX * progress
                countView.alpha = 1f - progress

                filterRow.translationY = filterShiftY * progress

                val shouldUseCollapsedTint = progress > 0.01f
                if (shouldUseCollapsedTint != filtersStickyApplied) {
                    val tint = if (shouldUseCollapsedTint) collapsedTint else expandedTint
                    filterButtons.forEach { it.backgroundTintList = tint }
                    filtersStickyApplied = shouldUseCollapsedTint
                }

                val paddingOffset = (paddingReduction * progress).toInt().coerceAtMost(paddingReduction)
                headerContainer.updatePadding(
                    top = (initialPaddingTop - paddingOffset).coerceAtLeast(0),
                    bottom = (initialPaddingBottom - paddingOffset).coerceAtLeast(0)
                )

                headerCard.cardElevation = 0f
            }
        })
    }

    private fun applyFilters() {
        val filteredTransactions = getFilteredTransactions()
        updatePaymentMethodFilterLabel()
        renderTransactions(filteredTransactions)
    }

    private fun getFilteredTransactions(): List<Transaction> {
        return when (paymentMethodFilter) {
            PaymentMethodOption.ALL -> allTransactions
            else -> allTransactions.filter { transaction ->
                transaction.paymentMethod?.equals(paymentMethodFilter.name, ignoreCase = true) == true
            }
        }
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

    private fun updatePaymentMethodFilterLabel() {
        val buttonText = when (paymentMethodFilter) {
            PaymentMethodOption.ALL -> getString(R.string.payment_filter_button_default)
            else -> getString(paymentMethodFilter.labelRes)
        }
        binding.btnFilterPaymentMethod.text = buttonText
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
        val detectedPaymentMethod = detectPaymentMethod(rawDescription)
        
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
            paymentMethod = detectedPaymentMethod?.name
        )
    }

    private fun detectPaymentMethod(description: String): PaymentMethodOption? {
        if (description.isBlank()) return null
        val normalized = description.lowercase(Locale.getDefault())
        return when {
            normalized.contains("upi") -> PaymentMethodOption.UPI
            normalized.contains("card") || normalized.contains("debit") || normalized.contains("credit") -> PaymentMethodOption.CARD
            else -> null
        }
    }

    private fun showEmptyState() {
        allTransactions.clear()
        renderTransactions(emptyList())
        updatePaymentMethodFilterLabel()
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
}
