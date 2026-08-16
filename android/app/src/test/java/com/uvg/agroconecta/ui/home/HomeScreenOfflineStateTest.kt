package com.uvg.agroconecta.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScreenOfflineStateTest {

    @Test
    fun `HomeScreen exposes accessible offline label when offline`() {
        val state = HomeUiState(isOffline = true)

        assertEquals(OFFLINE_INDICATOR_TEXT, offlineIndicatorText(state.isOffline))
    }

    @Test
    fun `HomeScreen hides offline label when online`() {
        val state = HomeUiState(isOffline = false)

        assertNull(offlineIndicatorText(state.isOffline))
    }
}
