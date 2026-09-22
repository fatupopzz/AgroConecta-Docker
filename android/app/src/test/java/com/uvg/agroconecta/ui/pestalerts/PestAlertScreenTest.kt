package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uvg.agroconecta.data.models.PestAlert
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
        assertEquals("750 m", formatDistance(0.75))
        assertEquals("12.5 km", formatDistance(12.45))
        assertEquals("21/09/2026", formatAlertDate("2026-09-21T15:30:00Z"))
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
