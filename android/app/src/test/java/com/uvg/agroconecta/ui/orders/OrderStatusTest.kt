package com.uvg.agroconecta.ui.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStatusTest {

    @Test
    fun `labels hide backend identifiers`() {
        assertEquals("En camino", orderStatusLabel("en_ruta"))
        assertEquals("Pendiente", orderStatusLabel(null))
        assertEquals("Por revisar", orderStatusLabel("por_revisar"))
    }

    @Test
    fun `progress grows with each step and is empty when canceled`() {
        assertEquals(0.25f, orderProgress("confirmado"))
        assertEquals(1f, orderProgress("entregado"))
        assertEquals(0f, orderProgress("cancelado"))
        assertEquals(0.1f, orderProgress("pendiente"))
    }

    @Test
    fun `filters separate active and delivered orders`() {
        assertTrue(OrderFilter.ACTIVE.matches("en_ruta"))
        assertFalse(OrderFilter.ACTIVE.matches("cancelado"))
        assertTrue(OrderFilter.DELIVERED.matches("entregado"))
        assertTrue(OrderFilter.ALL.matches("cancelado"))
    }
}
