package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.ProductCacheState
import com.uvg.agroconecta.data.repository.ProductLoadResult
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelOfflineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock estricto: estos tests solo ejercitan el catalogo offline, asi que si
    // alguno terminara pegandole al ApiService el test revienta en vez de
    // intentar salir a la red.
    private val api = mockk<ApiService>()

    @Test
    fun `loads expired cache immediately as last resort while offline`() {
        val cached = product(id = 1, name = "Producto guardado")
        val repository = FakeHomeProductCatalogRepository(
            cachedProducts = listOf(cached),
            online = false,
            currentCacheState = ProductCacheState.EXPIRED
        )

        val viewModel = HomeViewModel(api, NoOpCropCycleRepository, repository)

        assertEquals(listOf(cached), viewModel.uiState.value.productos)
        assertTrue(viewModel.uiState.value.isOffline)
        assertEquals(ProductCacheState.EXPIRED, viewModel.uiState.value.productCacheState)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun `updates products when API succeeds`() {
        val remote = product(id = 2, name = "Producto actualizado")
        val repository = FakeHomeProductCatalogRepository(
            cachedProducts = listOf(product(1)),
            loadResult = ProductLoadResult.Success(listOf(remote), total = 1)
        )
        val viewModel = HomeViewModel(api, NoOpCropCycleRepository, repository)

        viewModel.loadProductos(reset = true)

        assertEquals(listOf(remote), viewModel.uiState.value.productos)
        assertFalse(viewModel.uiState.value.isLoadingProductos)
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `keeps cached products when API fails`() {
        val cached = product(id = 3, name = "Respaldo local")
        val repository = FakeHomeProductCatalogRepository(
            cachedProducts = listOf(cached),
            loadResult = ProductLoadResult.Failure("API no disponible")
        )
        val viewModel = HomeViewModel(api, NoOpCropCycleRepository, repository)

        viewModel.loadProductos(reset = true)

        assertEquals(listOf(cached), viewModel.uiState.value.productos)
        assertEquals("API no disponible", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reflects offline and online state and refreshes after reconnecting`() {
        val remote = product(id = 4)
        val repository = FakeHomeProductCatalogRepository(
            online = true,
            loadResult = ProductLoadResult.Success(listOf(remote), 1)
        )
        val viewModel = HomeViewModel(api, NoOpCropCycleRepository, repository)

        repository.onlineState.value = false
        assertTrue(viewModel.uiState.value.isOffline)

        repository.onlineState.value = true

        assertFalse(viewModel.uiState.value.isOffline)
        assertEquals(1, repository.requests.size)
        assertEquals(listOf(remote), viewModel.uiState.value.productos)
    }

    private fun product(id: Int, name: String = "Producto $id") = Product(
        id = id,
        nombre = name,
        marca = null,
        descripcion = "Descripción",
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 0.0,
        categoria = "Semillas",
        precioDesde = 30.0,
        numDistribuidores = 1,
        activo = true
    )

    private data object NoOpCropCycleRepository : CropCycleRepository {
        override suspend fun getRelevantCycle() = null
    }
}
