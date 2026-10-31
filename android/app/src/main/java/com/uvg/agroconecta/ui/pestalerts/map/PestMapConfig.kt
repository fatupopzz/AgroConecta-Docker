package com.uvg.agroconecta.ui.pestalerts.map

import com.uvg.agroconecta.BuildConfig
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import org.maplibre.android.geometry.LatLngBounds

/** Configuración compartida por los componentes del mapa de alertas de plagas. */
object PestMapConfig {
    val styleUrl: String = BuildConfig.PEST_MAP_STYLE_URL

    const val INITIAL_LATITUDE = 15.7835
    const val INITIAL_LONGITUDE = -90.2308
    const val INITIAL_ZOOM = 6.5
    const val SELECTED_ALERT_ZOOM = 14.0
    const val MIN_ZOOM = 5.5
    const val MAX_ZOOM = 19.0
    const val GUATEMALA_NORTH = 18.0
    const val GUATEMALA_EAST = -88.0
    const val GUATEMALA_SOUTH = 13.6
    const val GUATEMALA_WEST = -92.4
    const val INITIAL_CAMERA_PADDING_DP = 56
    const val INITIAL_SEARCH_RADIUS_KM = 25.0
    const val SEARCH_CENTER_CHANGE_THRESHOLD_METERS = 1_000.0
    const val MIN_LOCATION_UPDATE_DISTANCE_METERS =
        CurrentLocationProvider.MIN_LOCATION_UPDATE_DISTANCE_METERS

    fun guatemalaBounds(): LatLngBounds = LatLngBounds.from(
        GUATEMALA_NORTH,
        GUATEMALA_EAST,
        GUATEMALA_SOUTH,
        GUATEMALA_WEST
    )
}
