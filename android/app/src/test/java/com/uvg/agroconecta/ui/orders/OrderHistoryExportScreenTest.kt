package com.uvg.agroconecta.ui.orders

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OrderHistoryExportScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `botón aparece solo para agricultor y se deshabilita durante descarga`() {
        val role = mutableStateOf("distribuidor")
        val state = mutableStateOf<OrderExportState>(OrderExportState.Idle)
        compose.setContent {
            MaterialTheme {
                OrderHistoryScreen(
                    orders = emptyList(), isLoading = false, errorMessage = null,
                    tipoUsuario = role.value, exportState = state.value,
                    onTrackOrder = {}, onOpenAdvice = {}, onBack = {},
                    onHomeClick = {}, onAgregarClick = {}, onPerfilClick = {}
                )
            }
        }
        compose.onNodeWithText("Exportar PDF").assertDoesNotExist()
        compose.runOnIdle {
            role.value = "agricultor"
            state.value = OrderExportState.Downloading
        }
        compose.onNodeWithText("Descargando PDF…").assertIsDisplayed().assertIsNotEnabled()
    }
}
