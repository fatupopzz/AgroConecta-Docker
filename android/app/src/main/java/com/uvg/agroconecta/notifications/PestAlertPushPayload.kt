package com.uvg.agroconecta.notifications

import java.util.Locale
import kotlin.math.roundToInt

data class PestAlertPushPayload(
    val alertId: Int,
    val pestType: String,
    val crop: String,
    val distanceKm: Double? = null,
    val title: String? = null,
    val body: String? = null
) {
    fun notificationTitle(): String = title
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: "Nueva alerta de ${pestType.displayName()}"

    fun notificationBody(): String = body
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: buildString {
            append("Se reportó ${pestType.displayName()} en $crop")
            distanceKm?.takeIf { it.isFinite() && it >= 0.0 }?.let { distance ->
                append(" a ${distance.displayDistance()}")
            }
            append('.')
        }

    companion object {
        private val supportedTypes = setOf("pest_alert", "alerta_plaga")

        fun from(
            data: Map<String, String>,
            notificationTitle: String? = null,
            notificationBody: String? = null
        ): PestAlertPushPayload? {
            val eventType = data.firstValue("type", "tipo")
                ?.trim()
                ?.lowercase(Locale.ROOT)
            if (eventType !in supportedTypes) return null

            val alertId = data.firstValue("alert_id", "id_alerta")
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?: return null
            val pestType = data.firstValue("pest_type", "tipo_plaga")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return null
            val crop = data.firstValue("crop", "cultivo_afectado")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return null

            return PestAlertPushPayload(
                alertId = alertId,
                pestType = pestType,
                crop = crop,
                distanceKm = data.firstValue("distance_km", "distancia_km")?.toDoubleOrNull(),
                title = data.firstValue("title", "titulo") ?: notificationTitle,
                body = data.firstValue("body", "mensaje") ?: notificationBody
            )
        }
    }
}

private fun Map<String, String>.firstValue(vararg keys: String): String? =
    keys.firstNotNullOfOrNull(::get)

private fun String.displayName(): String = when (trim().lowercase(Locale.ROOT)) {
    "pulgon", "pulgón" -> "pulgón"
    "mosca_blanca" -> "mosca blanca"
    "trips" -> "trips"
    "gusano_cogollero" -> "gusano cogollero"
    "hongos" -> "hongos"
    "acaros", "ácaros" -> "ácaros"
    else -> replace('_', ' ').trim().ifBlank { "plaga" }
}

private fun Double.displayDistance(): String {
    if (this < 1.0) return "${(this * 1_000).roundToInt()} m"
    val rounded = (this * 10.0).roundToInt() / 10.0
    val value = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "$value km"
}
