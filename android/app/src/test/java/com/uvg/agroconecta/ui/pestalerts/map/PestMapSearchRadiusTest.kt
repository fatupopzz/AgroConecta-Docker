package com.uvg.agroconecta.ui.pestalerts.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PestMapSearchRadiusTest {

    @Test
    fun `builds a closed geographic ring around search center`() {
        val searchRadius = PestMapSearchRadius(
            center = PestMapCoordinate(14.6349, -90.5069),
            radiusKm = 25.0
        )

        val ring = searchRadius.polygonRing()

        assertEquals(65, ring.size)
        assertEquals(ring.first(), ring.last())
        assertTrue(ring.all(PestMapCoordinate::isValid))
        assertEquals(14.8597, ring.first().latitude, 0.005)
    }

    @Test
    fun `larger radius produces a larger polygon`() {
        val center = PestMapCoordinate(14.6349, -90.5069)

        val tenKilometersNorth = PestMapSearchRadius(center, 10.0).polygonRing().first()
        val twentyFiveKilometersNorth = PestMapSearchRadius(center, 25.0).polygonRing().first()

        assertTrue(twentyFiveKilometersNorth.latitude > tenKilometersNorth.latitude)
    }

    @Test
    fun `invalid radius or center produces no polygon`() {
        assertTrue(
            PestMapSearchRadius(PestMapCoordinate(14.6349, -90.5069), 0.0)
                .polygonRing()
                .isEmpty()
        )
        assertTrue(
            PestMapSearchRadius(PestMapCoordinate(91.0, -90.5069), 25.0)
                .polygonRing()
                .isEmpty()
        )
    }
}
