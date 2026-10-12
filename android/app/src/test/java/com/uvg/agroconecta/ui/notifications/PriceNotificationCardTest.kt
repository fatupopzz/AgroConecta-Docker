package com.uvg.agroconecta.ui.notifications

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.agroconecta.data.models.AppNotification
import com.uvg.agroconecta.data.models.NotificationContent
import com.uvg.agroconecta.ui.home.PriceDropNotificationAlert
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PriceNotificationCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun showsPricesAndRoutesClickToProductAndSeparateReadAction() {
        val alert = AppNotification(8,"baja_precio", NotificationContent(idProducto=4,producto="Maíz",precioAnterior=100.0,precioNuevo=75.0,porcentajeDescuento=25.0))
        var destination: Int? = null
        var reads = 0
        compose.setContent {
            MaterialTheme {
                PriceDropNotificationAlert(alert, false,
                    onClick = { destination = priceNotificationProductId(alert) }, onRead = { reads++ })
            }
        }
        compose.onNodeWithText("Maíz").assertIsDisplayed().performClick()
        assertEquals(4,destination)
        compose.onNodeWithText("Antes Q100.00 · Ahora Q75.00 · Descuento 25.00%").assertIsDisplayed()
        compose.onNodeWithText("Marcar como leída").performClick()
        assertEquals(1,reads)
    }
}
