package com.gridee.parking.ui.wallet

import com.gridee.parking.data.model.WalletTransaction
import com.gridee.parking.ui.adapters.Transaction
import com.gridee.parking.ui.adapters.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Ensures wallet transactions are rendered consistently across screens.
 */
object WalletTransactionMapper {

    private val utcTimeZone = TimeZone.getTimeZone("UTC")
    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    private val creditTypes = setOf("credit", "top_up", "topup", "wallet_topup", "wallet_recharge")
    private val debitTypes = setOf("debit", "payment", "penalty_deduction", "charge", "deduction")
    private val rewardTypes = setOf("bonus", "reward_bonus", "reward", "ad_reward")

    fun map(transaction: WalletTransaction): Transaction {
        val timestamp = parseTimestamp(transaction.timestamp)
        val typeNorm = transaction.type?.trim()?.lowercase(Locale.getDefault())
        val statusNorm = transaction.status?.trim()?.lowercase(Locale.getDefault())
        val amount = transaction.amount ?: 0.0

        val transactionType = when {
            typeNorm in rewardTypes -> TransactionType.BONUS
            typeNorm == "refund" -> TransactionType.REFUND
            typeNorm in debitTypes -> TransactionType.PARKING_PAYMENT
            typeNorm in creditTypes -> TransactionType.TOP_UP
            amount < 0 -> TransactionType.PARKING_PAYMENT
            else -> TransactionType.TOP_UP
        }

        val displayAmount = when (transactionType) {
            TransactionType.PARKING_PAYMENT -> if (amount > 0) -amount else amount
            else -> if (amount < 0) -amount else amount
        }

        val baseDescription = when (transactionType) {
            TransactionType.TOP_UP -> "Wallet Top-up"
            TransactionType.PARKING_PAYMENT -> "Parking Payment"
            TransactionType.REFUND -> "Refund"
            TransactionType.BONUS -> "Reward Added"
        }

        val description = when (statusNorm) {
            "failed" -> "$baseDescription Failed"
            "cancelled", "canceled" -> "$baseDescription Cancelled"
            else -> transaction.description?.takeUnless { it.isBlank() } ?: baseDescription
        }

        return Transaction(
            id = transaction.id?.takeUnless { it.isBlank() } ?: "Unknown",
            type = transactionType,
            amount = displayAmount,
            description = description,
            timestamp = timestamp,
            balanceAfter = transaction.balanceAfter ?: 0.0,
            paymentMethod = null,
            status = transaction.status
        )
    }

    private fun parseTimestamp(rawTimestamp: String?): Date {
        if (rawTimestamp.isNullOrBlank()) {
            return Date()
        }

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = utcTimeZone
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).apply {
                timeZone = istTimeZone
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = istTimeZone
            },
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = istTimeZone
            },
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = istTimeZone
            }
        )

        formats.forEach { format ->
            try {
                val parsed = format.parse(rawTimestamp)
                if (parsed != null) {
                    return parsed
                }
            } catch (_: Exception) {
                // Try next parser
            }
        }

        return Date()
    }
}
