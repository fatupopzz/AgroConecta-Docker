package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.CropPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCropCycleCardTest {

    @Test
    fun `builds expected card title from crop name`() {
        assertEquals("Tu cultivo: Maíz", cropCycleTitle("maíz"))
    }

    @Test
    fun `normalizes recommended product before using it as catalog filter`() {
        assertEquals(
            "Fertilizante nitrogenado",
            normalizeCatalogQuery("  Fertilizante   nitrogenado  ")
        )
    }

    @Test
    fun `shows card only for farmer with an active phase`() {
        val cycle = cycleWithActivePhase()

        assertTrue(shouldShowCropCycleCard("agricultor", cycle))
        assertFalse(shouldShowCropCycleCard("distribuidor", cycle))
        assertFalse(shouldShowCropCycleCard("agricultor", cycle.copy(faseActual = null)))
        assertFalse(shouldShowCropCycleCard("agricultor", null))
    }

    private fun cycleWithActivePhase() = CropCycleResponse(
        cultivo = "maíz",
        mesActual = 8,
        faseActual = CropPhase(
            idCiclo = 3,
            fase = "Desarrollo vegetativo",
            mesInicio = 7,
            mesFin = 9,
            descripcion = "Crecimiento activo",
            productosRecomendados = listOf("Fertilizante nitrogenado")
        ),
        fasesActivas = emptyList(),
        proximaFase = null
    )
}
