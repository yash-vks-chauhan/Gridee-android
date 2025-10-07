package com.gridee.parking.ui.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.data.api.ApiClient
import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.databinding.ActivityTransactionHistoryBinding
import com.gridee.parking.ui.adapters.Transaction
import com.gridee.parking.ui.adapters.TransactionType
import com.gridee.parking.ui.adapters.TransactionsAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var transactionsAdapter: TransactionsAdapter
    private var allTransactions = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        loadAllTransactions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Transaction History"
        }
    }

    private fun setupRecyclerView() {
        transactionsAdapter = TransactionsAdapter(emptyList())
        
        binding.rvAllTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
            adapter = transactionsAdapter
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
                        
                        updateUI()
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

    private fun updateUI() {
        if (allTransactions.isEmpty()) {
            showEmptyState()
        } else {
            binding.rvAllTransactions.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvTransactionCount.text = "${allTransactions.size} transactions"
            transactionsAdapter.updateTransactions(allTransactions)
        }
    }

    private fun showEmptyState() {
        binding.rvAllTransactions.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tvTransactionCount.text = "0 transactions"
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvAllTransactions.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getUserId(): String? {
        val sharedPref = getSharedPreferences("gridee_prefs", MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }
}
