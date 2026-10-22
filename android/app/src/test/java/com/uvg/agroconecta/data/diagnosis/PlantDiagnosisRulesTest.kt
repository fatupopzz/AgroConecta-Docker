package com.uvg.agroconecta.data.diagnosis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantDiagnosisRulesTest {

    private val labels = listOf("Potato___Early_blight", "Potato___Late_blight", "Potato___healthy")

    @Test
    fun `diagnostica la clase con mayor confianza`() {
        val result = PlantDiagnosisRules.interpret(floatArrayOf(0.1f, 0.85f, 0.05f), labels)

        assertEquals(
            PlantDiagnosisResult.Diagnosed(
                label = PlantDiseaseLabel("Potato___Late_blight", "Papa", "Tizón tardío", isHealthy = false),
                confidence = 0.85f
            ),
            result
        )
    }

    @Test
    fun `con confianza menor al umbral pide otra foto`() {
        val result = PlantDiagnosisRules.interpret(floatArrayOf(0.59f, 0.3f, 0.11f), labels)

        assertEquals(PlantDiagnosisResult.UnclearPhoto(bestConfidence = 0.59f), result)
    }

    @Test
    fun `exactamente en el umbral ya es un diagnóstico`() {
        val result = PlantDiagnosisRules.interpret(
            floatArrayOf(PlantDiagnosisRules.MIN_CONFIDENCE, 0.3f, 0.1f),
            labels
        )

        assertTrue(result is PlantDiagnosisResult.Diagnosed)
        assertEquals(0.6f, PlantDiagnosisRules.MIN_CONFIDENCE)
    }

    @Test
    fun `una hoja sana con buena confianza se diagnostica como sana`() {
        val result = PlantDiagnosisRules.interpret(floatArrayOf(0.02f, 0.03f, 0.95f), labels)

        assertTrue((result as PlantDiagnosisResult.Diagnosed).label.isHealthy)
    }

    @Test
    fun `un resultado NaN se trata como foto no clara`() {
        val result = PlantDiagnosisRules.interpret(floatArrayOf(Float.NaN, 0.2f, 0.1f), labels)

        assertTrue(result is PlantDiagnosisResult.UnclearPhoto)
    }

    @Test
    fun `falla si el modelo y labels txt no coinciden`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlantDiagnosisRules.interpret(floatArrayOf(0.9f, 0.1f), labels)
        }
    }
}
