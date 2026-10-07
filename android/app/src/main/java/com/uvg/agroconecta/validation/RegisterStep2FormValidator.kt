package com.uvg.agroconecta.validation

object RegisterStep2FormValidator {

    fun errorFor(
        isDistributor: Boolean,
        businessName: String,
        hasBusinessLocation: Boolean,
        department: String,
        municipality: String
    ): String? = FormValidators.firstError(
        {
            if (isDistributor) {
                FormValidators.required(businessName, "Ingresa el nombre del negocio")
            } else {
                null
            }
        },
        {
            if (isDistributor && !hasBusinessLocation) {
                "Guarda la ubicación del negocio"
            } else {
                null
            }
        },
        { FormValidators.required(department, "Selecciona un departamento") },
        { FormValidators.required(municipality, "Selecciona un municipio") }
    )
}
