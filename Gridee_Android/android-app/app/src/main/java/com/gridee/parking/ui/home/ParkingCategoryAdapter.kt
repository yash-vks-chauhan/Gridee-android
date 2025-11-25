package com.gridee.parking.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.gridee.parking.R

class ParkingCategoryAdapter(
    private val onCategoryClick: (ParkingSpotCategory) -> Unit
) : RecyclerView.Adapter<ParkingCategoryAdapter.CategoryViewHolder>() {

    private val categories = mutableListOf<ParkingSpotCategory>()
    private var selectedCategory: ParkingSpotCategory? = null

    fun submitCategories(
        data: List<ParkingSpotCategory>,
        selected: ParkingSpotCategory?
    ) {
        categories.clear()
        categories.addAll(data)
        selectedCategory = selected
        notifyDataSetChanged()
    }

    fun updateSelection(category: ParkingSpotCategory?) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, category == selectedCategory, onCategoryClick)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        private val title: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.tvCategorySubtitle)
        private val selectedIcon: ImageView = itemView.findViewById(R.id.ivCategorySelected)

        fun bind(
            category: ParkingSpotCategory,
            isSelected: Boolean,
            onCategoryClick: (ParkingSpotCategory) -> Unit
        ) {
            val context = itemView.context
            title.text = context.getString(category.titleRes)
            subtitle.text = context.getString(category.descriptionRes)

            val selectedColor = ContextCompat.getColor(context, R.color.primary_blue_light)
            val defaultColor = ContextCompat.getColor(context, R.color.background_secondary)
            card.setCardBackgroundColor(if (isSelected) selectedColor else defaultColor)

            val strokeWidth = if (isSelected) {
                context.resources.getDimensionPixelSize(R.dimen.parking_category_card_stroke)
            } else {
                0
            }
            card.strokeWidth = strokeWidth
            selectedIcon.isVisible = isSelected

            card.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }
}
