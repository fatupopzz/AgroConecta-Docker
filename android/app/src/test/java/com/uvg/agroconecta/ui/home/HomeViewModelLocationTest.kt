package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.repository.CropCycleRepository
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class HomeViewModelLocationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<ApiService>()

    @Test
    fun `stores current coordinates without affecting dashboard content`() {
        val provider = FakeHomeLocationProvider(
            coordinates = GeoCoordinates(14.6349, -90.5069)
        )
        val viewModel = viewModel(provider)

        viewModel.refreshLocation()

        assertEquals(GeoCoordinates(14.6349, -90.5069), viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.isLocating)
        assertNull(viewModel.uiState.value.locationErrorMessage)
        assertEquals(1, provider.requests)
    }

    @Test
    fun `keeps dashboard available when GPS has no location`() {
        val viewModel = viewModel(FakeHomeLocationProvider(coordinates = null))

        viewModel.refreshLocation()

        assertNull(viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.isLocating)
        assertEquals(
            "No se pudo determinar tu ubicación. Verifica que el GPS esté activo",
            viewModel.uiState.value.locationErrorMessage
        )
    }

    @Test
    fun `permission denial is isolated from the general dashboard error`() {
        val viewModel = viewModel(FakeHomeLocationProvider())

        viewModel.onLocationPermissionDenied()

        assertNull(viewModel.uiState.value.location)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(
            "Activa el permiso de ubicación para ver distancias a distribuidores",
            viewModel.uiState.value.locationErrorMessage
        )
    }

    private fun viewModel(locationProvider: FakeHomeLocationProvider) = HomeViewModel(
        api = api.apply {
            coEvery { getVerifiedDistributors(any(), any()) } returns Response.success(emptyList())
        },
        cropCycleRepository = NoOpCropCycleRepository,
        productCatalogRepository = FakeHomeProductCatalogRepository(),
        locationProvider = locationProvider
    )

    private data object NoOpCropCycleRepository : CropCycleRepository {
        override suspend fun getRelevantCycle() = null
    }
}
