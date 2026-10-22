package com.uvg.agroconecta.data.diagnosis

/**
 * Traduce las etiquetas de PlantVillage (assets/ml/labels.txt) a los nombres que
 * ve el agricultor. Las etiquetas tienen la forma "<Cultivo>___<enfermedad>".
 */
object PlantDiseaseLabels {

    private const val SEPARATOR = "___"
    private const val HEALTHY = "healthy"

    private val CROPS = mapOf(
        "Corn" to "Maíz",
        // En Guatemala "chile pimiento" es el chile dulce que PlantVillage llama bell pepper.
        "Pepper,_bell" to "Chile pimiento",
        "Potato" to "Papa",
        "Tomato" to "Tomate"
    )

    private val DISEASES = mapOf(
        "Corn___Cercospora_leaf_spot Gray_leaf_spot" to "Mancha gris de la hoja (Cercospora)",
        "Corn___Common_rust" to "Roya común",
        "Corn___Northern_Leaf_Blight" to "Tizón foliar del norte",
        "Pepper,_bell___Bacterial_spot" to "Mancha bacteriana",
        "Potato___Early_blight" to "Tizón temprano",
        "Potato___Late_blight" to "Tizón tardío",
        "Tomato___Bacterial_spot" to "Mancha bacteriana",
        "Tomato___Early_blight" to "Tizón temprano",
        "Tomato___Late_blight" to "Tizón tardío",
        "Tomato___Leaf_Mold" to "Moho de la hoja",
        "Tomato___Septoria_leaf_spot" to "Mancha foliar por Septoria",
        "Tomato___Spider_mites Two-spotted_spider_mite" to "Araña roja",
        "Tomato___Target_Spot" to "Mancha diana",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to "Virus del rizado amarillo",
        "Tomato___Tomato_mosaic_virus" to "Virus del mosaico del tomate"
    )

    /** Etiquetas con traducción propia; los tests comprueban que cubran todo labels.txt. */
    internal fun hasTranslation(label: String): Boolean {
        val (crop, disease) = split(label)
        return crop in CROPS && (disease == HEALTHY || label in DISEASES)
    }

    fun describe(label: String): PlantDiseaseLabel {
        val (cropKey, diseaseKey) = split(label)
        val healthy = diseaseKey == HEALTHY
        // Si el modelo se reentrena con una clase nueva y nadie la traduce, se
        // muestra la etiqueta legible en vez de romper la pantalla.
        return PlantDiseaseLabel(
            label = label,
            crop = CROPS[cropKey] ?: readable(cropKey),
            disease = when {
                healthy -> "Hoja sana"
                else -> DISEASES[label] ?: readable(diseaseKey)
            },
            isHealthy = healthy
        )
    }

    private fun split(label: String): Pair<String, String> =
        label.substringBefore(SEPARATOR) to label.substringAfter(SEPARATOR, missingDelimiterValue = "")

    private fun readable(raw: String): String =
        raw.replace(',', ' ').replace('_', ' ').replace(Regex("\\s+"), " ").trim()
}
