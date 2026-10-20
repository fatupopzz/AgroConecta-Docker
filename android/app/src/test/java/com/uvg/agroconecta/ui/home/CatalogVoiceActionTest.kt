package com.uvg.agroconecta.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CatalogVoiceActionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `oculta el microfono cuando el reconocimiento no esta disponible`() {
        compose.setContent {
            MaterialTheme {
                CatalogSearchActions(
                    voiceSearchAvailable = false,
                    onVoiceSearchClick = {},
                    onSearchClick = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Buscar por voz").assertDoesNotExist()
        compose.onNodeWithContentDescription("Buscar").assertIsDisplayed()
    }

    @Test
    fun `muestra el microfono disponible y entrega su clic`() {
        var clicked = false
        compose.setContent {
            MaterialTheme {
                CatalogSearchActions(
                    voiceSearchAvailable = true,
                    onVoiceSearchClick = { clicked = true },
                    onSearchClick = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Buscar por voz")
            .assertIsDisplayed()
            .performClick()
        assertTrue(clicked)
    }
}
