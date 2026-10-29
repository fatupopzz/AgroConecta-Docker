package com.uvg.agroconecta.ui.pestalerts.map

import android.util.Log
import com.uvg.agroconecta.data.models.PestAlert

internal const val PEST_MAP_MARKER_LOG_TAG = "PestMapMarker"

internal data class PestMapMarker(
    val alertId: Int,
    val latitude: Double,
    val longitude: Double,
    val pestType: String,
    val crop: String
)

internal fun PestAlert.toPestMapMarker(): PestMapMarker? {
    val hasValidCoordinates = latitud.isFinite() &&
        longitud.isFinite() &&
        latitud in -90.0..90.0 &&
        longitud in -180.0..180.0

    if (!hasValidCoordinates) {
        Log.w(
            PEST_MAP_MARKER_LOG_TAG,
            "Ignoring pest alert $id with invalid coordinates: lat=$latitud, lng=$longitud"
        )
        return null
    }

    return PestMapMarker(
        alertId = id,
        latitude = latitud,
        longitude = longitud,
        pestType = pestType,
        crop = cultivo
    )
}

