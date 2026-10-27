package com.uvg.agroconecta.ui.pestalerts.map

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

@Immutable
data class PestMapCameraState(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0
) {
    fun toCameraPosition(): CameraPosition = CameraPosition.Builder()
        .target(LatLng(latitude, longitude))
        .zoom(zoom)
        .bearing(bearing)
        .tilt(tilt)
        .build()

    companion object {
        fun initial() = PestMapCameraState(
            latitude = PestMapConfig.INITIAL_LATITUDE,
            longitude = PestMapConfig.INITIAL_LONGITUDE,
            zoom = PestMapConfig.INITIAL_ZOOM
        )

        fun from(position: CameraPosition): PestMapCameraState {
            val target = position.target ?: LatLng(
                PestMapConfig.INITIAL_LATITUDE,
                PestMapConfig.INITIAL_LONGITUDE
            )
            return PestMapCameraState(
                latitude = target.latitude,
                longitude = target.longitude,
                zoom = position.zoom,
                bearing = position.bearing,
                tilt = position.tilt
            )
        }

        val Saver: Saver<PestMapCameraState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.latitude,
                    state.longitude,
                    state.zoom,
                    state.bearing,
                    state.tilt
                )
            },
            restore = { values ->
                PestMapCameraState(
                    latitude = values[0],
                    longitude = values[1],
                    zoom = values[2],
                    bearing = values[3],
                    tilt = values[4]
                )
            }
        )
    }
}
