package com.uvg.agroconecta.data.diagnosis

/**
 * Clasifica la foto de una hoja con el modelo que viene dentro de la app, así
 * que funciona sin conexión. Es una interfaz para poder reemplazarla por un
 * mock en los tests: el intérprete de TFLite necesita librerías nativas que no
 * existen en la JVM de los tests unitarios.
 */
interface PlantDiseaseClassifier {
    /**
     * Recibe la foto ya comprimida a JPEG, la misma que deja ProductImageReader.
     * Corre fuera del hilo principal. Lanza IllegalArgumentException si los
     * bytes no son una imagen legible.
     */
    suspend fun classify(jpegBytes: ByteArray): PlantDiagnosisResult
}

/** Clase del modelo lista para mostrar. [label] es la etiqueta original, la que se manda al backend. */
data class PlantDiseaseLabel(
    val label: String,
    val crop: String,
    val disease: String,
    val isHealthy: Boolean
)

sealed interface PlantDiagnosisResult {
    data class Diagnosed(
        val label: PlantDiseaseLabel,
        val confidence: Float
    ) : PlantDiagnosisResult

    /** La mejor clase no llegó a [PlantDiagnosisRules.MIN_CONFIDENCE]: hay que pedir otra foto. */
    data class UnclearPhoto(val bestConfidence: Float) : PlantDiagnosisResult
}

object PlantDiagnosisRules {
    /**
     * Debajo de esto el resultado se descarta en lugar de mostrar un diagnóstico
     * dudoso. Con las fotos de validación deja pasar el 94,5 % y, entre esas,
     * el modelo acierta el 96,3 % (ver ml/README.md).
     */
    const val MIN_CONFIDENCE = 0.6f

    fun interpret(scores: FloatArray, labels: List<String>): PlantDiagnosisResult {
        require(scores.size == labels.size) {
            "El modelo devolvió ${scores.size} clases pero labels.txt tiene ${labels.size}"
        }
        require(scores.isNotEmpty()) { "El modelo no devolvió clases" }

        val best = scores.indices.maxByOrNull { scores[it] } ?: 0
        val confidence = scores[best]
        // NaN no es menor que el umbral, así que se descarta aparte.
        if (confidence.isNaN() || confidence < MIN_CONFIDENCE) {
            return PlantDiagnosisResult.UnclearPhoto(bestConfidence = confidence)
        }
        return PlantDiagnosisResult.Diagnosed(
            label = PlantDiseaseLabels.describe(labels[best]),
            confidence = confidence
        )
    }
}
