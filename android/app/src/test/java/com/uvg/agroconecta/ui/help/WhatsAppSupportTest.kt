package com.uvg.agroconecta.ui.help

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppSupportTest {

    @Test
    fun `support URL normalizes phone and encodes message`() {
        val url = buildWhatsAppSupportUrl(
            phoneNumber = "+502 5555-1234",
            message = "Hola, necesito ayuda."
        )

        assertEquals(
            "https://wa.me/50255551234?text=Hola%2C%20necesito%20ayuda.",
            url
        )
    }

    @Test
    fun `support URL opens contact picker when phone is not configured`() {
        assertEquals(
            "https://wa.me/?text=Hola%2C%20necesito%20ayuda%20con%20AgroConecta.",
            buildWhatsAppSupportUrl(phoneNumber = "")
        )
    }
}
