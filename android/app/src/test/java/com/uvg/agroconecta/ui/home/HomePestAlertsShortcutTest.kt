package com.uvg.agroconecta.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomePestAlertsShortcutTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `pest alerts shortcut is visible and opens the feature`() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                PestAlertsShortcutCard(onClick = { clicks += 1 })
            }
        }

        compose.onNodeWithText("Alertas de plagas").assertIsDisplayed()
        compose.onNodeWithText("Consulta reportes cercanos o informa una plaga")
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Abrir alertas de plagas").assertIsDisplayed()
        compose.onNodeWithTag("pest-alerts-shortcut").performClick()

        assertEquals(1, clicks)
    }
}
