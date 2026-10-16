package com.uvg.agroconecta.validation

data class RegisterStep1Errors(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val password: String? = null,
    val passwordConfirmation: String? = null
) {
    /** El primero en el orden del formulario, para anunciarlo o enfocarlo. */
    val first: String? get() = name ?: phone ?: email ?: password ?: passwordConfirmation
}

object RegisterStep1FormValidator {

    /**
     * Errores por campo: el formulario los muestra debajo de cada uno para que
     * la persona vea todo lo que falta de una vez, no de uno en uno.
     */
    fun fieldErrors(
        name: String,
        phone: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): RegisterStep1Errors = RegisterStep1Errors(
        name = FormValidators.required(name, "Ingresa tu nombre"),
        phone = FormValidators.firstError(
            { FormValidators.required(phone, "Ingresa tu número de teléfono") },
            {
                FormValidators.minimumLength(
                    phone,
                    minimum = 8,
                    message = "El teléfono debe tener al menos 8 dígitos"
                )
            }
        ),
        email = FormValidators.firstError(
            { FormValidators.required(email, "Ingresa tu correo electrónico") },
            { FormValidators.email(email, "Correo electrónico inválido") }
        ),
        password = FormValidators.minimumLength(
            password,
            minimum = 6,
            message = "La contraseña debe tener al menos 6 caracteres"
        ),
        passwordConfirmation = FormValidators.matches(
            password,
            passwordConfirmation,
            message = "Las contraseñas no coinciden"
        )
    )

    fun errorFor(
        name: String,
        phone: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): String? = fieldErrors(name, phone, email, password, passwordConfirmation).first
}
