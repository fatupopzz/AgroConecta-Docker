package com.uvg.agroconecta.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uvg.agroconecta.data.models.Distributor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeDistributorDistanceCardTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `distributor card shows distance in kilometers`() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                DistribuidorCard(
                    distribuidor = distributor(distanceKm = 3.42),
                    onClick = { clicks += 1 }
                )
            }
        }

        compose.onNodeWithText("3.4 km").assertIsDisplayed()
        compose.onNodeWithContentDescription("Distancia al distribuidor").assertIsDisplayed()
        compose.onNodeWithText("Agro Centro").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `distributor card omits distance when location is unavailable`() {
        compose.setContent {
            MaterialTheme {
                DistribuidorCard(
                    distribuidor = distributor(distanceKm = null),
                    onClick = {}
                )
            }
        }

        compose.onAllNodesWithContentDescription("Distancia al distribuidor").assertCountEquals(0)
    }

    @Test
    fun `distance formatter rejects invalid values`() {
        assertNull(formatDistributorDistanceKm(null))
        assertNull(formatDistributorDistanceKm(Double.NaN))
        assertNull(formatDistributorDistanceKm(-1.0))
        assertEquals("0.0 km", formatDistributorDistanceKm(0.0))
        assertEquals("12.7 km", formatDistributorDistanceKm(12.65))
    }

    private fun distributor(distanceKm: Double?) = Distributor(
        id = 4,
        nombreNegocio = "Agro Centro",
        departamento = "Guatemala",
        estadoVerificacion = "verificado",
        calificacion = 4.5,
        cantidadResenas = 2,
        nombre = null,
        email = null,
        telefono = null,
        distanciaKm = distanceKm
    )
}
