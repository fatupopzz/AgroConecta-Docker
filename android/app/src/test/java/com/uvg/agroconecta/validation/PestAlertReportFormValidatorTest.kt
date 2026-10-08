package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PestAlertReportFormValidatorTest {

    @Test
    fun `conserva orden y mensajes del reporte de plaga`() {
        assertEquals("Selecciona el tipo de plaga", errorFor(hasPestType = false))
        assertEquals("Selecciona el cultivo afectado", errorFor(crop = ""))
        assertEquals(
            "No se pudo obtener la ubicación del reporte",
            errorFor(hasLocation = false)
        )
    }

    @Test
    fun `canSubmit usa las mismas reglas`() {
        assertFalse(PestAlertReportFormValidator.canSubmit(false, "Maíz", true))
        assertTrue(PestAlertReportFormValidator.canSubmit(true, "Maíz", true))
        assertNull(errorFor())
    }

    private fun errorFor(
        hasPestType: Boolean = true,
        crop: String = "Maíz",
        hasLocation: Boolean = true
    ): String? = PestAlertReportFormValidator.errorFor(hasPestType, crop, hasLocation)
}
