package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

/**
 * Alerta activa que puede mostrarse tanto en la lista como en el mapa.
 * Las coordenadas se mantienen en el modelo porque el mapa no debe depender
 * de una segunda consulta para construir sus marcadores.
 */
data class PestAlert(
    @SerializedName("id_alerta") val id: Int,
    @SerializedName("tipo_plaga") val pestType: String,
    val cultivo: String,
    @SerializedName("distancia_km") val distanceKm: Double,
    val latitud: Double,
    val longitud: Double,
    @SerializedName("fecha_reporte") val reportedAt: String,
    val activa: Boolean = true,
    @SerializedName("productos_preventivos")
    val suggestedProducts: List<PestSuggestedProduct> = emptyList()
)

data class PestSuggestedProduct(
    @SerializedName("id_producto") val id: Int,
    val nombre: String,
    val marca: String? = null,
    @SerializedName("uso_recomendado") val recommendedUse: String? = null
)

data class NearbyPestAlertsResponse(
    val alertas: List<PestAlert> = emptyList()
)

data class PestAlertReportRequest(
    @SerializedName("tipo_plaga") val pestType: String,
    val cultivo: String,
    val latitud: Double,
    val longitud: Double
)

data class PestAlertReportResponse(
    val message: String,
    val alerta: PestAlert
)

/** Opciones compartidas por los formularios que reportan una plaga. */
enum class PestType(val apiValue: String, val displayName: String) {
    APHID("pulgon", "Pulgón"),
    WHITEFLY("mosca_blanca", "Mosca blanca"),
    THRIPS("trips", "Trips"),
    FALL_ARMYWORM("gusano_cogollero", "Gusano cogollero"),
    FUNGI("hongos", "Hongos"),
    MITES("acaros", "Ácaros"),
    OTHER("otra", "Otra plaga")
}

