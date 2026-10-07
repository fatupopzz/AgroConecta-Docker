package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegisterStep2FormValidatorTest {

    @Test
    fun `requiere nombre de negocio para distribuidores`() {
        assertEquals(
            "Ingresa el nombre del negocio",
            validationError(isDistributor = true, businessName = " ")
        )
    }

    @Test
    fun `agricultor no requiere nombre de negocio`() {
        assertNull(
            validationError(
                isDistributor = false,
                businessName = "",
                hasBusinessLocation = false
            )
        )
    }

    @Test
    fun `requiere ubicacion de negocio para distribuidores`() {
        assertEquals(
            "Guarda la ubicación del negocio",
            validationError(
                isDistributor = true,
                businessName = "Agrotienda",
                hasBusinessLocation = false
            )
        )
    }

    @Test
    fun `requiere departamento y municipio para cualquier cuenta`() {
        assertEquals(
            "Selecciona un departamento",
            validationError(department = "", municipality = "")
        )
        assertEquals(
            "Selecciona un municipio",
            validationError(municipality = " ")
        )
    }

    @Test
    fun `acepta segundo paso valido para distribuidor`() {
        assertNull(validationError(isDistributor = true, businessName = "Agrotienda"))
    }

    private fun validationError(
        isDistributor: Boolean = false,
        businessName: String = "",
        hasBusinessLocation: Boolean = true,
        department: String = "Guatemala",
        municipality: String = "Guatemala"
    ): String? = RegisterStep2FormValidator.errorFor(
        isDistributor = isDistributor,
        businessName = businessName,
        hasBusinessLocation = hasBusinessLocation,
        department = department,
        municipality = municipality
    )
}
