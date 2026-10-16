package com.uvg.agroconecta.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfflineBannerTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows an accessible offline notice when there is no connection`() {
        compose.setContent {
            MaterialTheme { OfflineBanner(isOffline = true) }
        }

        compose.onNodeWithText(OFFLINE_INDICATOR_TEXT).assertIsDisplayed()
        compose.onNodeWithText(OFFLINE_INDICATOR_DETAIL).assertIsDisplayed()
    }

    @Test
    fun `stays hidden while online`() {
        compose.setContent {
            MaterialTheme { OfflineBanner(isOffline = false) }
        }

        compose.onAllNodesWithTag("offline-indicator").assertCountEquals(0)
    }
}
