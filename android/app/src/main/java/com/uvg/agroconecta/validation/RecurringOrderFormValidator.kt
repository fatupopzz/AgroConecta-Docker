package com.uvg.agroconecta.validation

import java.time.Instant

object RecurringOrderFormValidator {

    fun errorFor(
        isLoading: Boolean,
        hasProducts: Boolean,
        distributorId: Int,
        hasInvalidProducts: Boolean,
        isEditing: Boolean,
        address: String,
        nextDate: Instant?,
        now: Instant = Instant.now()
    ): String? = when {
        isLoading -> "Espera a que terminen de cargar los datos."
        !hasProducts -> "Agrega al menos un producto."
        distributorId <= 0 -> "El distribuidor no es válido."
        hasInvalidProducts -> "Revisa los productos y cantidades."
        !isEditing && address.trim().length < 5 ->
            "Ingresa una dirección de al menos cinco caracteres."
        nextDate == null || !nextDate.isAfter(now) ->
            "Ingresa una fecha futura con formato aaaa-MM-dd HH:mm."
        else -> null
    }
}
