package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginFormValidatorTest {

    @Test
    fun `marca cada campo vacio por separado`() {
        val errors = LoginFormValidator.fieldErrors(email = "", password = " ")

        assertEquals("Escribe tu correo", errors.email)
        assertEquals("Escribe tu contraseña", errors.password)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun `avisa cuando al correo le falta la arroba`() {
        val errors = LoginFormValidator.fieldErrors(email = "usuario", password = "secreto")

        assertEquals("Revisa tu correo, le falta la @", errors.email)
        assertNull(errors.password)
    }

    @Test
    fun `acepta credenciales completas`() {
        val errors = LoginFormValidator.fieldErrors(email = "usuario@correo.com", password = "secreto")

        assertFalse(errors.hasErrors)
    }
}
