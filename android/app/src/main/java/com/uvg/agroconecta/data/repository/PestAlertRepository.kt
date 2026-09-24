package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestSuggestedProduct

/**
 * Contrato de datos de HU-029. La implementación puede ser remota o local sin
 * que la pantalla conozca de dónde provienen las alertas.
 */
interface PestAlertRepository {
    suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = DEFAULT_RADIUS_KM
    ): List<PestAlert>

    suspend fun reportPest(request: PestAlertReportRequest): PestAlert

    suspend fun getSuggestedProducts(alertId: Int): List<PestSuggestedProduct>

    suspend fun syncPushInstallation(latitude: Double, longitude: Double)

    companion object {
        const val DEFAULT_RADIUS_KM = 25.0
    }
}
