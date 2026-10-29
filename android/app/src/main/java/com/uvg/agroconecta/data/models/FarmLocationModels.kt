package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName
import com.uvg.agroconecta.data.location.GeoCoordinates

/**
 * Ubicación de la finca del agricultor. Las coordenadas son opcionales:
 * quien no da permiso de GPS igual guarda su municipio y una referencia.
 */
data class FarmLocation(
    val departamento: String,
    val municipio: String,
    val referencia: String? = null,
    @SerializedName("latitud") val latitude: Double? = null,
    @SerializedName("longitud") val longitude: Double? = null
) {
    val coordinates: GeoCoordinates?
        get() = if (latitude != null && longitude != null) GeoCoordinates(latitude, longitude) else null

    /** Texto para la dirección de entrega: de lo más específico a lo general. */
    fun deliveryText(): String =
        listOfNotNull(referencia?.takeIf { it.isNotBlank() }, municipio, departamento).joinToString(", ")
}

data class SaveFarmLocationRequest(
    val departamento: String,
    val municipio: String,
    val referencia: String?,
    @SerializedName("latitud") val latitude: Double?,
    @SerializedName("longitud") val longitude: Double?
)
