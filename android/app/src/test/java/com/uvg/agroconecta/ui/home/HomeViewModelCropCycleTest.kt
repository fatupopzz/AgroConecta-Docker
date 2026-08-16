package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.CropPhase
import com.uvg.agroconecta.data.repository.CropCycleRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelCropCycleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads relevant crop cycle into home state`() {
        val expected = cycle()
        val viewModel = HomeViewModel(
            FakeCropCycleRepository(result = expected),
            FakeHomeProductCatalogRepository()
        )

        viewModel.loadRelevantCropCycle("token")

        assertEquals(expected, viewModel.uiState.value.cicloRelevante)
        assertFalse(viewModel.uiState.value.isLoadingCiclo)
    }

    @Test
    fun `keeps cycle empty when repository fails`() {
        val viewModel = HomeViewModel(
            FakeCropCycleRepository(error = IllegalStateException("sin conexión")),
            FakeHomeProductCatalogRepository()
        )

        viewModel.loadRelevantCropCycle("token")

        assertNull(viewModel.uiState.value.cicloRelevante)
        assertFalse(viewModel.uiState.value.isLoadingCiclo)
    }

    private fun cycle() = CropCycleResponse(
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

    private class FakeCropCycleRepository(
        private val result: CropCycleResponse? = null,
        private val error: Throwable? = null
    ) : CropCycleRepository {
        override suspend fun getRelevantCycle(token: String): CropCycleResponse? {
            error?.let { throw it }
            return result
        }
    }
}
