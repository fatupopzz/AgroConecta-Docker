package com.uvg.agroconecta.validation

/**
 * Validaciones de campos reutilizables y libres de dependencias de Android.
 *
 * Cada regla devuelve el mensaje recibido cuando el valor no es válido, o
 * `null` cuando la validación se cumple. Esto permite que cada formulario
 * conserve sus mensajes mientras comparte la lógica de validación.
 */
object FormValidators {

    fun required(value: String, message: String): String? =
        message.takeIf { value.isBlank() }

    fun minimumLength(value: String, minimum: Int, message: String): String? =
        message.takeIf { value.length < minimum }

    fun email(value: String, message: String): String? =
        message.takeUnless { value.contains("@") }

    fun matches(value: String, confirmation: String, message: String): String? =
        message.takeUnless { value == confirmation }

    fun firstError(vararg validations: () -> String?): String? {
        validations.forEach { validation ->
            validation()?.let { return it }
        }
        return null
    }
}
