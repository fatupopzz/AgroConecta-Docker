package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.ProductLoadResult
import com.uvg.agroconecta.data.repository.ProductRequest
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatalogSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val api = mockk<ApiService>()

    @Test
    fun `normaliza la consulta antes de solicitar productos`() {
        val repository = FakeHomeProductCatalogRepository(
            loadResult = ProductLoadResult.Success(emptyList(), total = 0)
        )
        val viewModel = viewModel(repository)

        viewModel.onSearchChange("  abono   orgánico  ")
        viewModel.onSearchSubmit()

        assertEquals("abono orgánico", viewModel.uiState.value.searchQuery)
        assertEquals(
            listOf(ProductRequest(name = "abono orgánico")),
            repository.requests
        )
    }

    @Test
    fun `envia consulta vacia como ausencia de filtro`() {
        val repository = FakeHomeProductCatalogRepository(
            loadResult = ProductLoadResult.Success(emptyList(), total = 0)
        )
        val viewModel = viewModel(repository)

        viewModel.onSearchChange("   ")
        viewModel.onSearchSubmit()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(ProductRequest(name = null)), repository.requests)
    }

    @Test
    fun `coloca el mejor resultado de voz y ejecuta la busqueda`() {
        val repository = FakeHomeProductCatalogRepository(
            loadResult = ProductLoadResult.Success(emptyList(), total = 0)
        )
        val viewModel = viewModel(repository)

        viewModel.onVoiceSearchResults(
            listOf("  abono   foliar  ", "fertilizante")
        )

        assertEquals("abono foliar", viewModel.uiState.value.searchQuery)
        assertEquals(
            listOf(ProductRequest(name = "abono foliar")),
            repository.requests
        )
    }

    @Test
    fun `ignora resultados de voz sin texto util`() {
        val repository = FakeHomeProductCatalogRepository()
        val viewModel = viewModel(repository)
        viewModel.onSearchChange("urea")

        viewModel.onVoiceSearchResults(emptyList())
        viewModel.onVoiceSearchResults(listOf("", "   "))

        assertEquals("urea", viewModel.uiState.value.searchQuery)
        assertEquals(emptyList<ProductRequest>(), repository.requests)
    }

    private fun viewModel(repository: FakeHomeProductCatalogRepository) = HomeViewModel(
        api = api,
        cropCycleRepository = NoOpCropCycleRepository,
        productCatalogRepository = repository,
        locationProvider = FakeHomeLocationProvider()
    )

    private data object NoOpCropCycleRepository : CropCycleRepository {
        override suspend fun getRelevantCycle() = null
    }
}
