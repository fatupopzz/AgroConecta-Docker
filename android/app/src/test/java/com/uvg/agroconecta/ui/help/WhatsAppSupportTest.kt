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
    fun `configured phone selects a directed conversation`() {
        val target = resolveWhatsAppSupportTarget(phoneNumber = "+502 5555-1234")

        assertEquals(
            WhatsAppSupportTarget.DirectConversation(
                "https://wa.me/50255551234?text=" +
                    "Hola%2C%20necesito%20ayuda%20con%20AgroConecta."
            ),
            target
        )
    }

    @Test
    fun `missing phone selects the WhatsApp contact picker`() {
        val target = resolveWhatsAppSupportTarget(phoneNumber = "")

        assertEquals(
            WhatsAppSupportTarget.ContactPicker(DEFAULT_SUPPORT_MESSAGE),
            target
        )
    }
}
