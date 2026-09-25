package com.uvg.agroconecta.ui.help

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uvg.agroconecta.ui.profile.HelpButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HelpScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `faq answer expands and collapses when its question is selected`() {
        val faq = FrequentlyAskedQuestion(
            id = "test-question",
            question = "¿Cómo funciona?",
            answer = "Esta es la respuesta."
        )
        compose.setContent {
            MaterialTheme {
                FaqAccordionItem(faq = faq)
            }
        }

        compose.onNodeWithText(faq.answer).assertDoesNotExist()
        compose.onNodeWithText(faq.question).performClick()
        compose.onNodeWithText(faq.answer).assertIsDisplayed()
        compose.onNodeWithText(faq.question).performClick()
        compose.onNodeWithText(faq.answer).assertDoesNotExist()
    }

    @Test
    fun `help screen exposes back and WhatsApp actions`() {
        var backClicks = 0
        var supportClicks = 0
        compose.setContent {
            MaterialTheme {
                HelpScreen(
                    onNavigateBack = { backClicks += 1 },
                    onContactSupport = { supportClicks += 1 },
                    sections = emptyList()
                )
            }
        }

        compose.onNodeWithContentDescription("Regresar").performClick()
        compose.onNodeWithTag("whatsapp-support-button").performClick()

        assertEquals(1, backClicks)
        assertEquals(1, supportClicks)
    }

    @Test
    fun `profile help button opens help`() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                HelpButton(onClick = { clicks += 1 })
            }
        }

        compose.onNodeWithText("Ayuda y preguntas frecuentes")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clicks)
    }
}
