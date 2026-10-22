package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

// Contrato de KAN-109: backend/docs/diagnoses.md

data class DiagnosisRequest(
    val cultivo: String,
    // Etiqueta del modelo (labels.txt), no el nombre traducido: es lo que usa
    // enfermedad_tratamiento del backend para encontrar productos.
    val enfermedad: String,
    val confianza: Double,
    // El backend no recibe la imagen, solo una referencia, y la foto se queda en
    // el teléfono. Se manda local://diagnostico/<uuid> hasta que exista subida.
    val foto: String,
    // Sin ubicación el backend guarda el diagnóstico, pero no permite avisar
    // a los vecinos. Gson omite los null, que el backend toma como "sin GPS".
    val latitud: Double?,
    val longitud: Double?
)

data class DiagnosisRecord(
    @SerializedName("id_diagnostico") val id: Int,
    val cultivo: String,
    val enfermedad: String,
    val confianza: Double,
    val foto: String,
    @SerializedName("id_alerta") val alertId: Int? = null
)

data class DiagnosisProductsResponse(
    @SerializedName("diagnostico_id") val diagnosisId: Int,
    val enfermedad: String,
    val productos: List<Product> = emptyList(),
    @SerializedName("ingredientes_activos") val ingredientesActivos: List<String> = emptyList(),
    @SerializedName("recomendacion_general") val recomendacionGeneral: String? = null
)

data class DiagnosisAlertRequest(val severidad: String = "media")

data class DiagnosisAlertResponse(val alerta: PestAlert?)
