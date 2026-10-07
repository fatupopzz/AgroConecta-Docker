package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegisterStep1FormValidatorTest {

    @Test
    fun `conserva el orden y los mensajes de las reglas de registro`() {
        assertEquals("Ingresa tu nombre", validationError(name = " "))
        assertEquals("Ingresa tu número de teléfono", validationError(phone = ""))
        assertEquals(
            "El teléfono debe tener al menos 8 dígitos",
            validationError(phone = "1234567")
        )
        assertEquals("Ingresa tu correo electrónico", validationError(email = " "))
        assertEquals("Correo electrónico inválido", validationError(email = "usuario"))
        assertEquals(
            "La contraseña debe tener al menos 6 caracteres",
            validationError(password = "12345", passwordConfirmation = "12345")
        )
        assertEquals(
            "Las contraseñas no coinciden",
            validationError(passwordConfirmation = "distinta")
        )
    }

    @Test
    fun `devuelve el primer error cuando varios campos son invalidos`() {
        assertEquals(
            "Ingresa tu nombre",
            validationError(name = "", phone = "", email = "")
        )
    }

    @Test
    fun `acepta un primer paso valido`() {
        assertNull(validationError())
    }

    private fun validationError(
        name: String = "Ana",
        phone: String = "55551234",
        email: String = "ana@correo.com",
        password: String = "secreto",
        passwordConfirmation: String = password
    ): String? = RegisterStep1FormValidator.errorFor(
        name = name,
        phone = phone,
        email = email,
        password = password,
        passwordConfirmation = passwordConfirmation
    )
}
