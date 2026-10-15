package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryEditFormValidatorTest {

    @Test
    fun `rechaza precio vacio cero negativo no numerico o infinito`() {
        listOf("", "0", "-1", "texto", "Infinity").forEach { price ->
            assertEquals(
                "Ingresá un precio válido",
                InventoryEditFormValidator.errorFor(price, stock = "0")
            )
        }
    }

    @Test
    fun `rechaza stock vacio negativo decimal o no numerico`() {
        listOf("", "-1", "1.5", "texto").forEach { stock ->
            assertEquals(
                "Ingresá un stock válido",
                InventoryEditFormValidator.errorFor(price = "12.50", stock = stock)
            )
        }
    }

    @Test
    fun `acepta precio positivo y stock entero desde cero`() {
        assertNull(InventoryEditFormValidator.errorFor(price = "12.50", stock = "0"))
        assertNull(InventoryEditFormValidator.errorFor(price = "0.01", stock = "25"))
        assertTrue(InventoryEditFormValidator.isValid(price = "7.75", stock = "10"))
    }

    @Test
    fun `expone valores numericos solo cuando son validos`() {
        assertEquals(
            12.5,
            checkNotNull(InventoryEditFormValidator.priceValue("12.5")),
            0.0
        )
        assertEquals(0, InventoryEditFormValidator.stockValue("0"))
        assertNull(InventoryEditFormValidator.priceValue("Infinity"))
        assertNull(InventoryEditFormValidator.stockValue("1.5"))
        assertFalse(InventoryEditFormValidator.isValid(price = "0", stock = "0"))
    }
}
