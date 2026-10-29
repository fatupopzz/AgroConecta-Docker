package com.uvg.agroconecta.ui.pestalerts

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollTo
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.ui.pestalerts.map.LocalPestMapHostFactory
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCameraState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCoordinate
import com.uvg.agroconecta.ui.pestalerts.map.PestMapHost
import com.uvg.agroconecta.ui.pestalerts.map.PestMapHostFactory
import com.uvg.agroconecta.ui.pestalerts.map.PestMapLoadState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapMarker
import com.uvg.agroconecta.ui.pestalerts.map.PestMapTrackingMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PestAlertScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows nearby alert data and opens selected alert`() {
        val alert = alert()
        var selectedAlert: PestAlert? = null
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(alerts = listOf(alert)),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = { selectedAlert = it },
                    onReportPest = {}
                )
            }
        }

        compose.onNodeWithText("Alertas activas cerca de ti").assertIsDisplayed()
        compose.onNodeWithText("Pulgón").assertIsDisplayed()
        compose.onNodeWithText("Cultivo: Frijol").assertIsDisplayed()
        compose.onNodeWithText("3.4 km").assertIsDisplayed()
        compose.onNodeWithText("21/09/2026").assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-29").performClick()

        assertEquals(alert, selectedAlert)
    }

    @Test
    fun `shows retry action when initial load fails`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(alertsErrorMessage = "Sin conexión"),
                    onNavigateBack = {},
                    onRetry = { retries += 1 },
                    onAlertClick = {},
                    onReportPest = {}
                )
            }
        }

        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun `empty state keeps report action available`() {
        var reportClicks = 0
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = {},
                    onReportPest = { reportClicks += 1 }
                )
            }
        }

        compose.onNodeWithText("No hay alertas activas cerca").assertIsDisplayed()
        compose.onNodeWithTag("report-pest").performClick()

        assertEquals(1, reportClicks)
    }

    @Test
    fun `formats backend values for the alert card`() {
        assertEquals("Mosca blanca", pestTypeDisplayName("mosca_blanca"))
        assertEquals("Plaga desconocida", pestTypeDisplayName("plaga_desconocida"))
        // Alertas publicadas desde un diagnóstico de KAN-110.
        assertEquals("Tizón tardío", pestTypeDisplayName("Tomato___Late_blight"))
        assertEquals("750 m", formatDistance(0.75))
        assertEquals("12.5 km", formatDistance(12.45))
        assertEquals("21/09/2026", formatAlertDate("2026-09-21T15:30:00Z"))
    }

    @Test
    fun `report form shows selections and submits complete report`() {
        var submissions = 0
        val location = PestAlertLocation(14.6349, -90.5069)
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(
                        location = location,
                        isReportFormVisible = true,
                        reportForm = PestReportFormState(
                            selectedPestType = PestType.APHID,
                            selectedCrop = "Frijol"
                        )
                    ),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = {},
                    onReportPest = {},
                    onSubmitReport = { submissions += 1 }
                )
            }
        }

        compose.onNodeWithText("Reportar una plaga").assertIsDisplayed()
        compose.onNodeWithText("Pulgón").assertIsDisplayed()
        compose.onNodeWithText("Frijol").assertIsDisplayed()
        compose.onNodeWithText("Lat. 14.63490, Long. -90.50690")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("submit-pest-report").performClick()

        assertEquals(1, submissions)
    }

    @Test
    fun `report form blocks submission while GPS location is unavailable`() {
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(
                        isReportFormVisible = true,
                        reportForm = PestReportFormState(
                            selectedPestType = PestType.THRIPS,
                            selectedCrop = "Tomate"
                        )
                    ),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = {},
                    onReportPest = {}
                )
            }
        }

        compose.onNodeWithText("Esperando ubicación GPS…")
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithTag("submit-pest-report").assertIsNotEnabled()
    }

    @Test
    fun `map mode forwards location and centers from my location button`() {
        val alert = alert()
        lateinit var mapHost: FakePestMapHost
        compose.setContent {
            CompositionLocalProvider(
                LocalPestMapHostFactory provides fakeMapHostFactory { mapHost = it }
            ) {
                MaterialTheme {
                    PestAlertScreen(
                        uiState = PestAlertUiState(
                            alerts = listOf(alert),
                            location = PestAlertLocation(14.6349, -90.5069),
                            viewMode = PestAlertViewMode.MAP
                        ),
                        onNavigateBack = {},
                        onRetry = {},
                        onAlertClick = {},
                        onReportPest = {}
                    )
                }
            }
        }

        compose.onNodeWithText("Mapa de alertas").assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-map").assertIsDisplayed()
        compose.onNodeWithTag("pest-map-my-location")
            .assertIsDisplayed()
            .performClick()

        assertEquals(
            listOf(PestMapCoordinate(14.6349, -90.5069) to true),
            mapHost.locationUpdates
        )
        assertEquals(1, mapHost.centerRequests)
    }

    @Test
    fun `my location button is disabled without coordinates`() {
        compose.setContent {
            CompositionLocalProvider(LocalPestMapHostFactory provides fakeMapHostFactory()) {
                MaterialTheme {
                    PestAlertScreen(
                        uiState = PestAlertUiState(
                            alerts = listOf(alert()),
                            location = null,
                            viewMode = PestAlertViewMode.MAP
                        ),
                        onNavigateBack = {},
                        onRetry = {},
                        onAlertClick = {},
                        onReportPest = {}
                    )
                }
            }
        }

        compose.onNodeWithTag("pest-map-my-location")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `map forwards pest markers and opens selected marker detail`() {
        val alert = alert()
        lateinit var mapHost: FakePestMapHost
        var selectedAlert: PestAlert? = null
        compose.setContent {
            CompositionLocalProvider(
                LocalPestMapHostFactory provides fakeMapHostFactory { mapHost = it }
            ) {
                MaterialTheme {
                    PestAlertScreen(
                        uiState = PestAlertUiState(
                            alerts = listOf(alert),
                            location = PestAlertLocation(14.6349, -90.5069),
                            viewMode = PestAlertViewMode.MAP
                        ),
                        onNavigateBack = {},
                        onRetry = {},
                        onAlertClick = { selectedAlert = it },
                        onReportPest = {}
                    )
                }
            }
        }

        compose.waitForIdle()
        assertEquals(
            listOf(
                PestMapMarker(
                    alertId = alert.id,
                    latitude = alert.latitud,
                    longitude = alert.longitud,
                    pestType = alert.pestType,
                    crop = alert.cultivo
                )
            ),
            mapHost.markerUpdates.single()
        )

        compose.runOnIdle { mapHost.simulateMarkerSelection(alert.id) }
        compose.onNodeWithTag("pest-map-selected-alert").assertIsDisplayed()
        compose.onNodeWithText("Pulgón").assertIsDisplayed()
        compose.onNodeWithText("Cultivo: Frijol").assertIsDisplayed()
        compose.onNodeWithTag("pest-map-view-details").performClick()

        assertEquals(alert, selectedAlert)
    }

    @Test
    fun `manual exploration prevents recentering on the next location`() {
        lateinit var mapHost: FakePestMapHost
        var location by mutableStateOf(PestAlertLocation(14.6349, -90.5069))
        compose.setContent {
            CompositionLocalProvider(
                LocalPestMapHostFactory provides fakeMapHostFactory { mapHost = it }
            ) {
                MaterialTheme {
                    PestAlertScreen(
                        uiState = PestAlertUiState(
                            alerts = listOf(alert()),
                            location = location,
                            viewMode = PestAlertViewMode.MAP
                        ),
                        onNavigateBack = {},
                        onRetry = {},
                        onAlertClick = {},
                        onReportPest = {}
                    )
                }
            }
        }

        compose.runOnIdle { mapHost.simulateManualExploration() }
        compose.runOnIdle {
            location = PestAlertLocation(14.6355, -90.5075)
        }
        compose.waitForIdle()

        assertEquals(
            PestMapCoordinate(14.6355, -90.5075) to false,
            mapHost.locationUpdates.last()
        )
    }

    @Test
    fun `manual exploration offers search for the visible camera area`() {
        lateinit var mapHost: FakePestMapHost
        var searches = 0
        var uiState by mutableStateOf(
            PestAlertUiState(
                alerts = listOf(alert()),
                location = PestAlertLocation(14.6349, -90.5069),
                lastSearchArea = PestMapSearchArea(14.6349, -90.5069, 25.0),
                viewMode = PestAlertViewMode.MAP
            )
        )
        compose.setContent {
            CompositionLocalProvider(
                LocalPestMapHostFactory provides fakeMapHostFactory { mapHost = it }
            ) {
                MaterialTheme {
                    PestAlertScreen(
                        uiState = uiState,
                        onNavigateBack = {},
                        onRetry = {},
                        onAlertClick = {},
                        onReportPest = {},
                        onMapCameraChanged = { latitude, longitude ->
                            uiState = uiState.copy(
                                cameraCenter = PestMapSearchArea(latitude, longitude, 25.0)
                            )
                        },
                        onSearchVisibleArea = { searches += 1 }
                    )
                }
            }
        }

        compose.runOnIdle {
            mapHost.simulateManualExploration(
                PestMapCameraState(
                    latitude = 15.5,
                    longitude = -89.9,
                    zoom = 12.0
                )
            )
        }
        compose.onNodeWithTag("pest-map-search-area")
            .assertIsDisplayed()
            .performClick()

        assertEquals(PestMapSearchArea(15.5, -89.9, 25.0), uiState.cameraCenter)
        assertEquals(1, searches)
    }

    @Test
    fun `alert detail shows preventive product information and can be dismissed`() {
        val alert = alert().copy(descripcion = "Presencia en hojas jóvenes")
        val product = PestSuggestedProduct(
            id = 8,
            nombre = "Aceite de neem",
            marca = "Agro Verde",
            descripcion = "Control preventivo de insectos",
            recommendedDose = "20 ml por bomba"
        )
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(
                        alerts = listOf(alert),
                        selectedAlert = alert,
                        suggestedProducts = listOf(product)
                    ),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = {},
                    onReportPest = {},
                    onDismissAlertDetail = { dismissals += 1 }
                )
            }
        }

        compose.onNodeWithTag("pest-alert-detail").assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-detail-content").performScrollToIndex(3)
        compose.onNodeWithText("Productos preventivos sugeridos")
            .assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-detail-content").performScrollToIndex(4)
        compose.onNodeWithText("Aceite de neem")
            .assertIsDisplayed()
        compose.onNodeWithText("20 ml por bomba")
            .assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-detail-content").performScrollToIndex(0)
        compose.onNodeWithTag("dismiss-pest-alert-detail").performClick()

        assertEquals(1, dismissals)
    }

    @Test
    fun `alert detail offers retry when product suggestions fail`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                PestAlertScreen(
                    uiState = PestAlertUiState(
                        selectedAlert = alert(),
                        detailErrorMessage = "No disponible"
                    ),
                    onNavigateBack = {},
                    onRetry = {},
                    onAlertClick = {},
                    onReportPest = {},
                    onRetryAlertDetail = { retries += 1 }
                )
            }
        }

        compose.onNodeWithTag("pest-alert-detail-content").performScrollToIndex(3)
        compose.onNodeWithText("No disponible")
            .assertIsDisplayed()
        compose.onNodeWithTag("retry-suggested-products")
            .performClick()

        assertEquals(1, retries)
    }

    private fun alert() = PestAlert(
        id = 29,
        pestType = "pulgon",
        cultivo = "Frijol",
        distanceKm = 3.4,
        latitud = 14.63,
        longitud = -90.50,
        reportedAt = "2026-09-21T15:30:00Z",
        severidad = "alta"
    )
}

private fun fakeMapHostFactory(
    onHostCreated: (FakePestMapHost) -> Unit = {}
) = PestMapHostFactory {
    context: Context,
    _: Bundle?,
    _: PestMapCameraState,
    onLoadStateChanged: (PestMapLoadState) -> Unit,
    onCameraChanged: (PestMapCameraState) -> Unit,
    onTrackingModeChanged: (PestMapTrackingMode) -> Unit ->
    FakePestMapHost(
        context = context,
        onLoadStateChanged = onLoadStateChanged,
        onCameraChanged = onCameraChanged,
        onTrackingModeChanged = onTrackingModeChanged
    ).also(onHostCreated)
}

private class FakePestMapHost(
    context: Context,
    private val onLoadStateChanged: (PestMapLoadState) -> Unit,
    private val onCameraChanged: (PestMapCameraState) -> Unit,
    private val onTrackingModeChanged: (PestMapTrackingMode) -> Unit
) : PestMapHost {
    override val view: View = View(context)
    val locationUpdates = mutableListOf<Pair<PestMapCoordinate?, Boolean>>()
    val markerUpdates = mutableListOf<List<PestMapMarker>>()
    var centerRequests = 0
    private var onMarkerSelected: (Int) -> Unit = {}

    override fun attach(lifecycle: Lifecycle) {
        onLoadStateChanged(PestMapLoadState.Ready)
    }

    override fun saveState(outState: Bundle) = Unit

    override fun retryStyle() = Unit

    override fun updateUserLocation(location: PestMapCoordinate?, centerCamera: Boolean) {
        locationUpdates += location to centerCamera
    }

    override fun updatePestMarkers(
        markers: List<PestMapMarker>,
        onMarkerSelected: (Int) -> Unit
    ) {
        markerUpdates += markers
        this.onMarkerSelected = onMarkerSelected
    }

    override fun centerOnUser() {
        centerRequests += 1
    }

    fun simulateManualExploration(
        cameraState: PestMapCameraState = PestMapCameraState.initial()
    ) {
        onTrackingModeChanged(PestMapTrackingMode.FREE_EXPLORATION)
        onCameraChanged(cameraState)
    }

    fun simulateMarkerSelection(alertId: Int) {
        onMarkerSelected(alertId)
    }

    override fun dispose() = Unit
}
