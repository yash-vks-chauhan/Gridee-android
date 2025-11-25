package com.gridee.parking.ui.fragments

import android.content.Intent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
import com.gridee.parking.ui.adapters.TransactionsAdapter
import com.gridee.parking.ui.base.BaseTabFragment
import com.gridee.parking.ui.wallet.WalletTransactionMapper
import kotlinx.coroutines.launch

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

    override fun onResume() {
        super.onResume()
        // Refresh wallet data after returning from Razorpay checkout
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
        // Add money button
        binding.btnAddMoney.setOnClickListener {
            showTopUpDialog()
        }
        
        // Tap balance to show/hide (privacy feature)
        binding.tvBalanceAmount.setOnClickListener {
            showTopUpDialog()
        }
        
        // Quick add buttons
        binding.btnQuickAdd10.setOnClickListener {
            processTopUp(50.0)
        }
        
        binding.btnQuickAdd20.setOnClickListener {
            processTopUp(100.0)
        }
        
        binding.btnQuickAdd100.setOnClickListener {
            processTopUp(200.0)
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
                    if (balanceResponse.code() == 401) {
                        handleUnauthorized()
                        return@launch
                    } else if (balanceResponse.code() == 404) {
                        // Wallet not found — treat as zero balance for resilience
                        currentBalance = 0.0
                        updateBalanceDisplay()
                        // Continue to load transactions
                    } else {
                        showToast("Failed to load wallet balance: ${balanceResponse.code()}")
                    }
                }

                // Fetch wallet transactions separately
                val transactionsResponse = ApiClient.apiService.getWalletTransactions(userId)
                if (transactionsResponse.isSuccessful) {
                    val transactions = transactionsResponse.body()
                    if (transactions != null && transactions.isNotEmpty()) {
                        userTransactions.clear()
                        try {
                            userTransactions.addAll(
                                transactions.mapNotNull { transaction ->
                                    // Skip null transactions and handle conversion errors
                                    try {
                                        WalletTransactionMapper.map(transaction)
                                    } catch (e: Exception) {
                                        showToast("Error converting transaction: ${e.message}")
                                        null
                                    }
                                }
                            )
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
                    if (transactionsResponse.code() == 401) {
                        handleUnauthorized()
                        return@launch
                    }
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
        binding.tvBalanceAmount.text = "₹${String.format("%.2f", currentBalance)}"
        
        // Update last updated time
        binding.tvLastUpdated.text = "Updated now"
        
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

    private fun getUserId(): String? {
        val sharedPref = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    private fun showTopUpDialog() {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter amount (₹)"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Money")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val amount = input.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) {
                    startRazorpayCheckout(amount)
                } else {
                    showToast("Please enter a valid amount")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun processTopUp(amount: Double) {
        // Route all top-ups via Razorpay
        startRazorpayCheckout(amount)
    }

    private fun startRazorpayCheckout(amount: Double) {
        val userId = getUserId()
        if (userId == null) {
            showToast("Please login to add money")
            return
        }

        lifecycleScope.launch {
            try {
                val initResp = ApiClient.apiService.initiatePayment(
                    com.gridee.parking.data.model.PaymentInitiateRequest(
                        userId = userId,
                        amount = amount
                    )
                )

                if (!initResp.isSuccessful) {
                    showToast("Failed to initiate payment: ${initResp.code()}")
                    return@launch
                }

                val body = initResp.body()
                val orderId = body?.orderId
                val keyId = body?.keyId
                if (orderId.isNullOrBlank()) {
                    showToast("Invalid payment order from server")
                    return@launch
                }

                val intent = Intent(requireContext(), com.gridee.parking.ui.wallet.WalletTopUpActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("AMOUNT", amount)
                intent.putExtra("ORDER_ID", orderId)
                keyId?.let { intent.putExtra("KEY_ID", it) }
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun handleUnauthorized() {
        showToast("Session expired. Please log in again.")
        try {
            val prefs = requireActivity().getSharedPreferences("gridee_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            com.gridee.parking.utils.JwtTokenManager(requireContext()).clearAuthToken()
        } catch (_: Exception) { }
        val intent = android.content.Intent(requireContext(), com.gridee.parking.ui.auth.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
