package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.R
import com.gridee.parking.databinding.ItemTransactionBinding
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
        private val IST_TIME_ZONE = TimeZone.getTimeZone("Asia/Kolkata")

        fun bindTransactionRow(binding: ItemTransactionBinding, transaction: Transaction) {
            val context = binding.root.context

            binding.tvDescription.text = transaction.description

            binding.tvTimestamp.text = formatRelativeTimestamp(transaction.timestamp.time)

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

        private fun formatRelativeTimestamp(timestamp: Long): String {
            val nowCalendar = Calendar.getInstance(IST_TIME_ZONE)
            val eventCalendar = Calendar.getInstance(IST_TIME_ZONE).apply {
                timeInMillis = timestamp
            }
            var diffMillis = nowCalendar.timeInMillis - eventCalendar.timeInMillis
            if (diffMillis < 0) diffMillis = 0

            val seconds = diffMillis / 1000
            if (seconds < 5) return "Just now"
            if (seconds < 60) return pluralize(seconds, "sec")

            val minutes = seconds / 60
            if (minutes < 60) return pluralize(minutes, "min")

            val hours = minutes / 60
            if (hours < 24) return pluralize(hours, "hour")

            val days = hours / 24
            if (days < 7) return pluralize(days, "day")

            val weeks = days / 7
            if (weeks < 4) return pluralize(weeks, "week")

            val months = days / 30
            if (months < 12) return pluralize(months, "month")

            val years = months / 12
            return pluralize(years, "year")
        }

        private fun pluralize(value: Long, unit: String): String {
            val suffix = if (value == 1L) unit else "${unit}s"
            return "$value $suffix ago"
        }
    }
}
