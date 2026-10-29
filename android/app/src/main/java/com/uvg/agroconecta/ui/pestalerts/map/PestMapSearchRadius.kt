package com.uvg.agroconecta.ui.pestalerts.map

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal data class PestMapSearchRadius(
    val center: PestMapCoordinate,
    val radiusKm: Double
) {
    val isValid: Boolean
        get() = center.isValid && radiusKm.isFinite() && radiusKm > 0.0 && radiusKm <= 100.0

    fun polygonRing(segments: Int = DEFAULT_SEGMENTS): List<PestMapCoordinate> {
        if (!isValid || segments < MIN_SEGMENTS) return emptyList()

        val centerLatitude = Math.toRadians(center.latitude)
        val centerLongitude = Math.toRadians(center.longitude)
        val angularRadius = radiusKm / EARTH_RADIUS_KM
        val ring = List(segments) { index ->
            val bearing = 2.0 * PI * index / segments
            val latitude = asin(
                sin(centerLatitude) * cos(angularRadius) +
                    cos(centerLatitude) * sin(angularRadius) * cos(bearing)
            )
            val longitude = centerLongitude + atan2(
                sin(bearing) * sin(angularRadius) * cos(centerLatitude),
                cos(angularRadius) - sin(centerLatitude) * sin(latitude)
            )
            PestMapCoordinate(
                latitude = Math.toDegrees(latitude),
                longitude = normalizeLongitude(Math.toDegrees(longitude))
            )
        }
        return ring + ring.first()
    }

    private fun normalizeLongitude(longitude: Double): Double =
        ((longitude + 540.0) % 360.0) - 180.0

    private companion object {
        const val EARTH_RADIUS_KM = 6_371.0
        const val DEFAULT_SEGMENTS = 64
        const val MIN_SEGMENTS = 4
    }
}
