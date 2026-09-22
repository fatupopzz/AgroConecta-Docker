package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest

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

    companion object {
        const val DEFAULT_RADIUS_KM = 25.0
    }
}

