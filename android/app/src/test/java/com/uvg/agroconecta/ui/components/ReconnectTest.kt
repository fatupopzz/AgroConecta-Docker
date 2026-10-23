package com.uvg.agroconecta.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReconnectTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `sin senal el error lo dice y se reintenta solo al volver`() {
        val offline = mutableStateOf(true)
        var retries = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalIsOffline provides offline.value) {
                    ErrorState(message = "No se pudo cargar", onRetry = { retries++ })
                }
            }
        }

        compose.onNodeWithText(OFFLINE_RETRY_MESSAGE).assertIsDisplayed()
        assertEquals(0, retries)

        offline.value = false
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()

        assertEquals(1, retries)
        compose.onNodeWithText("No se pudo cargar").assertIsDisplayed()
    }

    @Test
    fun `un error con senal no se reintenta solo`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalIsOffline provides false) {
                    ErrorState(message = "No se pudo cargar (500)", onRetry = { retries++ })
                }
            }
        }

        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()

        assertEquals(0, retries)
        compose.onNodeWithText("No se pudo cargar").assertIsDisplayed()
    }
}
