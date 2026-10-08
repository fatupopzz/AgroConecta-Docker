package com.uvg.agroconecta.validation

object ProfileFormValidator {
    private const val REQUIRED_FIELDS_MESSAGE = "Nombre y teléfono son obligatorios."

    fun errorFor(name: String, phone: String): String? =
        FormValidators.firstError(
            { FormValidators.required(name, REQUIRED_FIELDS_MESSAGE) },
            { FormValidators.required(phone, REQUIRED_FIELDS_MESSAGE) }
        )

    fun isValid(name: String, phone: String): Boolean =
        errorFor(name, phone) == null
}
