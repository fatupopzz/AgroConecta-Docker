package com.uvg.agroconecta.validation

object DoseCalculatorFormValidator {

    fun errorFor(product: String, crop: String, landArea: String): String? =
        FormValidators.firstError(
            { FormValidators.required(product, "Selecciona un producto.") },
            { FormValidators.required(crop, "Selecciona un cultivo.") },
            {
                FormValidators.positiveNumber(
                    landArea,
                    "Ingresa un tamaño de terreno válido."
                )
            }
        )

    fun isProductSelected(product: String): Boolean = product.isNotBlank()

    fun landAreaValue(value: String): Double? =
        value.toDoubleOrNull()?.takeIf { it > 0 }
}
