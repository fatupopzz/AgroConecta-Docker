package com.uvg.agroconecta.ui.cart

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pantalla de teléfono común: con la de Robolectric por defecto (320x470 px)
// la barra del total tapa el botón "Quitar" y el toque cae sobre ella.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w411dp-h891dp")
class CartScreenUndoTest {

    @get:Rule
    val compose = createComposeRule()

    private val removed = mutableListOf<Int>()

    private fun setCart(items: List<CartItemUI>, onCheckout: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                CartScreen(
                    items = items,
                    total = items.sumOf { it.subtotal },
                    errorMessage = null,
                    onIncreaseQuantity = {},
                    onDecreaseQuantity = {},
                    onRemoveItem = { removed += it },
                    onCheckout = onCheckout,
                    onUrgentOrder = {},
                    onGoToCatalog = {},
                    onNavigateBack = {}
                )
            }
        }
    }

    @Test
    fun `deshacer devuelve el producto sin borrarlo del servidor`() {
        setCart(listOf(item(1, "Urea"), item(2, "Glifosato")))

        compose.onNodeWithContentDescription("Quitar Urea del carrito").performClick()
        compose.onNodeWithText("Quitaste Urea").assertIsDisplayed()
        compose.onNodeWithText("Deshacer").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Urea").assertIsDisplayed()
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `si el aviso se va solo el producto se borra`() {
        setCart(listOf(item(1, "Urea"), item(2, "Glifosato")))

        compose.onNodeWithContentDescription("Quitar Urea del carrito").performClick()
        compose.mainClock.advanceTimeBy(11_000)
        compose.waitForIdle()

        assertEquals(listOf(1), removed)
    }

    @Test
    fun `seguir con el pedido confirma lo que se quito`() {
        var checkouts = 0
        setCart(listOf(item(1, "Urea"), item(2, "Glifosato")), onCheckout = { checkouts++ })

        compose.onNodeWithContentDescription("Quitar Urea del carrito").performClick()
        compose.onNodeWithText("Continuar con el pedido").performClick()

        assertEquals(listOf(1), removed)
        assertEquals(1, checkouts)
    }

    private fun item(id: Int, name: String) = CartItemUI(
        id = id,
        idInventario = id * 10,
        idDistribuidor = 3,
        nombre = name,
        distribuidor = "Agroservicio El Sol",
        cantidad = 1,
        precio = 50.0,
        subtotal = 50.0
    )
}
