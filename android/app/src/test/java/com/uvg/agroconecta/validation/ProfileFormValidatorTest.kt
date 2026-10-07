package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormValidatorTest {

    @Test
    fun `requiere nombre y telefono con el mensaje existente`() {
        assertEquals(
            "Nombre y teléfono son obligatorios.",
            ProfileFormValidator.errorFor(name = " ", phone = "55551234")
        )
        assertEquals(
            "Nombre y teléfono son obligatorios.",
            ProfileFormValidator.errorFor(name = "Ana", phone = "")
        )
    }

    @Test
    fun `acepta un perfil con nombre y telefono`() {
        assertNull(ProfileFormValidator.errorFor(name = "Ana", phone = "55551234"))
    }

    @Test
    fun `isValid refleja el resultado de las mismas reglas`() {
        assertFalse(ProfileFormValidator.isValid(name = "", phone = "55551234"))
        assertFalse(ProfileFormValidator.isValid(name = "Ana", phone = " "))
        assertTrue(ProfileFormValidator.isValid(name = "Ana", phone = "55551234"))
    }
}
