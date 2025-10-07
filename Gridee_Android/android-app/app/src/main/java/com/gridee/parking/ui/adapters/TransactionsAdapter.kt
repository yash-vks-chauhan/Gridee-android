package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val timestamp: Date,
    val balanceAfter: Double,
    val paymentMethod: String? = null,
    val reference: String? = null
)

enum class TransactionType {
    TOP_UP,
    PARKING_PAYMENT,
    REFUND,
    BONUS
}

class TransactionsAdapter(
    private var transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(transaction: Transaction) {
            binding.apply {
                // Set transaction icon based on type
                when (transaction.type) {
                    TransactionType.TOP_UP -> {
                        ivTransactionIcon.setImageResource(R.drawable.ic_add)
                        ivTransactionIcon.backgroundTintList = 
                            root.context.getColorStateList(R.color.success_green)
                    }
                    TransactionType.PARKING_PAYMENT -> {
                        ivTransactionIcon.setImageResource(R.drawable.ic_parking)
                        ivTransactionIcon.backgroundTintList = 
                            root.context.getColorStateList(R.color.primary_blue)
                    }
                    TransactionType.REFUND -> {
                        ivTransactionIcon.setImageResource(R.drawable.ic_refund)
                        ivTransactionIcon.backgroundTintList = 
                            root.context.getColorStateList(R.color.success_green)
                    }
                    TransactionType.BONUS -> {
                        ivTransactionIcon.setImageResource(R.drawable.ic_gift)
                        ivTransactionIcon.backgroundTintList = 
                            root.context.getColorStateList(R.color.primary_blue)
                    }
                }

                // Set transaction details
                tvDescription.text = transaction.description
                tvTimestamp.text = dateFormat.format(transaction.timestamp)
                tvBalance.text = "Balance: $${String.format("%.2f", transaction.balanceAfter)}"

                // Set amount with appropriate color
                val amountText = if (transaction.amount >= 0) {
                    tvAmount.setTextColor(root.context.getColor(R.color.success_green))
                    "+$${String.format("%.2f", transaction.amount)}"
                } else {
                    tvAmount.setTextColor(root.context.getColor(R.color.red))
                    "-$${String.format("%.2f", Math.abs(transaction.amount))}"
                }
                tvAmount.text = amountText
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
