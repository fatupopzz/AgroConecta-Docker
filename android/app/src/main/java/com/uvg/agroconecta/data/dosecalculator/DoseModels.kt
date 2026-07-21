package com.uvg.agroconecta.data.dosecalculator

enum class LandUnit(
    val displayName: String
) {
    HECTAREAS("Hectáreas"),
    MANZANAS("Manzanas")
}

data class DoseReference(
    val productName: String,
    val cropName: String,
    val dosePerHectare: Double,
    val doseUnit: String
)

data class DoseCalculationResult(
    val productName: String,
    val cropName: String,
    val landAreaInHectares: Double,
    val calculatedDose: Double,
    val doseUnit: String
)
