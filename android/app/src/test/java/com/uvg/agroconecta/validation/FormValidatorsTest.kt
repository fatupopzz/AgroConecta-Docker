package com.uvg.agroconecta.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormValidatorsTest {

    @Test
    fun `required rechaza valores vacios o con espacios`() {
        assertEquals("Campo obligatorio", FormValidators.required("", "Campo obligatorio"))
        assertEquals("Campo obligatorio", FormValidators.required("   ", "Campo obligatorio"))
        assertNull(FormValidators.required("valor", "Campo obligatorio"))
    }

    @Test
    fun `minimumLength valida el limite indicado`() {
        assertEquals(
            "Muy corto",
            FormValidators.minimumLength("12345", minimum = 6, message = "Muy corto")
        )
        assertNull(FormValidators.minimumLength("123456", minimum = 6, message = "Muy corto"))
    }

    @Test
    fun `email conserva la regla actual de requerir arroba`() {
        assertEquals("Correo inválido", FormValidators.email("usuario", "Correo inválido"))
        assertNull(FormValidators.email("usuario@dominio.com", "Correo inválido"))
    }

    @Test
    fun `matches compara el valor con su confirmacion`() {
        assertEquals(
            "Los valores no coinciden",
            FormValidators.matches("secreto", "distinto", "Los valores no coinciden")
        )
        assertNull(FormValidators.matches("secreto", "secreto", "Los valores no coinciden"))
    }

    @Test
    fun `firstError devuelve el primer error y detiene las validaciones restantes`() {
        var finalValidationWasCalled = false

        val error = FormValidators.firstError(
            { null },
            { "Primer error" },
            {
                finalValidationWasCalled = true
                "Error posterior"
            }
        )

        assertEquals("Primer error", error)
        assertEquals(false, finalValidationWasCalled)
    }

    @Test
    fun `firstError devuelve null cuando todas las reglas se cumplen`() {
        assertNull(FormValidators.firstError({ null }, { null }))
    }
}
