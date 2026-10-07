package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.repository.CropCycleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelDistributorDistanceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<ApiService>()

    @Test
    fun `loads legacy distributor list without coordinates`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val distributors = listOf(distributor(1, "Agro Centro", null))
            coEvery { api.getVerifiedDistributors(null, null) } returns
                Response.success(distributors)
            val viewModel = viewModel(FakeHomeLocationProvider())

            viewModel.loadDistribuidores()
            advanceUntilIdle()

            assertEquals(distributors, viewModel.uiState.value.distribuidores)
            coVerify(exactly = 1) { api.getVerifiedDistributors(null, null) }
        }

    @Test
    fun `reloads distributors with current coordinates and keeps backend order`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val nearby = listOf(
                distributor(2, "Más cercano", 1.25),
                distributor(7, "Más lejano", 8.4)
            )
            coEvery { api.getVerifiedDistributors(14.6349, -90.5069) } returns
                Response.success(nearby)
            val viewModel = viewModel(
                FakeHomeLocationProvider(GeoCoordinates(14.6349, -90.5069))
            )

            viewModel.refreshLocation()
            advanceUntilIdle()

            assertEquals(nearby, viewModel.uiState.value.distribuidores)
            assertFalse(viewModel.uiState.value.isLoadingDistribuidores)
            coVerify(exactly = 1) { api.getVerifiedDistributors(14.6349, -90.5069) }
        }

    @Test
    fun `keeps previous distributor list when nearby refresh fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fallback = listOf(distributor(1, "Agro Centro", null))
            coEvery { api.getVerifiedDistributors(null, null) } returns
                Response.success(fallback)
            coEvery { api.getVerifiedDistributors(14.6349, -90.5069) } throws
                IOException("Sin conexión")
            val viewModel = viewModel(
                FakeHomeLocationProvider(GeoCoordinates(14.6349, -90.5069))
            )

            viewModel.loadDistribuidores()
            advanceUntilIdle()
            viewModel.refreshLocation()
            advanceUntilIdle()

            assertEquals(fallback, viewModel.uiState.value.distribuidores)
            assertFalse(viewModel.uiState.value.isLoadingDistribuidores)
        }

    private fun viewModel(locationProvider: FakeHomeLocationProvider) = HomeViewModel(
        api = api,
        cropCycleRepository = NoOpCropCycleRepository,
        productCatalogRepository = FakeHomeProductCatalogRepository(),
        locationProvider = locationProvider
    )

    private fun distributor(id: Int, name: String, distanceKm: Double?) = Distributor(
        id = id,
        nombreNegocio = name,
        departamento = "Guatemala",
        estadoVerificacion = "verificado",
        nombre = null,
        email = null,
        telefono = null,
        distanciaKm = distanceKm
    )

    private data object NoOpCropCycleRepository : CropCycleRepository {
        override suspend fun getRelevantCycle() = null
    }
}
