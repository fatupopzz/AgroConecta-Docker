package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseCalculatorFormValidatorTest {

    @Test
    fun `conserva orden y mensajes del calculo de dosis`() {
        assertEquals("Selecciona un producto.", errorFor(product = ""))
        assertEquals("Selecciona un cultivo.", errorFor(crop = " "))
        assertEquals("Ingresa un tamaño de terreno válido.", errorFor(landArea = "0"))
    }

    @Test
    fun `acepta datos validos y expone el area numerica`() {
        assertNull(errorFor())
        assertEquals(
            2.5,
            checkNotNull(DoseCalculatorFormValidator.landAreaValue("2.5")),
            0.0
        )
    }

    @Test
    fun `identifica si existe producto seleccionado`() {
        assertFalse(DoseCalculatorFormValidator.isProductSelected(" "))
        assertTrue(DoseCalculatorFormValidator.isProductSelected("Fertilizante"))
    }

    private fun errorFor(
        product: String = "Fertilizante",
        crop: String = "Maíz",
        landArea: String = "2.5"
    ): String? = DoseCalculatorFormValidator.errorFor(product, crop, landArea)
}
