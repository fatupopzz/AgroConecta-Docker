package com.uvg.agroconecta.ui.pestalerts

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.data.repository.PestAlertRepository
import com.uvg.agroconecta.ui.pestalerts.map.PestMapTrackingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PestAlertViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads nearby alerts and retains requested location`() {
        val alert = alert()
        val repository = FakePestAlertRepository(alerts = listOf(alert))
        val viewModel = createViewModel(repository)

        viewModel.loadNearbyAlerts(14.63, -90.50, 12.0)

        assertEquals(listOf(alert), viewModel.uiState.value.alerts)
        assertEquals(
            PestMapSearchArea(14.63, -90.50, 12.0),
            viewModel.uiState.value.searchArea
        )
        assertEquals(
            PestMapSearchArea(14.63, -90.50, 12.0),
            viewModel.uiState.value.lastSearchArea
        )
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertNull(viewModel.uiState.value.alertsErrorMessage)
        assertEquals(1, repository.nearbyRequests.size)
    }

    @Test
    fun `rejects invalid coordinates without querying repository`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(repository)

        viewModel.loadNearbyAlerts(95.0, -90.50)

        assertEquals("La latitud debe estar entre -90 y 90", viewModel.uiState.value.alertsErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertEquals(0, repository.nearbyRequests.size)
    }

    @Test
    fun `retains alerts and exposes message when refresh fails`() {
        val originalAlert = alert()
        val repository = FakePestAlertRepository(alerts = listOf(originalAlert))
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.63, -90.50)
        repository.nearbyError = IllegalStateException("Sin conexión")

        viewModel.retryNearbyAlerts()

        assertEquals(listOf(originalAlert), viewModel.uiState.value.alerts)
        assertEquals(
            PestMapSearchArea(14.63, -90.50, PestAlertRepository.DEFAULT_RADIUS_KM),
            viewModel.uiState.value.lastSearchArea
        )
        assertEquals("Sin conexión", viewModel.uiState.value.alertsErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingAlerts)
        assertEquals(2, repository.nearbyRequests.size)
    }

    @Test
    fun `selecting alert loads its suggested products`() {
        val alert = alert()
        val product = PestSuggestedProduct(id = 8, nombre = "Aceite de neem")
        val repository = FakePestAlertRepository(
            alerts = listOf(alert),
            products = mapOf(alert.id to listOf(product))
        )
        val viewModel = createViewModel(repository)

        viewModel.loadNearbyAlerts(14.63, -90.50)
        viewModel.openAlertDetails(alert.id)

        assertEquals(alert.id, viewModel.uiState.value.selectedAlertId)
        assertEquals(alert, viewModel.uiState.value.selectedAlertDetails)
        assertEquals(listOf(product), viewModel.uiState.value.suggestedProducts)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
        assertNull(viewModel.uiState.value.detailErrorMessage)
        assertEquals(listOf(alert.id), repository.productRequests)
    }

    @Test
    fun `map selection stores only alert id until details are requested`() {
        val alert = alert()
        val viewModel = createViewModel(FakePestAlertRepository(alerts = listOf(alert)))
        viewModel.loadNearbyAlerts(14.63, -90.50)

        viewModel.selectAlert(alert.id)

        assertEquals(alert.id, viewModel.uiState.value.selectedAlertId)
        assertNull(viewModel.uiState.value.selectedAlertDetails)

        viewModel.selectAlert(null)
        assertNull(viewModel.uiState.value.selectedAlertId)
    }

    @Test
    fun `notification id opens alert detail and its suggested products`() {
        val alert = alert()
        val product = PestSuggestedProduct(id = 8, nombre = "Aceite de neem")
        val repository = FakePestAlertRepository(
            alerts = listOf(alert),
            products = mapOf(alert.id to listOf(product))
        )
        val viewModel = createViewModel(repository)

        viewModel.openAlert(alert.id)

        assertEquals(listOf(alert.id), repository.detailRequests)
        assertEquals(alert.id, viewModel.uiState.value.selectedAlertId)
        assertEquals(alert, viewModel.uiState.value.selectedAlertDetails)
        assertEquals(listOf(product), viewModel.uiState.value.suggestedProducts)
        assertEquals(listOf(alert.id), repository.productRequests)
    }

    @Test
    fun `dismissing detail preserves map selection and clears detail data`() {
        val alert = alert()
        val repository = FakePestAlertRepository(
            alerts = listOf(alert),
            products = mapOf(alert.id to listOf(PestSuggestedProduct(8, "Aceite de neem")))
        )
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.63, -90.50)
        viewModel.openAlertDetails(alert.id)

        viewModel.dismissAlertDetail()

        assertEquals(alert.id, viewModel.uiState.value.selectedAlertId)
        assertNull(viewModel.uiState.value.selectedAlertDetails)
        assertEquals(emptyList<PestSuggestedProduct>(), viewModel.uiState.value.suggestedProducts)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
    }

    @Test
    fun `exposes detail error without clearing selected alert`() {
        val alert = alert()
        val repository = FakePestAlertRepository(
            alerts = listOf(alert),
            productsError = IllegalStateException("No disponible")
        )
        val viewModel = createViewModel(repository)

        viewModel.loadNearbyAlerts(14.63, -90.50)
        viewModel.openAlertDetails(alert.id)

        assertEquals(alert.id, viewModel.uiState.value.selectedAlertId)
        assertEquals(alert, viewModel.uiState.value.selectedAlertDetails)
        assertEquals("No disponible", viewModel.uiState.value.detailErrorMessage)
        assertFalse(viewModel.uiState.value.isLoadingSuggestions)
    }

    @Test
    fun `retries suggested products for the selected alert`() {
        val alert = alert()
        val product = PestSuggestedProduct(id = 8, nombre = "Aceite de neem")
        val repository = FakePestAlertRepository(
            alerts = listOf(alert),
            products = mapOf(alert.id to listOf(product)),
            productsError = IllegalStateException("No disponible")
        )
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.63, -90.50)
        viewModel.openAlertDetails(alert.id)
        repository.productsError = null

        viewModel.retrySuggestedProducts()

        assertEquals(listOf(product), viewModel.uiState.value.suggestedProducts)
        assertNull(viewModel.uiState.value.detailErrorMessage)
        assertEquals(listOf(alert.id, alert.id), repository.productRequests)
    }

    @Test
    fun `report validation requires pest crop and current location`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(repository)
        viewModel.openReportForm()

        viewModel.submitPestReport()

        assertEquals("Selecciona el tipo de plaga", viewModel.uiState.value.reportErrorMessage)
        assertTrue(viewModel.uiState.value.isReportFormVisible)
        assertEquals(0, repository.reportRequests.size)
    }

    @Test
    fun `search area does not replace GPS location for reports`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.63, -90.50)
        viewModel.openReportForm()
        viewModel.selectReportPestType(PestType.APHID)
        viewModel.selectReportCrop("Frijol")

        viewModel.submitPestReport()

        assertEquals(
            "No se pudo obtener la ubicación del reporte",
            viewModel.uiState.value.reportErrorMessage
        )
        assertEquals(0, repository.reportRequests.size)
    }

    @Test
    fun `submits report with selected values and prepends created alert`() {
        val createdAlert = alert().copy(id = 30, distanceKm = 0.0)
        val repository = FakePestAlertRepository(reportResult = createdAlert)
        val viewModel = createViewModel(
            repository,
            FakeCurrentLocationProvider(GeoCoordinates(14.63, -90.50))
        )
        viewModel.refreshLocation()
        viewModel.openReportForm()
        viewModel.selectReportPestType(PestType.THRIPS)
        viewModel.selectReportCrop("Tomate")
        viewModel.updateReportDescription("Daño visible en las hojas")

        viewModel.submitPestReport()

        val request = repository.reportRequests.single()
        assertEquals("trips", request.pestType)
        assertEquals("Tomate", request.cultivo)
        assertEquals(14.63, request.latitud, 0.0)
        assertEquals(-90.50, request.longitud, 0.0)
        assertEquals("Daño visible en las hojas", request.descripcion)
        assertEquals(createdAlert, viewModel.uiState.value.alerts.first())
        assertFalse(viewModel.uiState.value.isReportFormVisible)
        assertFalse(viewModel.uiState.value.isSubmittingReport)
        assertEquals("Alerta reportada correctamente", viewModel.uiState.value.reportSuccessMessage)
    }

    @Test
    fun `keeps report form open when submission fails`() {
        val repository = FakePestAlertRepository(
            reportError = IllegalStateException("Sin conexión")
        )
        val viewModel = createViewModel(
            repository,
            FakeCurrentLocationProvider(GeoCoordinates(14.63, -90.50))
        )
        viewModel.refreshLocation()
        viewModel.openReportForm()
        viewModel.selectReportPestType(PestType.APHID)
        viewModel.selectReportCrop("Frijol")

        viewModel.submitPestReport()

        assertTrue(viewModel.uiState.value.isReportFormVisible)
        assertFalse(viewModel.uiState.value.isSubmittingReport)
        assertEquals("Sin conexión", viewModel.uiState.value.reportErrorMessage)
    }

    @Test
    fun `limits optional report description to backend safe length`() {
        val viewModel = createViewModel(FakePestAlertRepository())
        viewModel.openReportForm()

        viewModel.updateReportDescription("a".repeat(600))

        assertEquals(500, viewModel.uiState.value.reportForm.description.length)
    }

    @Test
    fun `GPS location automatically loads nearby alerts`() {
        val alert = alert()
        val repository = FakePestAlertRepository(alerts = listOf(alert))
        val locationProvider = FakeCurrentLocationProvider(
            coordinates = GeoCoordinates(14.6349, -90.5069)
        )
        val viewModel = createViewModel(repository, locationProvider)

        viewModel.refreshLocation()

        assertEquals(
            PestMapSearchArea(
                14.6349,
                -90.5069,
                PestAlertRepository.DEFAULT_RADIUS_KM
            ),
            viewModel.uiState.value.searchArea
        )
        assertEquals(
            PestAlertLocation(14.6349, -90.5069),
            viewModel.uiState.value.userLocation
        )
        assertEquals(listOf(alert), viewModel.uiState.value.alerts)
        assertFalse(viewModel.uiState.value.isLocating)
        assertNull(viewModel.uiState.value.locationErrorMessage)
        assertEquals(1, locationProvider.requests)
        assertEquals(1, repository.nearbyRequests.size)
    }

    @Test
    fun `GPS location synchronizes push registration`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(
            repository = repository,
            locationProvider = FakeCurrentLocationProvider(
                coordinates = GeoCoordinates(14.6349, -90.5069)
            )
        )

        viewModel.refreshLocation()

        assertEquals(listOf(14.6349 to -90.5069), repository.pushRegistrations)
    }

    @Test
    fun `shows actionable error when GPS cannot determine location`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(
            repository,
            FakeCurrentLocationProvider(coordinates = null)
        )

        viewModel.refreshLocation()

        assertNull(viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
        assertEquals(
            "No se pudo determinar tu ubicación. Verifica que el GPS esté activo",
            viewModel.uiState.value.locationErrorMessage
        )
        assertEquals(0, repository.nearbyRequests.size)
    }

    @Test
    fun `permission denial clears location and explains required action`() {
        val repository = FakePestAlertRepository()
        val locationProvider = FakeCurrentLocationProvider(
            error = SecurityException("denegado")
        )
        val viewModel = createViewModel(repository, locationProvider)

        viewModel.refreshLocation()
        viewModel.onLocationPermissionDenied()

        assertNull(viewModel.uiState.value.userLocation)
        assertFalse(viewModel.uiState.value.isLocating)
        assertEquals(
            "Activa el permiso de ubicación para consultar y reportar alertas cercanas",
            viewModel.uiState.value.locationErrorMessage
        )
    }

    @Test
    fun `continuous location moves user marker without replacing search center`() {
        val locationProvider = FakeCurrentLocationProvider()
        val viewModel = createViewModel(FakePestAlertRepository(), locationProvider)
        viewModel.loadNearbyAlerts(14.63, -90.50, 25.0)

        viewModel.startLocationTracking()
        locationProvider.emitCoordinates(GeoCoordinates(14.6355, -90.5075))

        assertEquals(
            PestAlertLocation(14.6355, -90.5075, 25.0),
            viewModel.uiState.value.userLocation
        )
        assertEquals(
            PestMapSearchArea(14.63, -90.50, 25.0),
            viewModel.uiState.value.searchArea
        )
    }

    @Test
    fun `stopping tracking ignores later location updates`() {
        val locationProvider = FakeCurrentLocationProvider()
        val viewModel = createViewModel(FakePestAlertRepository(), locationProvider)
        viewModel.startLocationTracking()
        locationProvider.emitCoordinates(GeoCoordinates(14.6355, -90.5075))
        viewModel.stopLocationTracking()

        locationProvider.emitCoordinates(GeoCoordinates(14.6400, -90.5100))

        assertEquals(
            PestAlertLocation(14.6355, -90.5075),
            viewModel.uiState.value.userLocation
        )
    }

    @Test
    fun `changes between list and map presentation modes`() {
        val viewModel = createViewModel(FakePestAlertRepository())

        viewModel.setViewMode(PestAlertViewMode.MAP)

        assertEquals(PestAlertViewMode.MAP, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `tracking mode is explicit and center on user restores following`() {
        val viewModel = createViewModel(FakePestAlertRepository())

        viewModel.setTrackingMode(PestMapTrackingMode.FREE_EXPLORATION)
        assertEquals(PestMapTrackingMode.FREE_EXPLORATION, viewModel.uiState.value.trackingMode)

        viewModel.centerOnUser()
        assertEquals(PestMapTrackingMode.FOLLOWING_USER, viewModel.uiState.value.trackingMode)
    }

    @Test
    fun `camera movement only suggests search after a relevant distance`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)

        viewModel.onMapMoved(GeoCoordinates(14.6380, -90.5069))

        assertFalse(viewModel.uiState.value.shouldShowVisibleAreaSearch)
        assertEquals(1, repository.nearbyRequests.size)

        viewModel.onMapMoved(GeoCoordinates(14.6600, -90.5069))

        assertTrue(viewModel.uiState.value.shouldShowVisibleAreaSearch)
        assertEquals(1, repository.nearbyRequests.size)
    }

    @Test
    fun `searches visible camera area without replacing user location`() {
        val repository = FakePestAlertRepository(alerts = listOf(alert()))
        val viewModel = createViewModel(
            repository = repository,
            locationProvider = FakeCurrentLocationProvider(
                coordinates = GeoCoordinates(14.6349, -90.5069)
            )
        )
        viewModel.refreshLocation()
        viewModel.onMapMoved(GeoCoordinates(15.5000, -89.9000))

        viewModel.searchVisibleArea()

        assertEquals(
            PestAlertLocation(15.5000, -89.9000, PestAlertRepository.DEFAULT_RADIUS_KM),
            repository.nearbyRequests.last()
        )
        assertEquals(
            PestMapSearchArea(15.5000, -89.9000, PestAlertRepository.DEFAULT_RADIUS_KM),
            viewModel.uiState.value.lastSearchArea
        )
        assertEquals(
            PestAlertLocation(14.6349, -90.5069),
            viewModel.uiState.value.userLocation
        )
        assertFalse(viewModel.uiState.value.shouldShowVisibleAreaSearch)
    }

    @Test
    fun `failed area search keeps radius on last successful results`() {
        val repository = FakePestAlertRepository(alerts = listOf(alert()))
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)
        viewModel.onMapMoved(GeoCoordinates(15.5000, -89.9000))
        repository.nearbyError = IllegalStateException("Sin conexión")

        viewModel.searchVisibleArea()

        assertEquals(
            PestMapSearchArea(14.6349, -90.5069, 25.0),
            viewModel.uiState.value.lastSearchArea
        )
        assertTrue(viewModel.uiState.value.shouldShowVisibleAreaSearch)
        assertEquals("Sin conexión", viewModel.uiState.value.alertsErrorMessage)
    }

    @Test
    fun `changing radius searches again around current camera center`() {
        val repository = FakePestAlertRepository(alerts = listOf(alert()))
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)
        viewModel.onMapMoved(GeoCoordinates(15.5000, -89.9000))

        viewModel.changeSearchRadius(50.0)
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(SEARCH_RADIUS_DEBOUNCE_MS)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()

        assertEquals(
            PestAlertLocation(15.5000, -89.9000, 50.0),
            repository.nearbyRequests.last()
        )
        assertEquals(
            PestMapSearchArea(15.5000, -89.9000, 50.0),
            viewModel.uiState.value.cameraCenter
        )
        assertEquals(
            PestMapSearchArea(15.5000, -89.9000, 50.0),
            viewModel.uiState.value.lastSearchArea
        )
        assertEquals(50.0, viewModel.uiState.value.activeSearchRadiusKm, 0.0)
    }

    @Test
    fun `rapid radius changes debounce to the latest search`() {
        val repository = FakePestAlertRepository(alerts = listOf(alert()))
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)

        viewModel.changeSearchRadius(50.0)
        viewModel.changeSearchRadius(10.0)

        assertEquals(1, repository.nearbyRequests.size)
        assertEquals(10.0, viewModel.uiState.value.activeSearchRadiusKm, 0.0)

        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(SEARCH_RADIUS_DEBOUNCE_MS)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()

        assertEquals(2, repository.nearbyRequests.size)
        assertEquals(
            PestAlertLocation(14.6349, -90.5069, 10.0),
            repository.nearbyRequests.last()
        )
    }

    @Test
    fun `new area search cancels the previous request`() {
        val firstRequestGate = CompletableDeferred<Unit>()
        val repository = FakePestAlertRepository(nearbyGate = firstRequestGate)
        val viewModel = createViewModel(repository)

        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)
        repository.nearbyGate = null
        viewModel.loadNearbyAlerts(15.5000, -89.9000, 10.0)

        assertEquals(
            listOf(PestAlertLocation(14.6349, -90.5069, 25.0)),
            repository.cancelledNearbyRequests
        )
        assertEquals(PestMapSearchArea(15.5000, -89.9000, 10.0), viewModel.uiState.value.lastSearchArea)
        assertEquals(2, repository.nearbyRequests.size)
    }

    @Test
    fun `same or unsupported radius does not repeat search`() {
        val repository = FakePestAlertRepository()
        val viewModel = createViewModel(repository)
        viewModel.loadNearbyAlerts(14.6349, -90.5069, 25.0)

        viewModel.changeSearchRadius(25.0)
        viewModel.changeSearchRadius(30.0)

        assertEquals(1, repository.nearbyRequests.size)
        assertEquals(25.0, viewModel.uiState.value.activeSearchRadiusKm, 0.0)
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

    private fun createViewModel(
        repository: PestAlertRepository,
        locationProvider: CurrentLocationProvider = FakeCurrentLocationProvider()
    ) = PestAlertViewModel(repository, locationProvider)
}

private class FakeCurrentLocationProvider(
    var coordinates: GeoCoordinates? = null,
    var error: Throwable? = null
) : CurrentLocationProvider {
    var requests = 0
    private val observedCoordinates = MutableStateFlow<GeoCoordinates?>(null)

    override suspend fun getCurrentCoordinates(): GeoCoordinates? {
        requests += 1
        error?.let { throw it }
        return coordinates
    }

    override fun observeCoordinates(): Flow<GeoCoordinates> =
        observedCoordinates.filterNotNull()

    fun emitCoordinates(coordinates: GeoCoordinates) {
        observedCoordinates.value = coordinates
    }
}

private class FakePestAlertRepository(
    private val alerts: List<PestAlert> = emptyList(),
    private val products: Map<Int, List<PestSuggestedProduct>> = emptyMap(),
    var nearbyError: Throwable? = null,
    var productsError: Throwable? = null,
    private val reportResult: PestAlert? = null,
    private val reportError: Throwable? = null,
    var nearbyGate: CompletableDeferred<Unit>? = null
) : PestAlertRepository {
    val nearbyRequests = mutableListOf<PestAlertLocation>()
    val cancelledNearbyRequests = mutableListOf<PestAlertLocation>()
    val detailRequests = mutableListOf<Int>()
    val productRequests = mutableListOf<Int>()
    val reportRequests = mutableListOf<PestAlertReportRequest>()
    val pushRegistrations = mutableListOf<Pair<Double, Double>>()

    override suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<PestAlert> {
        val request = PestAlertLocation(latitude, longitude, radiusKm)
        nearbyRequests += request
        try {
            nearbyGate?.await()
        } catch (error: CancellationException) {
            cancelledNearbyRequests += request
            throw error
        }
        nearbyError?.let { throw it }
        return alerts
    }

    override suspend fun getAlert(alertId: Int): PestAlert {
        detailRequests += alertId
        return alerts.first { it.id == alertId }
    }

    override suspend fun reportPest(request: PestAlertReportRequest): PestAlert {
        reportRequests += request
        reportError?.let { throw it }
        return checkNotNull(reportResult)
    }

    override suspend fun getSuggestedProducts(alertId: Int): List<PestSuggestedProduct> {
        productRequests += alertId
        productsError?.let { throw it }
        return products[alertId].orEmpty()
    }

    override suspend fun syncPushRegistration(latitude: Double, longitude: Double) {
        pushRegistrations += latitude to longitude
    }
}
