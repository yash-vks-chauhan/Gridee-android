package com.gridee.parking.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.databinding.FragmentWalletNewBinding
import com.gridee.parking.ui.activities.TransactionHistoryActivity
import com.gridee.parking.ui.adapters.Transaction
import com.gridee.parking.ui.adapters.TransactionType
import com.gridee.parking.ui.adapters.TransactionsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WalletFragment : BaseTabFragment<FragmentWalletNewBinding>() {

    private lateinit var transactionsAdapter: TransactionsAdapter
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
        transactionsAdapter = TransactionsAdapter(emptyList())
        
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        // Add top-up functionality to the wallet balance card
        binding.tvBalanceAmount.setOnClickListener {
            showTopUpDialog()
        }
        
        binding.tvViewAll.setOnClickListener {
            // Navigate to TransactionHistoryActivity to show all transactions
            val intent = Intent(requireContext(), TransactionHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadWalletData() {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to view wallet")
            return
        }

        lifecycleScope.launch {
            try {
                // Fetch wallet balance
                val balanceResponse = ApiClient.apiService.getWalletDetails(userId)
                if (balanceResponse.isSuccessful) {
                    val walletDetails = balanceResponse.body()
                    if (walletDetails != null) {
                        currentBalance = walletDetails.balance ?: 0.0
                        updateBalanceDisplay()
                    } else {
                        showToast("Wallet details response is null")
                    }
                } else {
                    showToast("Failed to load wallet balance: ${balanceResponse.code()}")
                }

                // Fetch wallet transactions separately
                val transactionsResponse = ApiClient.apiService.getWalletTransactions(userId)
                if (transactionsResponse.isSuccessful) {
                    val transactions = transactionsResponse.body()
                    if (transactions != null && transactions.isNotEmpty()) {
                        userTransactions.clear()
                        try {
                            userTransactions.addAll(transactions.mapNotNull { transaction ->
                                // Skip null transactions and handle conversion errors
                                try {
                                    convertToUITransaction(transaction)
                                } catch (e: Exception) {
                                    showToast("Error converting transaction: ${e.message}")
                                    null
                                }
                            })
                            updateTransactionsList()
                            if (userTransactions.isNotEmpty()) {
                                showToast("Loaded ${userTransactions.size} transactions")
                            } else {
                                showToast("No valid transactions found")
                            }
                        } catch (e: Exception) {
                            showToast("Error processing transactions: ${e.message}")
                            userTransactions.clear()
                            updateTransactionsList()
                        }
                    } else {
                        // Empty transactions list
                        userTransactions.clear()
                        updateTransactionsList()
                        showToast("No transactions found")
                    }
                } else {
                    showToast("Failed to load transactions: ${transactionsResponse.code()}")
                    userTransactions.clear()
                    updateTransactionsList() // Show empty state
                }
                
            } catch (e: Exception) {
                showToast("Error loading wallet: ${e.message}")
                // Load cached balance as fallback
                loadCachedBalance()
                userTransactions.clear()
                updateTransactionsList() // Show empty state for transactions
            }
        }
    }

    private fun loadCachedBalance() {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        currentBalance = sharedPref.getFloat("wallet_balance", 0.0f).toDouble()
        updateBalanceDisplay()
    }

    private fun updateBalanceDisplay() {
        binding.tvBalanceAmount.text = "$${String.format("%.2f", currentBalance)}"
        
        // Save balance to cache
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        sharedPref.edit().putFloat("wallet_balance", currentBalance.toFloat()).apply()
    }

    private fun updateTransactionsList() {
        if (userTransactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            
            // Show only recent transactions (last 5)
            val recentTransactions = userTransactions.take(5)
            transactionsAdapter.updateTransactions(recentTransactions)
        }
    }

    private fun convertToUITransaction(walletTransaction: WalletTransaction): Transaction {
        // Validate required fields
        val id = walletTransaction.id?.trim()
        val timestamp = walletTransaction.timestamp?.trim()
        
        if (id.isNullOrBlank() || timestamp.isNullOrBlank()) {
            throw IllegalArgumentException("Transaction missing required fields (id or timestamp)")
        }
        
        // Try multiple date formats
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
            parsedDate ?: Date() // Use current date if parsing fails
        } catch (e: Exception) {
            Date() // Fallback to current date
        }

        // Validate and convert transaction type
        val transactionType = walletTransaction.type?.uppercase()?.trim() ?: "CREDIT"
        
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
            description = walletTransaction.description?.trim() ?: "Transaction",
            timestamp = parsedTimestamp,
            balanceAfter = walletTransaction.balanceAfter ?: 0.0
        )
    }

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    private fun showTopUpDialog() {
        // TODO: Implement top-up functionality
        showToast("Top-up feature coming soon!")
    }

    private fun processTopUp(amount: Double) {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to top up wallet")
            return
        }

        lifecycleScope.launch {
            try {
                val request = mapOf("amount" to amount)
                val response = ApiClient.apiService.topUpWallet(userId, request)
                
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result != null) {
                        // Update balance from server response
                        val newBalance = result["balance"] as? Double
                        if (newBalance != null) {
                            currentBalance = newBalance
                            updateBalanceDisplay()
                        }
                        
                        showToast("Successfully added $${String.format("%.2f", amount)} to your wallet!")
                        
                        // Refresh wallet data to get updated transactions
                        loadWalletData()
                    }
                } else {
                    showToast("Top-up failed. Please try again.")
                }
            } catch (e: Exception) {
                showToast("Error processing top-up: ${e.message}")
            }
        }
    }
}
