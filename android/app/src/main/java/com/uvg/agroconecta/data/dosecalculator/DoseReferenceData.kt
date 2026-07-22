package com.uvg.agroconecta.data.dosecalculator

object DoseReferenceData {

    val references: List<DoseReference> = listOf(
        DoseReference(
            productName = "Fertilizante NPK 15-15-15",
            cropName = "Maíz",
            dosePerHectare = 200.0,
            doseUnit = "kg"
        ),
        DoseReference(
            productName = "Fertilizante NPK 15-15-15",
            cropName = "Frijol",
            dosePerHectare = 150.0,
            doseUnit = "kg"
        ),
        DoseReference(
            productName = "Fertilizante NPK 20-20-0",
            cropName = "Café",
            dosePerHectare = 250.0,
            doseUnit = "kg"
        ),
        DoseReference(
            productName = "Herbicida agrícola",
            cropName = "Maíz",
            dosePerHectare = 2.0,
            doseUnit = "L"
        ),
        DoseReference(
            productName = "Fungicida agrícola",
            cropName = "Tomate",
            dosePerHectare = 1.5,
            doseUnit = "L"
        )
    )

    fun getProducts(): List<String> {
        return references
            .map { it.productName }
            .distinct()
            .sorted()
    }

    fun getCropsForProduct(productName: String): List<String> {
        return references
            .filter { it.productName == productName }
            .map { it.cropName }
            .distinct()
            .sorted()
    }

    fun findReference(
        productName: String,
        cropName: String
    ): DoseReference? {
        return references.firstOrNull {
            it.productName == productName &&
                it.cropName == cropName
        }
    }
}
