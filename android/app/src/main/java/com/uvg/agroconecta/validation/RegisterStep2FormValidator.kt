package com.uvg.agroconecta.validation

data class RegisterStep2Errors(
    val businessName: String? = null,
    val businessLocation: String? = null,
    val department: String? = null,
    val municipality: String? = null
) {
    val first: String? get() = businessName ?: businessLocation ?: department ?: municipality
}

object RegisterStep2FormValidator {

    fun fieldErrors(
        isDistributor: Boolean,
        businessName: String,
        hasBusinessLocation: Boolean,
        department: String,
        municipality: String
    ): RegisterStep2Errors = RegisterStep2Errors(
        businessName = if (isDistributor) {
            FormValidators.required(businessName, "Ingresa el nombre del negocio")
        } else {
            null
        },
        businessLocation = if (isDistributor && !hasBusinessLocation) {
            "Guarda la ubicación del negocio"
        } else {
            null
        },
        department = FormValidators.required(department, "Selecciona un departamento"),
        municipality = FormValidators.required(municipality, "Selecciona un municipio")
    )

    fun errorFor(
        isDistributor: Boolean,
        businessName: String,
        hasBusinessLocation: Boolean,
        department: String,
        municipality: String
    ): String? = fieldErrors(isDistributor, businessName, hasBusinessLocation, department, municipality).first
}
