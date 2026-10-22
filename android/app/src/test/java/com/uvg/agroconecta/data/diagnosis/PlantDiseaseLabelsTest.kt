package com.uvg.agroconecta.data.diagnosis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlantDiseaseLabelsTest {

    // Los tests unitarios corren con el módulo app como directorio de trabajo.
    private val modelLabels = File("src/main/assets/ml/labels.txt").readLines().filter(String::isNotBlank)

    @Test
    fun `cada clase del modelo tiene nombre en español`() {
        assertEquals(19, modelLabels.size)
        val withoutTranslation = modelLabels.filterNot(PlantDiseaseLabels::hasTranslation)
        assertEquals(emptyList<String>(), withoutTranslation)
    }

    @Test
    fun `traduce cultivo y enfermedad`() {
        assertEquals(
            PlantDiseaseLabel(
                label = "Tomato___Late_blight",
                crop = "Tomate",
                disease = "Tizón tardío",
                isHealthy = false
            ),
            PlantDiseaseLabels.describe("Tomato___Late_blight")
        )
        assertEquals("Maíz", PlantDiseaseLabels.describe("Corn___Common_rust").crop)
        assertEquals("Roya común", PlantDiseaseLabels.describe("Corn___Common_rust").disease)
        assertEquals(
            "Araña roja",
            PlantDiseaseLabels.describe("Tomato___Spider_mites Two-spotted_spider_mite").disease
        )
    }

    @Test
    fun `el chile aparece como chile pimiento`() {
        val label = PlantDiseaseLabels.describe("Pepper,_bell___Bacterial_spot")

        assertEquals("Chile pimiento", label.crop)
        assertEquals("Mancha bacteriana", label.disease)
    }

    @Test
    fun `las hojas sanas se marcan como sanas en los cuatro cultivos`() {
        val healthy = modelLabels.filter { it.endsWith("___healthy") }.map(PlantDiseaseLabels::describe)

        assertEquals(listOf("Maíz", "Chile pimiento", "Papa", "Tomate"), healthy.map { it.crop })
        assertTrue(healthy.all { it.isHealthy && it.disease == "Hoja sana" })
        assertFalse(PlantDiseaseLabels.describe("Potato___Early_blight").isHealthy)
    }

    @Test
    fun `una etiqueta nueva sin traducir se muestra legible`() {
        val label = PlantDiseaseLabels.describe("Soybean___Frogeye_leaf_spot")

        assertEquals("Soybean", label.crop)
        assertEquals("Frogeye leaf spot", label.disease)
        assertEquals("Soybean___Frogeye_leaf_spot", label.label)
        assertFalse(PlantDiseaseLabels.hasTranslation("Soybean___Frogeye_leaf_spot"))
    }
}
