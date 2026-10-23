package com.uvg.agroconecta.validation

data class LoginFieldErrors(
    val email: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean get() = email != null || password != null
}

/**
 * Validación del login campo por campo: cada mensaje se muestra debajo del
 * campo que hay que corregir, en lugar de un aviso general que no dice cuál.
 */
object LoginFormValidator {

    fun fieldErrors(email: String, password: String): LoginFieldErrors =
        LoginFieldErrors(
            email = FormValidators.firstError(
                { FormValidators.required(email, "Escribe tu correo") },
                { FormValidators.email(email, "Revisa tu correo, le falta la @") }
            ),
            password = FormValidators.required(password, "Escribe tu contraseña")
        )
}
