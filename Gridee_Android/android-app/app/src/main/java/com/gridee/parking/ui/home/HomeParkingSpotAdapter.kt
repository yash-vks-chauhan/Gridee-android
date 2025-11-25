package com.gridee.parking.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.gridee.parking.R
import com.gridee.parking.ui.utils.TicketEdgeTreatment

class HomeParkingSpotAdapter : RecyclerView.Adapter<HomeParkingSpotAdapter.HomeSpotViewHolder>() {

    private val spots = mutableListOf<HomeParkingSpot>()
    var onSpotClick: ((HomeParkingSpot, View) -> Unit)? = null

    fun submitSpots(data: List<HomeParkingSpot>) {
        spots.clear()
        spots.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeSpotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_parking_spot, parent, false)
        return HomeSpotViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeSpotViewHolder, position: Int) {
        holder.bind(spots[position], onSpotClick)
    }

    override fun getItemCount(): Int = spots.size

    class HomeSpotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardSpot)
        private val spotName: TextView = itemView.findViewById(R.id.tv_name)
        private val lotName: TextView = itemView.findViewById(R.id.tv_lot_name)
        private val availabilityCount: TextView = itemView.findViewById(R.id.tv_availability_count)
        private val availabilityLabel: TextView = itemView.findViewById(R.id.tv_availability_label)


        private val topSection: View = itemView.findViewById(R.id.cl_top_section)
        private val bottomSection: View = itemView.findViewById(R.id.cl_bottom_section)
        private val ticketRoot: View = itemView.findViewById(R.id.layout_ticket_root)


        fun bind(spot: HomeParkingSpot, onSpotClick: ((HomeParkingSpot, View) -> Unit)?) {
            // Apply Ticket Shape (Notches) - Keeping this for legacy or if needed, but XML handles visual
            card.shapeAppearanceModel = card.shapeAppearanceModel.toBuilder()
                .setLeftEdge(TicketEdgeTreatment(32f))
                .setRightEdge(TicketEdgeTreatment(32f))
                .build()

            spotName.text = spot.spotName
            lotName.text = spot.lotName

            val context = itemView.context
            
            // Reset State (Animations & Translations)
            availabilityCount.clearAnimation()
            availabilityCount.alpha = 1f
            topSection.translationY = 0f
            bottomSection.translationY = 0f
            topSection.rotation = 0f
            bottomSection.rotation = 0f
            topSection.scaleX = 1f
            topSection.scaleY = 1f
            topSection.alpha = 1f
            bottomSection.scaleX = 1f
            bottomSection.scaleY = 1f
            bottomSection.alpha = 1f

            // Availability Logic
            if (spot.isAvailable) {
                availabilityCount.text = if (spot.availableUnits > 0) "${spot.availableUnits}" else "✓"
                availabilityLabel.text = if (spot.availableUnits > 0) "SPOTS" else "OPEN"
                
                availabilityLabel.setTextColor(context.getColor(R.color.booking_status_active_text))
                availabilityCount.setTextColor(context.getColor(R.color.text_primary))
                
                // "Live" Pulse for Low Availability (< 5 spots)
                if (spot.availableUnits in 1..4) {
                    val pulseAnim = android.animation.ObjectAnimator.ofFloat(availabilityCount, "alpha", 1f, 0.6f, 1f)
                    pulseAnim.duration = 2000 // Slow 2s pulse
                    pulseAnim.repeatCount = android.animation.ValueAnimator.INFINITE
                    pulseAnim.repeatMode = android.animation.ValueAnimator.REVERSE
                    pulseAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    pulseAnim.start()
                }

            } else {
                availabilityCount.text = "0"
                availabilityLabel.text = "FULL"
                
                availabilityLabel.setTextColor(context.getColor(R.color.booking_status_completed_text))
                availabilityCount.setTextColor(context.getColor(R.color.text_secondary))
            }

            // "Spring" Tactile Feedback on Touch (Applied to the Ticket Root)
            ticketRoot.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                    }
                }
                true
            }

            ticketRoot.setOnClickListener { v ->
                // Haptic Feedback
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                
                // Trigger navigation immediately with the view for transition
                onSpotClick?.invoke(spot, ticketRoot)
            }
        }
    }
}
