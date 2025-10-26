package com.gridee.parking.ui.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.databinding.ItemTransactionBinding
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val timestamp: Date,
    val balanceAfter: Double,
    val paymentMethod: String? = null,
    val reference: String? = null,
    val status: String? = null
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

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            bindTransactionRow(binding, transaction)
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

    companion object {
        fun bindTransactionRow(binding: ItemTransactionBinding, transaction: Transaction) {
            val context = binding.root.context

            binding.tvDescription.text = transaction.description

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                transaction.timestamp.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.tvTimestamp.text = relativeTime

            // Show balance after for successful transactions only; otherwise show status
            val statusLower = transaction.status?.lowercase(Locale.getDefault())
            if (statusLower == "failed" || statusLower == "cancelled" || statusLower == "canceled") {
                binding.tvBalance.text = if (statusLower == "failed") "Failed" else "Cancelled"
            } else {
                binding.tvBalance.text = context.getString(
                    R.string.wallet_transaction_after,
                    transaction.balanceAfter
                )
            }

            val statusIndicatesFailure = statusLower == "failed" || statusLower == "cancelled" || statusLower == "canceled"
            val amountPositive = transaction.amount >= 0
            val amountColor = when {
                statusIndicatesFailure -> R.color.red
                amountPositive -> R.color.success_green
                else -> R.color.red
            }
            binding.tvAmount.setTextColor(ContextCompat.getColor(context, amountColor))
            val absoluteAmount = abs(transaction.amount)
            val formattedAmount = String.format(
                Locale.getDefault(),
                "%.2f",
                absoluteAmount
            )

            val amountText = if (statusIndicatesFailure) {
                // For failed/cancelled, avoid misleading +/-, just show amount
                "₹$formattedAmount"
            } else {
                if (amountPositive) "+₹$formattedAmount" else "-₹$formattedAmount"
            }
            binding.tvAmount.text = amountText

            binding.ivTransactionIcon.setImageResource(R.drawable.ic_wallet_outline)
            binding.ivTransactionIcon.imageTintList =
                ContextCompat.getColorStateList(context, R.color.text_secondary)
            
            binding.cardIconContainer.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.background_primary)
            )

            val strokeColor = ContextCompat.getColor(context, R.color.border_light)
            binding.cardIconContainer.strokeColor = strokeColor
        }
    }
}
