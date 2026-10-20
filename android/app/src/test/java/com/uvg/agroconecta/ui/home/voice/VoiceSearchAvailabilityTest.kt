package com.uvg.agroconecta.ui.home.voice

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSearchAvailabilityTest {

    private val context = mockk<Context>().also {
        every { it.applicationContext } returns it
    }

    @Test
    fun `reporta disponible cuando existe servicio de reconocimiento`() {
        val availability = AndroidVoiceSearchAvailability(context) { true }

        assertTrue(availability.isAvailable())
    }

    @Test
    fun `reporta no disponible cuando no existe servicio de reconocimiento`() {
        val availability = AndroidVoiceSearchAvailability(context) { false }

        assertFalse(availability.isAvailable())
    }

    @Test
    fun `reporta no disponible si el sistema falla al consultar el servicio`() {
        val availability = AndroidVoiceSearchAvailability(context) {
            error("Servicio de reconocimiento defectuoso")
        }

        assertFalse(availability.isAvailable())
    }
}
