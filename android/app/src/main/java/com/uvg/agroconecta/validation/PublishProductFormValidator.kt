package com.uvg.agroconecta.validation

object PublishProductFormValidator {

    fun errorFor(
        name: String,
        hasCategory: Boolean,
        price: String,
        stock: String
    ): String? = FormValidators.firstError(
        { FormValidators.required(name, "El nombre es obligatorio") },
        { "Seleccioná una categoría".takeUnless { hasCategory } },
        { FormValidators.positiveNumber(price, "Ingresá un precio válido") },
        { FormValidators.nonNegativeInteger(stock, "Ingresá un stock válido") }
    )

    fun priceValue(value: String): Double? =
        value.toDoubleOrNull()?.takeIf { it > 0 }

    fun stockValue(value: String): Int? =
        value.toIntOrNull()?.takeIf { it >= 0 }
}
