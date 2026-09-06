package com.uvg.agroconecta.ui.orders

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.agroconecta.ui.cart.CartItemUI
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OrderConfirmationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `error permanece visible reintento envia una vez y carga bloquea las acciones`() {
        val loading = mutableStateOf(false)
        val error = mutableStateOf<String?>("Se perdió la conexión. Comprueba tu acceso a internet.")
        var retries = 0
        var confirmations = 0
        compose.setContent {
            MaterialTheme {
                OrderConfirmationScreen(
                    items = listOf(item()), total = 14.14,
                    deliveryAddress = "Parcela norte", pickupAddress = null,
                    isLoadingPickupAddress = false, tipoEntrega = "domicilio",
                    isCreatingOrder = loading.value, errorMessage = error.value,
                    canSubmit = error.value == null, canRetry = error.value != null,
                    onRetryOrder = { retries++; loading.value = true; error.value = null },
                    onDeliveryAddressChange = {}, onTipoEntregaChange = {},
                    onConfirmOrder = { confirmations++ }, onBack = {}
                )
            }
        }
        compose.onNodeWithText("Se perdió la conexión. Comprueba tu acceso a internet.").assertIsDisplayed()
        compose.onAllNodesWithText("Confirmar pedido").filter(hasClickAction()).onFirst().assertIsNotEnabled()
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithText("Se perdió la conexión. Comprueba tu acceso a internet.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsEnabled().performClick()
        compose.onNodeWithText("Enviando pedido…").assertIsDisplayed().assertIsNotEnabled().performClick()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").assertIsNotEnabled()
        compose.onNodeWithText("Parcela norte").assertIsNotEnabled()
        assertEquals(1, retries)
        assertEquals(0, confirmations)
    }

    @Test
    fun `error no reintentable permanece visible sin accion de reintento`() {
        compose.setContent {
            MaterialTheme {
                OrderConfirmationBottomBar(
                    total = 14.14, canConfirm = false, onConfirmOrder = {},
                    isCreatingOrder = false, errorMessage = "Revisa los productos y cantidades (409).",
                    canRetry = false, onRetryOrder = { error("No debe reintentar") }
                )
            }
        }
        compose.onNodeWithText("Revisa los productos y cantidades (409).").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertDoesNotExist()
        compose.onNodeWithText("Confirmar pedido").assertIsNotEnabled()
    }

    @Test
    fun `doble toque al confirmar deshabilita boton y muestra progreso`() {
        val loading = mutableStateOf(false)
        var calls = 0
        compose.setContent {
            MaterialTheme {
                OrderConfirmationBottomBar(
                    total = 14.14, canConfirm = true,
                    onConfirmOrder = { calls++; loading.value = true },
                    isCreatingOrder = loading.value, errorMessage = null,
                    canRetry = false, onRetryOrder = {}
                )
            }
        }
        compose.onNodeWithText("Confirmar pedido").performClick()
        compose.onNodeWithText("Enviando pedido…").assertIsNotEnabled().performClick()
        assertEquals(1, calls)
    }

    private fun item() = CartItemUI(1, 8, 3, "Fertilizante", "Agroinsumos", 2, 7.07, 14.14)
}
