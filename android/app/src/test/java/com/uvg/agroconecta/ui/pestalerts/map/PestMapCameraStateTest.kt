package com.uvg.agroconecta.ui.pestalerts.map

import androidx.compose.runtime.saveable.SaverScope
import org.maplibre.android.geometry.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PestMapCameraStateTest {

    @Test
    fun `uses default location when there is no valid map content`() {
        val plan = buildInitialCameraPlan(
            userLocation = PestMapCoordinate(Double.NaN, -90.5),
            markers = listOf(marker(id = 1, latitude = 91.0, longitude = -90.5))
        )

        assertEquals(PestMapInitialCameraPlan.DefaultLocation, plan)
    }

    @Test
    fun `centers a single user coordinate`() {
        val coordinate = PestMapCoordinate(14.6349, -90.5069)

        assertEquals(
            PestMapInitialCameraPlan.Center(coordinate),
            buildInitialCameraPlan(userLocation = coordinate, markers = emptyList())
        )
    }

    @Test
    fun `treats overlapping user and alerts as one coordinate`() {
        val coordinate = PestMapCoordinate(14.6349, -90.5069)

        assertEquals(
            PestMapInitialCameraPlan.Center(coordinate),
            buildInitialCameraPlan(
                userLocation = coordinate,
                markers = listOf(marker(1, coordinate), marker(2, coordinate))
            )
        )
    }

    @Test
    fun `builds bounds containing user and distant valid alerts`() {
        val user = PestMapCoordinate(14.6349, -90.5069)
        val nearbyAlert = PestMapCoordinate(14.7, -90.4)
        val distantAlert = PestMapCoordinate(16.9, -89.9)

        assertEquals(
            PestMapInitialCameraPlan.Bounds(listOf(user, nearbyAlert, distantAlert)),
            buildInitialCameraPlan(
                userLocation = user,
                markers = listOf(
                    marker(1, nearbyAlert),
                    marker(2, distantAlert),
                    marker(id = 3, latitude = 14.0, longitude = 181.0)
                )
            )
        )
    }

    @Test
    fun `centers a single alert when user location is unavailable`() {
        val alert = PestMapCoordinate(15.5, -90.2)

        assertEquals(
            PestMapInitialCameraPlan.Center(alert),
            buildInitialCameraPlan(
                userLocation = null,
                markers = listOf(marker(1, alert))
            )
        )
    }

    @Test
    fun `camera state survives save and restore`() {
        val camera = PestMapCameraState(
            latitude = 15.5000,
            longitude = -89.9000,
            zoom = 13.5,
            bearing = 24.0,
            tilt = 35.0
        )
        val saverScope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

        val saved = with(PestMapCameraState.Saver) {
            saverScope.save(camera)
        }
        val restored = PestMapCameraState.Saver.restore(checkNotNull(saved))

        assertEquals(camera, restored)
    }

    @Test
    fun `Guatemala camera bounds include the country and reject distant coordinates`() {
        val bounds = PestMapConfig.guatemalaBounds()

        assertTrue(bounds.contains(LatLng(14.6349, -90.5069)))
        assertTrue(bounds.contains(LatLng(17.0, -89.9)))
        assertFalse(bounds.contains(LatLng(14.1, -87.2)))
    }

    private fun marker(
        id: Int,
        coordinate: PestMapCoordinate
    ) = marker(id, coordinate.latitude, coordinate.longitude)

    private fun marker(
        id: Int,
        latitude: Double,
        longitude: Double
    ) = PestMapMarker(
        alertId = id,
        latitude = latitude,
        longitude = longitude,
        pestType = "pulgon",
        crop = "Frijol"
    )
}
