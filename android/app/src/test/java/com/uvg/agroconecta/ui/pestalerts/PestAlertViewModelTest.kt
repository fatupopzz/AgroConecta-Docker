package com.uvg.agroconecta.ui.pestalerts

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.repository.PestAlertRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PestAlertViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads nearby alerts and retains requested location`() {
        val alert = alert()
        val repository = FakePestAlertRepository(alerts = listOf(alert))
        val viewModel = PestAlertViewModel(repository)

        viewModel.loadNearbyAlerts(14.63, -90.50, 12.0)

        assertEquals(listOf(alert), viewModel.uiState.value.alerts)
        assertEquals(PestAlertLocation(14.63, -90.50, 12.0), viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertNull(viewModel.uiState.value.alertsErrorMessage)
        assertEquals(1, repository.nearbyRequests.size)
    }

    @Test
    fun `rejects invalid coordinates without querying repository`() {
        val repository = FakePestAlertRepository()
        val viewModel = PestAlertViewModel(repository)

        viewModel.loadNearbyAlerts(95.0, -90.50)

        assertEquals("La latitud debe estar entre -90 y 90", viewModel.uiState.value.alertsErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertEquals(0, repository.nearbyRequests.size)
    }

    @Test
    fun `retains alerts and exposes message when refresh fails`() {
        val originalAlert = alert()
        val repository = FakePestAlertRepository(alerts = listOf(originalAlert))
        val viewModel = PestAlertViewModel(repository)
        viewModel.loadNearbyAlerts(14.63, -90.50)
        repository.nearbyError = IllegalStateException("Sin conexión")

        viewModel.retryNearbyAlerts()

        assertEquals(listOf(originalAlert), viewModel.uiState.value.alerts)
        assertEquals("Sin conexión", viewModel.uiState.value.alertsErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertEquals(2, repository.nearbyRequests.size)
    }

    @Test
    fun `selecting alert loads its suggested products`() {
        val alert = alert()
        val product = PestSuggestedProduct(id = 8, nombre = "Aceite de neem")
        val repository = FakePestAlertRepository(products = mapOf(alert.id to listOf(product)))
        val viewModel = PestAlertViewModel(repository)

        viewModel.selectAlert(alert)

        assertEquals(alert, viewModel.uiState.value.selectedAlert)
        assertEquals(listOf(product), viewModel.uiState.value.suggestedProducts)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
        assertNull(viewModel.uiState.value.detailErrorMessage)
        assertEquals(listOf(alert.id), repository.productRequests)
    }

    @Test
    fun `dismissing detail clears selected alert and suggestions`() {
        val alert = alert()
        val repository = FakePestAlertRepository(
            products = mapOf(alert.id to listOf(PestSuggestedProduct(8, "Aceite de neem")))
        )
        val viewModel = PestAlertViewModel(repository)
        viewModel.selectAlert(alert)

        viewModel.dismissAlertDetail()

        assertNull(viewModel.uiState.value.selectedAlert)
        assertEquals(emptyList<PestSuggestedProduct>(), viewModel.uiState.value.suggestedProducts)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
    }

    @Test
    fun `exposes detail error without clearing selected alert`() {
        val alert = alert()
        val repository = FakePestAlertRepository(
            productsError = IllegalStateException("No disponible")
        )
        val viewModel = PestAlertViewModel(repository)

        viewModel.selectAlert(alert)

        assertEquals(alert, viewModel.uiState.value.selectedAlert)
        assertEquals("No disponible", viewModel.uiState.value.detailErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
    }

    private fun alert() = PestAlert(
        id = 29,
        pestType = "pulgon",
        cultivo = "Frijol",
        distanceKm = 3.4,
        latitud = 14.63,
        longitud = -90.50,
        reportedAt = "2026-09-21T15:30:00Z"
    )
}

private class FakePestAlertRepository(
    private val alerts: List<PestAlert> = emptyList(),
    private val products: Map<Int, List<PestSuggestedProduct>> = emptyMap(),
    var nearbyError: Throwable? = null,
    private val productsError: Throwable? = null
) : PestAlertRepository {
    val nearbyRequests = mutableListOf<PestAlertLocation>()
    val productRequests = mutableListOf<Int>()

    override suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<PestAlert> {
        nearbyRequests += PestAlertLocation(latitude, longitude, radiusKm)
        nearbyError?.let { throw it }
        return alerts
    }

    override suspend fun reportPest(request: PestAlertReportRequest): PestAlert =
        error("No se usa en estas pruebas")

    override suspend fun getSuggestedProducts(alertId: Int): List<PestSuggestedProduct> {
        productRequests += alertId
        productsError?.let { throw it }
        return products[alertId].orEmpty()
    }
}
