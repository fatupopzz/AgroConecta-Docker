package com.uvg.agroconecta.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleNavigationTest {
    @Test
    fun `agricultor conserva acceso y ruta de calculadora`() {
        val access = RoleNavigationAccess.fromBackend(" AGRICULTOR ")

        assertTrue(access.isFarmer)
        assertFalse(access.isDistributor)
        assertEquals(Screen.DoseCalculator.route, access.addTabRoute)
    }

    @Test
    fun `distribuidor conserva acceso y ruta de publicacion`() {
        val access = RoleNavigationAccess.fromBackend("DISTRIBUIDOR")

        assertFalse(access.isFarmer)
        assertTrue(access.isDistributor)
        assertEquals(Screen.PublishProduct.route, access.addTabRoute)
    }

    @Test
    fun `rol desconocido no recibe accesos exclusivos`() {
        val access = RoleNavigationAccess.fromBackend("cliente")

        assertFalse(access.isFarmer)
        assertFalse(access.isDistributor)
        assertEquals(Screen.Home.route, access.addTabRoute)
    }
}
