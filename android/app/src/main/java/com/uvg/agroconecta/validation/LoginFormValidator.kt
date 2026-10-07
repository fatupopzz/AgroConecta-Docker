package com.uvg.agroconecta.validation

object LoginFormValidator {
    private const val REQUIRED_FIELDS_MESSAGE = "Completa todos los campos"

    fun errorFor(email: String, password: String): String? =
        FormValidators.firstError(
            { FormValidators.required(email, REQUIRED_FIELDS_MESSAGE) },
            { FormValidators.required(password, REQUIRED_FIELDS_MESSAGE) }
        )
}
