package com.uvg.agroconecta.validation

object InventoryEditFormValidator {

    fun errorFor(price: String, stock: String): String? = FormValidators.firstError(
        { FormValidators.positiveNumber(price, "Ingresá un precio válido") },
        { FormValidators.nonNegativeInteger(stock, "Ingresá un stock válido") }
    )

    fun isValid(price: String, stock: String): Boolean =
        errorFor(price, stock) == null

    fun priceValue(value: String): Double? =
        value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }

    fun stockValue(value: String): Int? =
        value.toIntOrNull()?.takeIf { it >= 0 }
}
