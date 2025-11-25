package com.gridee.parking.ui.utils

import com.google.android.material.shape.EdgeTreatment
import com.google.android.material.shape.ShapePath

class TicketEdgeTreatment(private val size: Float) : EdgeTreatment() {

    override fun getEdgePath(
        length: Float,
        center: Float,
        interpolation: Float,
        shapePath: ShapePath
    ) {
        val circleRadius = size * interpolation
        shapePath.lineTo(center - circleRadius, 0f)
        shapePath.addArc(
            center - circleRadius,
            -circleRadius,
            center + circleRadius,
            circleRadius,
            180f,
            -180f
        )
        shapePath.lineTo(length, 0f)
    }
}
