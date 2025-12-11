package com.gridee.parking.ui.adapters

import java.util.Calendar

object WalletTransactionGrouping {

    fun buildGroupedItems(
        transactions: List<Transaction>,
        maxItems: Int? = null
    ): List<WalletTransactionListItem> {
        if (transactions.isEmpty()) return emptyList()

        val sorted = transactions.sortedByDescending { it.timestamp }
        val limited = maxItems?.let { sorted.take(it) } ?: sorted

        val groups = linkedMapOf(
            "Today" to mutableListOf<Transaction>(),
            "Last Week" to mutableListOf(),
            "Last Month" to mutableListOf()
        )

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfLastWeek = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }

        limited.forEach { transaction ->
            val txnCal = Calendar.getInstance().apply { time = transaction.timestamp }
            val bucket = when {
                txnCal.timeInMillis >= startOfToday.timeInMillis -> "Today"
                txnCal.timeInMillis >= startOfLastWeek.timeInMillis -> "Last Week"
                else -> "Last Month"
            }
            groups[bucket]?.add(transaction)
        }

        val items = mutableListOf<WalletTransactionListItem>()
        groups.forEach { (title, txns) ->
            if (txns.isNotEmpty()) {
                items.add(WalletTransactionListItem.Header(title))
                txns.forEach { txn ->
                    items.add(WalletTransactionListItem.Item(txn))
                }
            }
        }

        return items
    }
}
