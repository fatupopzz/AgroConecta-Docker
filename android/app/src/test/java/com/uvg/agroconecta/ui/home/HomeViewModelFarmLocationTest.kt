package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.FarmLocation
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.FarmLocationSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class HomeViewModelFarmLocationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock estricto: si se pidieran distribuidores con otras coordenadas, falla.
    private val api = mockk<ApiService>()

    private data object NoCycle : CropCycleRepository {
        override suspend fun getRelevantCycle() = null
    }

    private class SavedFarm(private val location: FarmLocation?) : FarmLocationSource {
        override suspend fun savedLocation(): FarmLocation? = location
    }

    private fun viewModel(farm: FarmLocationSource, gps: FakeHomeLocationProvider) = HomeViewModel(
        api = api,
        cropCycleRepository = NoCycle,
        productCatalogRepository = FakeHomeProductCatalogRepository(),
        locationProvider = gps,
        farmLocations = farm
    )

    @Test
    fun `con la finca marcada ordena los distribuidores desde ahí sin usar el GPS`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { api.getVerifiedDistributors(14.76123, -91.00123) } returns Response.success(emptyList())
            val gps = FakeHomeLocationProvider(coordinates = GeoCoordinates(1.0, 1.0))
            val viewModel = viewModel(
                SavedFarm(FarmLocation("Chimaltenango", "Tecpán Guatemala", null, 14.76123, -91.00123)),
                gps
            )

            val used = viewModel.useSavedFarmLocation()

            assertTrue(used)
            assertEquals(GeoCoordinates(14.76123, -91.00123), viewModel.uiState.value.location)
            assertEquals(0, gps.requests)
            coVerify(exactly = 1) { api.getVerifiedDistributors(14.76123, -91.00123) }
        }

    @Test
    fun `sin coordenadas guardadas deja que la pantalla pida el GPS`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel(SavedFarm(FarmLocation("Sololá", "Panajachel")), FakeHomeLocationProvider())

            assertFalse(viewModel.useSavedFarmLocation())
            assertEquals(null, viewModel.uiState.value.location)
        }
}
