package com.gridee.parking.data.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Lenient parser for ParkingSpot that tolerates legacy backend payloads:
 * - `available` can be boolean or number
 * - `status` may be missing/null
 * - `lotName` may be present instead of `lotId`
 */
class ParkingSpotDeserializer : JsonDeserializer<ParkingSpot> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ParkingSpot {
        val obj = json.asJsonObject

        val spotCode = obj.get("spotId")?.asString
        val id = obj.get("id")?.asString
            ?: obj.get("_id")?.asString
            ?: spotCode
            ?: ""
        val lotId = obj.get("lotId")?.asString ?: ""
        val lotName = obj.get("lotName")?.asString
        val name = obj.get("name")?.asString
        val zoneName = obj.get("zoneName")?.asString
        val capacity = obj.get("capacity")?.asInt ?: 0

        val availableElement = obj.get("available")
        val available = when {
            availableElement == null || availableElement.isJsonNull -> 0
            availableElement.isJsonPrimitive && availableElement.asJsonPrimitive.isBoolean ->
                if (availableElement.asBoolean) 1 else 0
            availableElement.isJsonPrimitive && availableElement.asJsonPrimitive.isNumber ->
                availableElement.asInt
            else -> 0
        }

        val status = obj.get("status")?.takeIf { !it.isJsonNull }?.asString
            ?: if (available > 0) "available" else "unavailable"

        return ParkingSpot(
            id = id,
            lotId = lotId,
            lotName = lotName,
            spotCode = spotCode,
            name = name,
            zoneName = zoneName,
            capacity = capacity,
            available = available,
            status = status
        )
    }
}
