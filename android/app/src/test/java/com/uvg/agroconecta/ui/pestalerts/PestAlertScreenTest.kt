package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.material3.MaterialTheme
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `map mode shows user and alert markers and opens selected alert`() {
        val alert = alert()
        var selectedAlert: PestAlert? = null
        compose.setContent {
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

        compose.onNodeWithText("Mapa de alertas").assertIsDisplayed()
        compose.onNodeWithTag("pest-alert-map").assertIsDisplayed()
        compose.onNodeWithTag("current-location-marker").assertIsDisplayed()
        compose.onNodeWithTag("pest-map-marker-29").performClick()

        assertEquals(alert, selectedAlert)
    }

    @Test
    fun `map bounds contain all coordinates and normalize marker positions`() {
        val alert = alert()
        val location = PestAlertLocation(14.6349, -90.5069)

        val bounds = calculatePestMapBounds(listOf(alert), location)
        val marker = normalizePestMapPoint(alert.latitud, alert.longitud, bounds)

        assertTrue(bounds.minLatitude < minOf(alert.latitud, location.latitude))
        assertTrue(bounds.maxLatitude > maxOf(alert.latitud, location.latitude))
        assertTrue(bounds.minLongitude < minOf(alert.longitud, location.longitude))
        assertTrue(bounds.maxLongitude > maxOf(alert.longitud, location.longitude))
        assertTrue(marker.x in 0f..1f)
        assertTrue(marker.y in 0f..1f)
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
