package com.uvg.agroconecta.validation

object RegisterStep1FormValidator {

    fun errorFor(
        name: String,
        phone: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): String? = FormValidators.firstError(
        { FormValidators.required(name, "Ingresa tu nombre") },
        { FormValidators.required(phone, "Ingresa tu número de teléfono") },
        {
            FormValidators.minimumLength(
                phone,
                minimum = 8,
                message = "El teléfono debe tener al menos 8 dígitos"
            )
        },
        { FormValidators.required(email, "Ingresa tu correo electrónico") },
        { FormValidators.email(email, "Correo electrónico inválido") },
        {
            FormValidators.minimumLength(
                password,
                minimum = 6,
                message = "La contraseña debe tener al menos 6 caracteres"
            )
        },
        {
            FormValidators.matches(
                password,
                passwordConfirmation,
                message = "Las contraseñas no coinciden"
            )
        }
    )
}
