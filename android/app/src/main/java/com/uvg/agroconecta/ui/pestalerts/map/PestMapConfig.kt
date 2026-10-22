package com.uvg.agroconecta.ui.pestalerts.map

import com.uvg.agroconecta.BuildConfig

/** Configuración compartida por los componentes del mapa de alertas de plagas. */
object PestMapConfig {
    val styleUrl: String = BuildConfig.PEST_MAP_STYLE_URL

    const val INITIAL_LATITUDE = 14.6349
    const val INITIAL_LONGITUDE = -90.5069
    const val INITIAL_ZOOM = 12.0
    const val MIN_ZOOM = 3.0
    const val MAX_ZOOM = 19.0
    const val INITIAL_SEARCH_RADIUS_KM = 25.0
    const val MIN_LOCATION_UPDATE_DISTANCE_METERS = 15f
}
