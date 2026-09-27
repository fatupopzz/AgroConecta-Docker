package com.uvg.agroconecta.ui.dashboard

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.FarmerDashboardResponse
import com.uvg.agroconecta.data.models.FarmerLastOrder
import com.uvg.agroconecta.data.models.FarmerMonthlySpending
import com.uvg.agroconecta.data.models.FarmerTopProduct
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FarmerDashboardViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test
    fun `loads dashboard metrics`() = runTest(dispatcher.testDispatcher) {
        val api = mockk<ApiService>()
        val dashboard = sampleDashboard()
        coEvery { api.getFarmerDashboard() } returns Response.success(dashboard)
        val viewModel = FarmerDashboardViewModel(api)

        viewModel.loadDashboard()
        advanceUntilIdle()

        assertEquals(dashboard, viewModel.uiState.value.dashboard)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `shows a useful permission error`() = runTest(dispatcher.testDispatcher) {
        val api = mockk<ApiService>()
        coEvery { api.getFarmerDashboard() } returns Response.error(
            403,
            "".toResponseBody(null)
        )
        val viewModel = FarmerDashboardViewModel(api)

        viewModel.loadDashboard()
        advanceUntilIdle()

        assertEquals(
            "Este resumen solo está disponible para agricultores.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `shows a connection error and allows retry`() = runTest(dispatcher.testDispatcher) {
        val api = mockk<ApiService>()
        coEvery { api.getFarmerDashboard() } throws java.io.IOException("offline")
        val viewModel = FarmerDashboardViewModel(api)

        viewModel.loadDashboard()
        advanceUntilIdle()

        assertEquals("No se pudo conectar con AgroConecta.", viewModel.uiState.value.errorMessage)
    }

    private fun sampleDashboard() = FarmerDashboardResponse(
        totalGastadoHistorico = 725.5,
        totalGastadoMesActual = 125.0,
        cantidadPedidos = 4,
        gastosPorMes = listOf(FarmerMonthlySpending("2026-09", 125.0)),
        productosMasComprados = listOf(FarmerTopProduct(1, "Fertilizante", 5, 500.0)),
        ultimoPedido = FarmerLastOrder(8, "2026-09-25T10:00:00Z", "en_ruta", 125.0, "Agro Centro")
    )
}
