package com.uvg.agroconecta.validation

object PestAlertReportFormValidator {

    fun errorFor(hasPestType: Boolean, crop: String, hasLocation: Boolean): String? =
        FormValidators.firstError(
            { "Selecciona el tipo de plaga".takeUnless { hasPestType } },
            { FormValidators.required(crop, "Selecciona el cultivo afectado") },
            { "No se pudo obtener la ubicación del reporte".takeUnless { hasLocation } }
        )

    fun canSubmit(hasPestType: Boolean, crop: String, hasLocation: Boolean): Boolean =
        errorFor(hasPestType, crop, hasLocation) == null
}
