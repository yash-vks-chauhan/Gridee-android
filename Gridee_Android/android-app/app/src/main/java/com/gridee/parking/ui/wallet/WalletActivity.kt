package com.gridee.parking.ui.wallet

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gridee.parking.R
import com.gridee.parking.databinding.ActivityWalletBinding
import com.gridee.parking.ui.base.BaseActivityWithBottomNav
import com.gridee.parking.ui.components.CustomBottomNavigation

class WalletActivity : BaseActivityWithBottomNav<ActivityWalletBinding>() {
    
    private lateinit var viewModel: WalletViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    
    override fun getViewBinding(): ActivityWalletBinding {
        return ActivityWalletBinding.inflate(layoutInflater)
    }
    
    override fun getCurrentTab(): Int {
        return CustomBottomNavigation.TAB_WALLET
    }
    
    override fun setupUI() {
        viewModel = ViewModelProvider(this)[WalletViewModel::class.java]
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Setup scroll behavior for RecyclerView
        setupScrollBehaviorForView(binding.rvTransactions)
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@WalletActivity)
            adapter = transactionAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            btnAddMoney.setOnClickListener { showCustomTopupDialog() }
            
            btnTopup50.setOnClickListener { viewModel.topUpWallet(50.0) }
            btnTopup100.setOnClickListener { viewModel.topUpWallet(100.0) }
            btnTopup200.setOnClickListener { viewModel.topUpWallet(200.0) }
            
            tvViewAll.setOnClickListener {
                // TODO: Navigate to full transaction history
                showToast("Full transaction history coming soon!")
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.walletDetails.observe(this) { walletDetails ->
            binding.tvBalance.text = "₹${String.format("%.2f", walletDetails.balance ?: 0.0)}"
            
            // Show recent transactions (last 5)
            val transactions = walletDetails.transactions ?: emptyList()
            val recentTransactions = transactions.take(5)
            transactionAdapter.submitList(recentTransactions)
            
            // Show/hide empty state
            if (transactions.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvTransactions.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvTransactions.visibility = View.VISIBLE
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                showToast(it)
                viewModel.clearError()
            }
        }
        
        viewModel.topupSuccess.observe(this) { success ->
            if (success) {
                showToast("Wallet topped up successfully!")
                viewModel.clearTopupSuccess()
            }
        }
    }
    
    private fun showCustomTopupDialog() {
        val editText = EditText(this).apply {
            hint = "Enter amount (₹)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        AlertDialog.Builder(this)
            .setTitle("Add Money to Wallet")
            .setMessage("Enter the amount you want to add:")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val amountText = editText.text.toString()
                if (amountText.isNotEmpty()) {
                    try {
                        val amount = amountText.toDouble()
                        if (amount > 0) {
                            viewModel.topUpWallet(amount)
                        } else {
                            showToast("Please enter a valid amount")
                        }
                    } catch (e: NumberFormatException) {
                        showToast("Please enter a valid number")
                    }
                } else {
                    showToast("Please enter an amount")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
