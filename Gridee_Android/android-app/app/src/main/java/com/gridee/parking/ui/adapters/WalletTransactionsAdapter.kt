package com.gridee.parking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gridee.parking.databinding.ItemTransactionBinding
import com.gridee.parking.databinding.ItemWalletTransactionHeaderBinding

sealed class WalletTransactionListItem {
    data class Header(val title: String) : WalletTransactionListItem()
    data class Item(val transaction: Transaction) : WalletTransactionListItem()
}

class WalletTransactionsAdapter(
    private var items: List<WalletTransactionListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TRANSACTION = 1
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WalletTransactionListItem.Header -> TYPE_HEADER
            is WalletTransactionListItem.Item -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemWalletTransactionHeaderBinding.inflate(layoutInflater, parent, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemTransactionBinding.inflate(layoutInflater, parent, false)
                TransactionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is WalletTransactionListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is WalletTransactionListItem.Item -> (holder as TransactionViewHolder).bind(item.transaction)
        }
    }

    fun updateItems(newItems: List<WalletTransactionListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private class HeaderViewHolder(
        private val binding: ItemWalletTransactionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WalletTransactionListItem.Header) {
            binding.tvSectionTitle.text = item.title
        }
    }

    private inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            TransactionsAdapter.bindTransactionRow(binding, transaction)
        }
    }
}
