package com.gridee.parking.ui.fragments

import android.content.Intent
import android.text.TextWatcher
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.databinding.FragmentWalletNewBinding
import com.gridee.parking.databinding.BottomSheetTopUpSimpleBinding
import com.gridee.parking.ui.activities.TransactionHistoryActivity
import com.gridee.parking.ui.adapters.Transaction
import com.gridee.parking.ui.adapters.TransactionType
import com.gridee.parking.ui.adapters.WalletTransactionGrouping
import com.gridee.parking.ui.adapters.WalletTransactionsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WalletFragmentNew : BaseTabFragment<FragmentWalletNewBinding>() {

    private lateinit var transactionsAdapter: WalletTransactionsAdapter
    private var currentBalance = 0.0
    private var userTransactions = mutableListOf<Transaction>()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWalletNewBinding {
        return FragmentWalletNewBinding.inflate(inflater, container, false)
    }

    override fun getScrollableView(): View? {
        return try {
            binding.scrollContent
        } catch (e: IllegalStateException) {
            null
        }
    }

    override fun setupUI() {
        setupRecyclerView()
        setupClickListeners()
        loadWalletData()
    }

    private fun setupRecyclerView() {
        transactionsAdapter = WalletTransactionsAdapter(emptyList())
        
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.tvViewAll.setOnClickListener {
            val intent = Intent(requireContext(), TransactionHistoryActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnAddMoney.setOnClickListener {
            android.util.Log.d("WalletFragmentNew", "Add money button clicked")
            showTopUpDialog()
        }
        
        // Quick Add buttons - direct top-up
        binding.btnQuickAdd10.setOnClickListener {
            processTopUp(50.0)
        }
        
        binding.btnQuickAdd20.setOnClickListener {
            processTopUp(100.0)
        }
        
        binding.btnQuickAdd100.setOnClickListener {
            processTopUp(200.0)
        }
    }

    private fun loadWalletData() {
        binding.progressLoading.visibility = View.VISIBLE
        
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to view wallet")
            binding.progressLoading.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                android.util.Log.d("WalletFragmentNew", "Loading wallet balance for user: $userId")
                loadWalletBalance(userId)
                android.util.Log.d("WalletFragmentNew", "Loading wallet transactions for user: $userId")
                loadWalletTransactions(userId)
            } catch (e: Exception) {
                android.util.Log.e("WalletFragmentNew", "Unexpected error loading wallet data", e)
                showToast("Error loading wallet data: ${e.message}")
            } finally {
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private suspend fun loadWalletBalance(userId: String) {
        try {
            val response = ApiClient.apiService.getWalletDetails(userId)
            android.util.Log.d("WalletFragmentNew", "Wallet balance API response: ${response.code()}")
            if (response.isSuccessful) {
                val walletDetails = response.body()
                currentBalance = walletDetails?.balance ?: 0.0
                updateBalanceDisplay()
            } else {
                showToast("Unable to load wallet balance (${response.code()})")
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletFragmentNew", "Error loading wallet balance", e)
            showToast("Error loading balance: ${e.message}")
        }
    }

    private suspend fun loadWalletTransactions(userId: String) {
        try {
            val transactionResponse = ApiClient.apiService.getWalletTransactions(userId)
            android.util.Log.d("WalletFragmentNew", "Wallet transactions API response: ${transactionResponse.code()}")
            if (transactionResponse.isSuccessful) {
                val backendTransactions = transactionResponse.body().orEmpty()
                userTransactions.clear()
                
                val convertedTransactions = backendTransactions.map { convertToUITransaction(it) }
                userTransactions.addAll(convertedTransactions.sortedByDescending { it.timestamp })
                
                android.util.Log.d("WalletFragmentNew", "Loaded ${userTransactions.size} transactions from API")
                updateTransactionsDisplay()
            } else {
                userTransactions.clear()
                updateTransactionsDisplay()
                showToast("Unable to load transactions (${transactionResponse.code()})")
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletFragmentNew", "Error loading wallet transactions", e)
            userTransactions.clear()
            updateTransactionsDisplay()
            showToast("Error loading transactions: ${e.message}")
        }
    }

    private fun updateBalanceDisplay() {
        binding.tvBalanceAmount.text = "₹${String.format("%.2f", currentBalance)}"
    }

    private fun updateTransactionsDisplay() {
        if (userTransactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            transactionsAdapter.updateItems(emptyList())
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            
            val groupedItems = WalletTransactionGrouping.buildGroupedItems(
                userTransactions,
                MAX_RECENT_TRANSACTIONS
            )
            android.util.Log.d("WalletFragmentNew", "Rendering ${groupedItems.size} grouped transaction items")
            transactionsAdapter.updateItems(groupedItems)
        }
    }

    private fun loadSampleData() {
        // This method is kept for testing purposes only
        // In production, this should not be called - real data should always be used
        android.util.Log.w("WalletFragmentNew", "Using sample data - this should not happen in production!")
        
        currentBalance = 0.0
        updateBalanceDisplay()
        userTransactions.clear()
        updateTransactionsDisplay()
        showToast("No real wallet data available")
    }

    private fun convertToUITransaction(backendTransaction: WalletTransaction): Transaction {
        // Use ISO 8601 format with timezone support for better date parsing
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Set timezone to handle UTC correctly
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        fallbackFormat.timeZone = TimeZone.getDefault()
        
        // Parse timestamp or use current date as fallback
        val timestamp = try {
            if (backendTransaction.timestamp != null) {
                // Try multiple date formats
                try {
                    dateFormat.parse(backendTransaction.timestamp) ?: Date()
                } catch (e: Exception) {
                    try {
                        fallbackFormat.parse(backendTransaction.timestamp) ?: Date()
                    } catch (e2: Exception) {
                        // Try simple date format as last resort
                        simpleDateFormat.parse(backendTransaction.timestamp) ?: Date()
                    }
                }
            } else {
                Date()
            }
        } catch (e: Exception) {
            android.util.Log.w("WalletFragmentNew", "Failed to parse timestamp: ${backendTransaction.timestamp}", e)
            Date()
        }
        
        // Map transaction type and handle amount correctly
        val transactionType = when (backendTransaction.type?.uppercase()) {
            "CREDIT" -> TransactionType.TOP_UP
            "DEBIT" -> TransactionType.PARKING_PAYMENT
            "REFUND" -> TransactionType.REFUND
            "BONUS" -> TransactionType.BONUS
            else -> TransactionType.PARKING_PAYMENT
        }
        
        // Ensure proper amount handling:
        // CREDIT transactions should be positive
        // DEBIT transactions should be negative
        val amount = backendTransaction.amount ?: 0.0
        val displayAmount = when (backendTransaction.type?.uppercase()) {
            "DEBIT" -> if (amount > 0) -amount else amount  // Make sure debits are negative
            "CREDIT", "REFUND", "BONUS" -> if (amount < 0) -amount else amount  // Make sure credits are positive
            else -> amount
        }
        
        android.util.Log.d("WalletFragmentNew", "Converting transaction: type=${backendTransaction.type}, originalAmount=$amount, displayAmount=$displayAmount")
        
        return Transaction(
            id = backendTransaction.id ?: "Unknown",
            type = transactionType,
            amount = displayAmount,
            description = backendTransaction.description ?: "Transaction",
            timestamp = timestamp,
            balanceAfter = backendTransaction.balanceAfter ?: 0.0
        )
    }

    private fun showTopUpDialog() {
        try {
            android.util.Log.d("WalletFragmentNew", "showTopUpDialog called")
            
            val bottomSheetDialog = BottomSheetDialog(requireContext())
            val bottomSheetBinding = BottomSheetTopUpSimpleBinding.inflate(layoutInflater)
            bottomSheetDialog.setContentView(bottomSheetBinding.root)
            
            android.util.Log.d("WalletFragmentNew", "Bottom sheet dialog created")
            
            // Set current balance
            bottomSheetBinding.tvCurrentBalance.text = "₹${String.format("%.2f", currentBalance)}"
            
            // Setup click listeners for quick amount buttons (only 3 buttons now)
            bottomSheetBinding.btnAmount50.setOnClickListener {
                bottomSheetBinding.etAmount.setText("50")
                updateAddButtonState(bottomSheetBinding)
            }
            
            bottomSheetBinding.btnAmount100.setOnClickListener {
                bottomSheetBinding.etAmount.setText("100")
                updateAddButtonState(bottomSheetBinding)
            }
            
            bottomSheetBinding.btnAmount200.setOnClickListener {
                bottomSheetBinding.etAmount.setText("200")
                updateAddButtonState(bottomSheetBinding)
            }
            
            // Setup payment method selection (only UPI and Card)
            var selectedPaymentMethod = "UPI" // Default selection
            
            bottomSheetBinding.layoutUpi.setOnClickListener {
                selectPaymentMethod(bottomSheetBinding, "UPI")
                selectedPaymentMethod = "UPI"
            }
            
            bottomSheetBinding.layoutCard.setOnClickListener {
                selectPaymentMethod(bottomSheetBinding, "CARD")
                selectedPaymentMethod = "CARD"
            }
            
            // Setup text change listener for amount input
            bottomSheetBinding.etAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateAddButtonState(bottomSheetBinding)
                }
            })
            
            // Close button
            bottomSheetBinding.btnClose.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            
            // Add money button
            bottomSheetBinding.btnAddMoneyConfirm.setOnClickListener {
                val amountText = bottomSheetBinding.etAmount.text.toString()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        showToast("Processing payment via $selectedPaymentMethod...")
                        processTopUp(amount)
                        bottomSheetDialog.dismiss()
                    } else {
                        showToast("Please enter a valid amount")
                    }
                }
            }
            
            // Initialize with UPI selected
            selectPaymentMethod(bottomSheetBinding, "UPI")
            
            android.util.Log.d("WalletFragmentNew", "About to show bottom sheet")
            bottomSheetDialog.show()
            android.util.Log.d("WalletFragmentNew", "Bottom sheet shown")
            
        } catch (e: Exception) {
            android.util.Log.e("WalletFragmentNew", "Error showing bottom sheet", e)
            showToast("Error opening top-up dialog: ${e.message}")
        }
    }
    
    private fun selectPaymentMethod(binding: BottomSheetTopUpSimpleBinding, method: String) {
        // Reset all radio buttons (only UPI and Card now)
        binding.radioUpi.isChecked = false
        binding.radioCard.isChecked = false
        
        // Select the chosen method
        when (method) {
            "UPI" -> binding.radioUpi.isChecked = true
            "CARD" -> binding.radioCard.isChecked = true
        }
    }
    
    private fun updateAddButtonState(binding: BottomSheetTopUpSimpleBinding) {
        val amountText = binding.etAmount.text.toString()
        val amount = amountText.toDoubleOrNull()
        val isValidAmount = amount != null && amount > 0
        
        binding.btnAddMoneyConfirm.isEnabled = isValidAmount
        binding.btnAddMoneyConfirm.text = if (isValidAmount) {
            "Add ₹${amount?.toInt()}"
        } else {
            "Add Money"
        }
    }
    
    private fun processTopUp(amount: Double) {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to add money")
            return
        }

        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            var shouldRefreshWallet = false
            try {
                // Call the top-up API
                val response = ApiClient.apiService.topUpWallet(
                    userId = userId,
                    request = mapOf("amount" to amount)
                )

                if (response.isSuccessful) {
                    // Update balance immediately for better UX
                    currentBalance += amount
                    updateBalanceDisplay()
                    
                    showToast("₹${amount.toInt()} added successfully!")
                    shouldRefreshWallet = true
                } else {
                    showToast("Failed to add money. Please try again.")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressLoading.visibility = View.GONE
                if (shouldRefreshWallet) {
                    loadWalletData()
                }
            }
        }
    }

    override fun scrollToTop() {
        try {
            binding.scrollContent.smoothScrollTo(0, 0)
        } catch (e: Exception) {
            // Handle any exceptions
        }
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }
    private companion object {
        private const val MAX_RECENT_TRANSACTIONS = 5
    }
}
