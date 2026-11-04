package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.CropPhase
import com.uvg.agroconecta.data.repository.CropCycleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class HomeViewModelCropCycleTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // El ciclo de cultivo sale del repositorio, no del ApiService; el mock
    // estricto deja constancia de que aqui no debe haber llamadas de red.
    private val api = mockk<ApiService>()

    @Test
    fun `loads relevant crop cycle into home state`() {
        val expected = cycle()
        val viewModel = HomeViewModel(
            api,
            FakeCropCycleRepository(result = expected),
            FakeHomeProductCatalogRepository(),
            FakeHomeLocationProvider()
        )

        viewModel.loadRelevantCropCycle()

        assertEquals(expected, viewModel.uiState.value.cicloRelevante)
        assertFalse(viewModel.uiState.value.isLoadingCiclo)
    }

    @Test
    fun `keeps cycle empty when repository fails`() {
        val viewModel = HomeViewModel(
            api,
            FakeCropCycleRepository(error = IllegalStateException("sin conexión")),
            FakeHomeProductCatalogRepository(),
            FakeHomeLocationProvider()
        )

        viewModel.loadRelevantCropCycle()

        assertNull(viewModel.uiState.value.cicloRelevante)
        assertFalse(viewModel.uiState.value.isLoadingCiclo)
    }

    @Test
    fun `farmer role loads personalized sections`() {
        val strictApi = mockk<ApiService>()
        coEvery { strictApi.getRecommendedProducts() } returns Response.success(emptyList())
        coEvery { strictApi.getCategories() } returns Response.success(emptyList())
        coEvery { strictApi.getVerifiedDistributors(null, null) } returns Response.success(emptyList())
        val cycles = FakeCropCycleRepository(result = cycle())
        val viewModel = HomeViewModel(
            strictApi,
            cycles,
            FakeHomeProductCatalogRepository(),
            FakeHomeLocationProvider()
        )

        viewModel.init(" AGRICULTOR ")

        assertEquals(1, cycles.requests)
        coVerify(exactly = 1) { strictApi.getRecommendedProducts() }
        coVerify(exactly = 1) { strictApi.getCategories() }
        coVerify(exactly = 1) { strictApi.getVerifiedDistributors(null, null) }
        confirmVerified(strictApi)
    }

    @Test
    fun `unknown role skips farmer sections`() {
        val strictApi = mockk<ApiService>()
        coEvery { strictApi.getCategories() } returns Response.success(emptyList())
        coEvery { strictApi.getVerifiedDistributors(null, null) } returns Response.success(emptyList())
        val cycles = FakeCropCycleRepository(result = cycle())
        val viewModel = HomeViewModel(
            strictApi,
            cycles,
            FakeHomeProductCatalogRepository(),
            FakeHomeLocationProvider()
        )

        viewModel.init("cliente")

        assertEquals(0, cycles.requests)
        coVerify(exactly = 0) { strictApi.getRecommendedProducts() }
        coVerify(exactly = 1) { strictApi.getCategories() }
        coVerify(exactly = 1) { strictApi.getVerifiedDistributors(null, null) }
        confirmVerified(strictApi)
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
        var requests: Int = 0

        override suspend fun getRelevantCycle(): CropCycleResponse? {
            requests += 1
            error?.let { throw it }
            return result
        }
    }
}
