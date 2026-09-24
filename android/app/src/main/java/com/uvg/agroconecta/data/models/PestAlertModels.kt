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
    @SerializedName("cultivo_afectado") val cultivo: String,
    @SerializedName("distancia_km") val distanceKm: Double = 0.0,
    val latitud: Double,
    val longitud: Double,
    @SerializedName("fecha_reporte") val reportedAt: String,
    val activa: Boolean = true,
    val descripcion: String? = null,
    val departamento: String? = null,
    val municipio: String? = null,
    val severidad: String = "media",
    @SerializedName("nombre_usuario") val reporterName: String? = null,
    @SerializedName("fecha_expiracion") val expiresAt: String? = null
)

data class PestSuggestedProduct(
    @SerializedName("id_producto") val id: Int,
    val nombre: String,
    val marca: String? = null,
    val descripcion: String? = null,
    val composicion: String? = null,
    @SerializedName("dosis_recomendada") val recommendedDose: String? = null,
    val categoria: String? = null
)

data class NearbyPestAlertsResponse(
    val total: Int = 0,
    @SerializedName("radio_km") val radiusKm: Double = 0.0,
    val alertas: List<PestAlert> = emptyList()
)

data class PestAlertReportRequest(
    @SerializedName("tipo_plaga") val pestType: String,
    @SerializedName("cultivo_afectado") val cultivo: String,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String? = null,
    val departamento: String? = null,
    val municipio: String? = null,
    val severidad: String = "media"
)

data class PestAlertReportResponse(
    val message: String,
    val alerta: PestAlert
)

data class SuggestedPestProductsResponse(
    @SerializedName("alerta_id") val alertId: Int,
    @SerializedName("tipo_plaga") val pestType: String,
    @SerializedName("cultivo_afectado") val cultivo: String,
    @SerializedName("productos_sugeridos")
    val products: List<PestSuggestedProduct> = emptyList()
)

data class PestAlertInstallationRequest(
    @SerializedName("installation_id") val installationId: String,
    val latitud: Double,
    val longitud: Double
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
