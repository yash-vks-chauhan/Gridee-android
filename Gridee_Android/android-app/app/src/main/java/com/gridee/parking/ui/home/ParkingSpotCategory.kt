package com.gridee.parking.ui.home

import androidx.annotation.StringRes
import com.gridee.parking.R
import java.util.Locale

enum class ParkingSpotCategory(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    private val names: Set<String>,
    private val ids: Set<String>
) {
    MEDICAL(
        titleRes = R.string.parking_category_medical_title,
        descriptionRes = R.string.parking_category_medical_subtitle,
        names = setOf(
            "SRM COLLEGE OF NURSING BLOCK",
            "SRM DENTAL COLLEGE"
        ),
        ids = setOf(
            "692201ab79257a79a244937b",
            "692201ab79257a79a244937a"
        )
    ),
    CHEMICAL(
        titleRes = R.string.parking_category_chemical_title,
        descriptionRes = R.string.parking_category_chemical_subtitle,
        names = setOf(
            "BEL CAR PARKING",
            "BEL PARKING"
        ),
        ids = setOf(
            "6921ff20be555406ab15da99",
            "6921ff20be555406ab15da97"
        )
    ),
    ENGINEERING(
        titleRes = R.string.parking_category_engineering_title,
        descriptionRes = R.string.parking_category_engineering_subtitle,
        names = setOf(
            "TP AVENUE",
            "TP JAVA PARKING",
            "TP VENDHAR SQUARE",
            "TP ZONE A",
            "TP ZONE B"
        ),
        ids = setOf(
            "6921ff20be555406ab15da98",
            "6922004dc6b0924cc69e9364",
            "6922004dc6b0924cc69e9363",
            "6921ff20be555406ab15da95",
            "6921ff20be555406ab15da96"
        )
    ),
    UB(
        titleRes = R.string.parking_category_ub_title,
        descriptionRes = R.string.parking_category_ub_subtitle,
        names = setOf(
            "UB PARKING (MEENAKSHI)"
        ),
        ids = setOf(
            "69220147a0d9145bcdbc817e"
        )
    );

    private val normalized: Set<String> = names.map { it.trim().uppercase(Locale.ROOT) }.toSet()
    private val normalizedIds: Set<String> = ids.map { it.trim().lowercase(Locale.ROOT) }.toSet()

    fun matches(spot: HomeParkingSpot): Boolean {
        val normalizedId = spot.id.trim().lowercase(Locale.ROOT)
        if (normalizedIds.contains(normalizedId)) return true
        if (normalized.isEmpty()) return true
        val spotName = spot.spotName.trim().uppercase(Locale.ROOT)
        return normalized.contains(spotName)
    }

    fun filter(spots: List<HomeParkingSpot>): List<HomeParkingSpot> {
        if (normalized.isEmpty()) return spots
        return spots.filter { matches(it) }
    }

    companion object {
        fun default(): ParkingSpotCategory = MEDICAL

        fun asList(): List<ParkingSpotCategory> = values().toList()
    }
}
