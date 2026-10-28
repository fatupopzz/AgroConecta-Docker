package com.uvg.agroconecta.ui.pestalerts.map

import com.uvg.agroconecta.data.models.PestAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PestMapMarkerTest {

    @Test
    fun `maps alert fields to a geographic pest marker`() {
        val alert = alert(latitude = 14.6349, longitude = -90.5069)

        assertEquals(
            PestMapMarker(
                alertId = 41,
                latitude = 14.6349,
                longitude = -90.5069,
                pestType = "mosca_blanca",
                crop = "Tomate"
            ),
            alert.toPestMapMarker()
        )
    }

    @Test
    fun `rejects coordinates outside geographic bounds`() {
        assertNull(alert(latitude = 90.1, longitude = -90.5069).toPestMapMarker())
        assertNull(alert(latitude = 14.6349, longitude = -180.1).toPestMapMarker())
        assertNull(alert(latitude = Double.NaN, longitude = -90.5069).toPestMapMarker())
    }

    private fun alert(latitude: Double, longitude: Double) = PestAlert(
        id = 41,
        pestType = "mosca_blanca",
        cultivo = "Tomate",
        latitud = latitude,
        longitud = longitude,
        reportedAt = "2026-10-27T12:00:00Z"
    )
}
