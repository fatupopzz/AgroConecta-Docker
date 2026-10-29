package com.uvg.agroconecta.ui.pestalerts.map

import android.util.Log
import com.uvg.agroconecta.data.models.PestAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
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
        ShadowLog.clear()

        assertNull(alert(latitude = 90.1, longitude = -90.5069).toPestMapMarker())
        assertNull(alert(latitude = 14.6349, longitude = -180.1).toPestMapMarker())
        assertNull(alert(latitude = Double.NaN, longitude = -90.5069).toPestMapMarker())

        val warnings = ShadowLog.getLogsForTag(PEST_MAP_MARKER_LOG_TAG)
        assertEquals(3, warnings.size)
        assertTrue(warnings.all { it.type == Log.WARN && it.msg.contains("pest alert 41") })
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
