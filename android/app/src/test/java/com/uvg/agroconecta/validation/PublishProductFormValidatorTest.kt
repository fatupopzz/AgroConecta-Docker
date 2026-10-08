package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublishProductFormValidatorTest {

    @Test
    fun `conserva orden y mensajes de publicacion`() {
        assertEquals("El nombre es obligatorio", errorFor(name = ""))
        assertEquals("Seleccioná una categoría", errorFor(hasCategory = false))
        assertEquals("Ingresá un precio válido", errorFor(price = "0"))
        assertEquals("Ingresá un stock válido", errorFor(stock = "-1"))
    }

    @Test
    fun `acepta datos validos y expone valores numericos`() {
        assertNull(errorFor())
        assertEquals(
            12.5,
            checkNotNull(PublishProductFormValidator.priceValue("12.5")),
            0.0
        )
        assertEquals(0, PublishProductFormValidator.stockValue("0"))
    }

    private fun errorFor(
        name: String = "Fertilizante",
        hasCategory: Boolean = true,
        price: String = "12.5",
        stock: String = "8"
    ): String? = PublishProductFormValidator.errorFor(name, hasCategory, price, stock)
}
