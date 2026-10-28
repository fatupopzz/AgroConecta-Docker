package com.uvg.agroconecta.ui.pestalerts.map

import com.uvg.agroconecta.data.models.PestAlert

internal data class PestMapMarker(
    val alertId: Int,
    val latitude: Double,
    val longitude: Double,
    val pestType: String,
    val crop: String
)

internal fun PestAlert.toPestMapMarker(): PestMapMarker? =
    if (latitud.isFinite() &&
        longitud.isFinite() &&
        latitud in -90.0..90.0 &&
        longitud in -180.0..180.0
    ) {
        PestMapMarker(
            alertId = id,
            latitude = latitud,
            longitude = longitud,
            pestType = pestType,
            crop = cultivo
        )
    } else {
        null
    }

