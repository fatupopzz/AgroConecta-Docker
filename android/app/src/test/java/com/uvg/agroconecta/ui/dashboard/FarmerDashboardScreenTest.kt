package com.uvg.agroconecta.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uvg.agroconecta.data.models.FarmerDashboardResponse
import com.uvg.agroconecta.data.models.FarmerLastOrder
import com.uvg.agroconecta.data.models.FarmerMonthlySpending
import com.uvg.agroconecta.data.models.FarmerTopProduct
import com.uvg.agroconecta.ui.profile.FarmerDashboardButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FarmerDashboardScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `dashboard displays metrics chart products and latest order`() {
        compose.setContent {
            MaterialTheme {
                FarmerDashboardBody(
                    uiState = FarmerDashboardUiState(
                        dashboard = FarmerDashboardResponse(
                            totalGastadoHistorico = 725.5,
                            totalGastadoMesActual = 125.0,
                            cantidadPedidos = 4,
                            gastosPorMes = listOf(FarmerMonthlySpending("2026-09", 125.0)),
                            productosMasComprados = listOf(
                                FarmerTopProduct(1, "Fertilizante", 5, 500.0)
                            ),
                            ultimoPedido = FarmerLastOrder(
                                8,
                                "2026-09-25T10:00:00Z",
                                "en_ruta",
                                125.0,
                                "Agro Centro"
                            )
                        )
                    ),
                    onRetry = {}
                )
            }
        }

        compose.onNodeWithText("Total histórico").assertIsDisplayed()
        compose.onNodeWithText("Pedidos realizados").assertIsDisplayed()
        compose.onNodeWithText("Gasto de los últimos 6 meses").assertIsDisplayed()
        compose.onNodeWithText("Fertilizante").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pedido #8").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `error state retries`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                FarmerDashboardBody(
                    uiState = FarmerDashboardUiState(errorMessage = "Sin conexión"),
                    onRetry = { retries += 1 }
                )
            }
        }

        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `farmer profile action opens dashboard`() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                FarmerDashboardButton { clicks += 1 }
            }
        }

        compose.onNodeWithText("Mi resumen").performClick()
        assertEquals(1, clicks)
    }
}
