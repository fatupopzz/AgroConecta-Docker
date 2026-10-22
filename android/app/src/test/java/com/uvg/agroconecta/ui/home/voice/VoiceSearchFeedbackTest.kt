package com.uvg.agroconecta.ui.home.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSearchFeedbackTest {

    @Test
    fun `explica cuando no se entiende la frase`() {
        assertEquals(
            VOICE_NO_MATCH_MESSAGE,
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_NO_MATCH)
        )
        assertEquals(
            VOICE_NO_MATCH_MESSAGE,
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        )
    }

    @Test
    fun `explica cuando falta permiso de microfono`() {
        assertEquals(
            VOICE_PERMISSION_DENIED_MESSAGE,
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        )
    }

    @Test
    fun `distingue problemas de red idioma y reconocedor ocupado`() {
        assertEquals(
            "No se pudo conectar al reconocimiento de voz. Revisa tu conexión.",
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_NETWORK)
        )
        assertEquals(
            "El reconocimiento en español no está disponible en este dispositivo.",
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        )
        assertEquals(
            "El reconocimiento de voz está ocupado. Intenta nuevamente.",
            voiceSearchErrorMessage(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
        )
    }
}
