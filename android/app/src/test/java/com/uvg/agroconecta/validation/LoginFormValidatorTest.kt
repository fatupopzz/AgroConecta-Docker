package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginFormValidatorTest {

    @Test
    fun `requiere correo y contrasena`() {
        assertEquals(
            "Completa todos los campos",
            LoginFormValidator.errorFor(email = "", password = "secreto")
        )
        assertEquals(
            "Completa todos los campos",
            LoginFormValidator.errorFor(email = "usuario@correo.com", password = " ")
        )
    }

    @Test
    fun `acepta credenciales no vacias`() {
        assertNull(
            LoginFormValidator.errorFor(
                email = "usuario@correo.com",
                password = "secreto"
            )
        )
    }
}
