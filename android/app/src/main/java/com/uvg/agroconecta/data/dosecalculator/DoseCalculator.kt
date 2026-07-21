package com.uvg.agroconecta.data.dosecalculator

object DoseCalculator {

    private const val HECTARES_PER_MANZANA = 0.6987

    fun convertToHectares(
        landArea: Double,
        landUnit: LandUnit
    ): Double {
        require(landArea > 0) {
            "El tamaño del terreno debe ser mayor que cero."
        }

        return when (landUnit) {
            LandUnit.HECTAREAS -> landArea
            LandUnit.MANZANAS -> landArea * HECTARES_PER_MANZANA
        }
    }

    fun calculate(
        reference: DoseReference,
        landArea: Double,
        landUnit: LandUnit
    ): DoseCalculationResult {
        val landAreaInHectares = convertToHectares(
            landArea = landArea,
            landUnit = landUnit
        )

        val calculatedDose =
            reference.dosePerHectare * landAreaInHectares

        return DoseCalculationResult(
            productName = reference.productName,
            cropName = reference.cropName,
            landAreaInHectares = landAreaInHectares,
            calculatedDose = calculatedDose,
            doseUnit = reference.doseUnit
        )
    }
}
