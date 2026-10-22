package com.uvg.agroconecta.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalogSearchNavigationTest {

    @Test
    fun `normaliza y codifica la consulta al abrir el catalogo`() {
        assertEquals(
            "catalog?query=abono%20org%C3%A1nico&category=0&calendar=false",
            catalogSearchRoute("  abono   orgánico  ")
        )
    }
}
