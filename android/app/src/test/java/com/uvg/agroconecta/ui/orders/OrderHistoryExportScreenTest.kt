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
        compose.onNodeWithText("Pedidos recurrentes").assertIsDisplayed()
    }

    @Test fun `historial conserva PDF y abre recurrencia de pedido entregado`() {
        var openedId: Int? = null
        var openedList = false
        compose.setContent {
            MaterialTheme {
                OrderHistoryScreen(
                    orders = listOf(com.uvg.agroconecta.data.models.OrderSummary(
                        id = 44, estado = "entregado", fechaPedido = "2026-09-28T12:00:00Z",
                        totalPedido = 10.0, cantidadProductos = 1
                    )),
                    isLoading = false, errorMessage = null, tipoUsuario = "agricultor",
                    onTrackOrder = {}, onOpenAdvice = {}, onBack = {},
                    onHomeClick = {}, onAgregarClick = {}, onPerfilClick = {},
                    onRecurringOrders = { openedList = true },
                    onMakeRecurring = { openedId = it }
                )
            }
        }
        compose.onNodeWithText("Exportar PDF").assertIsDisplayed()
        compose.onNodeWithText("Pedidos recurrentes").performClick()
        compose.onNodeWithText("Hacer recurrente").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertTrue(openedList)
            org.junit.Assert.assertEquals(44, openedId)
        }
    }
}
