package com.uvg.agroconecta.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.agroconecta.data.models.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WeatherCardTest {
    @get:Rule val compose = createComposeRule()
    private val report = WeatherReport(WeatherCurrent(22.0, 0.5), (21..23).map {
        WeatherDay("2026-10-$it", 15.0, 25.0, 2.0, 60.0)
    }, listOf(WeatherRecommendation("STRONG_WIND", "No fumigues hoy, hay viento fuerte")),
        "2026-10-21T15:00:00Z", "2026-10-21T15:01:00Z", false, "Open-Meteo")

    private fun show(state: WeatherUiState, onRefresh: () -> Unit = {}) {
        compose.setContent { MaterialTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) { WeatherCard(state, onRefresh) }
        } }
    }

    @Test fun `muestra temperatura lluvia tres dias y consejo literal`() {
        show(WeatherUiState(report = report, location = WeatherLocation.MAIN_ADDRESS))
        compose.onNodeWithText("Clima de tu finca").assertIsDisplayed()
        compose.onNodeWithText("Dirección de entrega principal").assertIsDisplayed()
        compose.onNodeWithText("22°C").assertIsDisplayed()
        compose.onNodeWithText("Está lloviendo: 0.5 mm").assertIsDisplayed()
        compose.onAllNodesWithText("Lluvia 60 %").assertCountEquals(3)
        compose.onNodeWithText("No fumigues hoy, hay viento fuerte").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dato del 21/10 a las 09:00 · Open-Meteo").performScrollTo().assertIsDisplayed()
    }
    @Test fun `dato antiguo tiene aviso textual y consejos historicos`() {
        show(WeatherUiState(report = report, stale = true))
        compose.onNodeWithText("Datos desactualizados · actualiza antes de planificar labores").assertIsDisplayed()
        compose.onNodeWithText("Consejos del último dato disponible").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Consejos para tu cultivo").assertDoesNotExist()
    }
    @Test fun `sin datos muestra error y permite reintentar`() {
        var retries = 0
        show(WeatherUiState(error = "Sin ubicación")) { retries++ }
        compose.onNodeWithText("Sin ubicación").assertIsDisplayed()
        compose.onNodeWithText("Actualizar").performClick()
        assertEquals(1, retries)
        compose.onNodeWithText("Pronóstico de 3 días").assertDoesNotExist()
    }
    @Test fun `carga deshabilita reintento y no inventa temperatura`() {
        show(WeatherUiState(loading = true))
        compose.onNodeWithText("Consultando el clima…").assertIsDisplayed()
        compose.onNodeWithText("Actualizar").assertIsNotEnabled()
        compose.onNodeWithText("22°C").assertDoesNotExist()
    }
}
