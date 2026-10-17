package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CalendarCatalogFilterTest {
    @get:Rule val main = MainDispatcherRule()
    private val api = mockk<ApiService>()
    private val cycles = mockk<CropCycleRepository>()
    private val location = mockk<CurrentLocationProvider>()
    private val catalog = mockk<ProductCatalogRepository>()
    private fun vm(offline: Boolean = false): HomeViewModel {
        every { catalog.connectivity } returns flowOf(!offline)
        every { catalog.observeCachedProducts() } returns flowOf(listOf(
            Product(1, "Producto ajeno", null, null, null, null, null, 0.0, "Otra categoría", 10.0, 1)))
        coEvery { catalog.cacheState() } returns ProductCacheState.FRESH
        return HomeViewModel(api, cycles, catalog, location)
    }
    @Test fun `usa categoria sugerida real en repositorio existente`() {
        val vm = vm()
        val request = ProductRequest(categoryId = 4)
        coEvery { catalog.loadProducts(request) } returns ProductLoadResult.Success(emptyList(), 0)
        vm.applyCatalogFilter("", 4)
        assertEquals(4, vm.uiState.value.categoriaSeleccionadaId)
        coVerify(exactly = 1) { catalog.loadProducts(request) }
        coVerify(exactly = 2) { catalog.cacheState() }
        verify(exactly = 1) { catalog.connectivity }; verify(exactly = 1) { catalog.observeCachedProducts() }
        confirmVerified(api, cycles, location, catalog)
    }
    @Test fun `offline no asocia productos ajenos a categoria desconocida`() {
        val vm = vm(offline = true)
        coEvery { catalog.loadProducts(ProductRequest(categoryId = 4)) } returns ProductLoadResult.Offline
        vm.applyCatalogFilter("", 4)
        assertTrue(vm.uiState.value.productos.isEmpty())
        coVerify(exactly = 1) { catalog.loadProducts(ProductRequest(categoryId = 4)) }
        coVerify(exactly = 2) { catalog.cacheState() }
        verify(exactly = 1) { catalog.connectivity }; verify(exactly = 1) { catalog.observeCachedProducts() }
        confirmVerified(api, cycles, location, catalog)
    }
}
