package com.uvg.agroconecta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyErrorsTest {

    @Test
    fun `sin mensaje usa el texto general`() {
        assertEquals(GENERIC_ERROR_MESSAGE, friendlyErrorMessage(null))
        assertEquals(GENERIC_ERROR_MESSAGE, friendlyErrorMessage("   "))
    }

    @Test
    fun `fallas de red en ingles se explican como falta de senal`() {
        assertEquals(
            NO_SIGNAL_MESSAGE,
            friendlyErrorMessage("Unable to resolve host \"20.63.8.63\": No address associated with hostname")
        )
        assertEquals(NO_SIGNAL_MESSAGE, friendlyErrorMessage("Failed to connect to /10.0.2.2:8080"))
    }

    @Test
    fun `tiempos de espera se explican como conexion lenta`() {
        assertEquals(SLOW_CONNECTION_MESSAGE, friendlyErrorMessage("timeout"))
        assertEquals(SLOW_CONNECTION_MESSAGE, friendlyErrorMessage("Read timed out"))
    }

    @Test
    fun `textos tecnicos se cambian por el respaldo indicado`() {
        assertEquals(
            "No pudimos cargar tus favoritos",
            friendlyErrorMessage(
                "java.lang.IllegalStateException: Expected BEGIN_OBJECT",
                fallback = "No pudimos cargar tus favoritos"
            )
        )
        assertEquals(GENERIC_ERROR_MESSAGE, friendlyErrorMessage("HTTP 500 Internal Server Error"))
    }

    @Test
    fun `mensajes en espanol se conservan sin el codigo http`() {
        assertEquals(
            "No se pudo cargar el carrito",
            friendlyErrorMessage("No se pudo cargar el carrito (500)")
        )
        assertEquals("Sin conexión", friendlyErrorMessage("Sin conexión"))
    }
}
