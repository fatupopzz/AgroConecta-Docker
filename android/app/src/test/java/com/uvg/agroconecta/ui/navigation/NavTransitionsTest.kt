package com.uvg.agroconecta.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTransitionsTest {

    @Test
    fun `las pestanas de la barra inferior se reconocen como tales`() {
        assertTrue(isTabRoute(Screen.Home.route))
        assertTrue(isTabRoute(Screen.OrderHistory.route))
        assertTrue(isTabRoute(Screen.Profile.route))
        assertTrue(isTabRoute(Screen.DoseCalculator.route))
        assertTrue(isTabRoute(Screen.PublishProduct.route))
    }

    @Test
    fun `las pantallas que se abren encima no son pestanas`() {
        assertFalse(isTabRoute(Screen.Cart.route))
        assertFalse(isTabRoute(Screen.Catalog.route))
        assertFalse(isTabRoute(Screen.Favorites.route))
        assertFalse(isTabRoute(null))
    }
}
