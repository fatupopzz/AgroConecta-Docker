package com.uvg.agroconecta.validation

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringOrderFormValidatorTest {
    private val now = Instant.parse("2026-10-07T12:00:00Z")

    @Test
    fun `conserva orden y mensajes del pedido recurrente`() {
        assertEquals("Espera a que terminen de cargar los datos.", errorFor(isLoading = true))
        assertEquals("Agrega al menos un producto.", errorFor(hasProducts = false))
        assertEquals("El distribuidor no es válido.", errorFor(distributorId = 0))
        assertEquals("Revisa los productos y cantidades.", errorFor(hasInvalidProducts = true))
        assertEquals(
            "Ingresa una dirección de al menos cinco caracteres.",
            errorFor(address = "1234")
        )
        assertEquals(
            "Ingresa una fecha futura con formato aaaa-MM-dd HH:mm.",
            errorFor(nextDate = now)
        )
    }

    @Test
    fun `edicion no requiere direccion nueva y acepta fecha futura`() {
        assertNull(errorFor(isEditing = true, address = "", nextDate = now.plusSeconds(60)))
    }

    private fun errorFor(
        isLoading: Boolean = false,
        hasProducts: Boolean = true,
        distributorId: Int = 1,
        hasInvalidProducts: Boolean = false,
        isEditing: Boolean = false,
        address: String = "Parcela norte",
        nextDate: Instant? = now.plusSeconds(60)
    ): String? = RecurringOrderFormValidator.errorFor(
        isLoading = isLoading,
        hasProducts = hasProducts,
        distributorId = distributorId,
        hasInvalidProducts = hasInvalidProducts,
        isEditing = isEditing,
        address = address,
        nextDate = nextDate,
        now = now
    )
}
