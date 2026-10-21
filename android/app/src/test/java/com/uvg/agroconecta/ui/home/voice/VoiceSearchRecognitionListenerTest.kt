package com.uvg.agroconecta.ui.home.voice

import android.os.Bundle
import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceSearchRecognitionListenerTest {

    @Test
    fun `entrega en orden los resultados finales del sistema`() {
        var deliveredResults: List<String>? = null
        val listener = VoiceSearchRecognitionListener(
            onFinalResults = { deliveredResults = it }
        )
        val bundle = Bundle().apply {
            putStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION,
                arrayListOf("abono foliar", "fertilizante")
            )
        }

        listener.onResults(bundle)

        assertEquals(
            listOf("abono foliar", "fertilizante"),
            deliveredResults
        )
    }

    @Test
    fun `actualiza el estado de escucha y lo limpia al finalizar`() {
        var isListening = false
        val listener = VoiceSearchRecognitionListener(
            onListeningChanged = { isListening = it }
        )

        listener.onReadyForSpeech(null)
        assertTrue(isListening)

        listener.onEndOfSpeech()
        assertFalse(isListening)
    }

    @Test
    fun `limpia escucha y entrega el error del sistema`() {
        var isListening = true
        var deliveredError: Int? = null
        val listener = VoiceSearchRecognitionListener(
            onRecognitionError = { deliveredError = it },
            onListeningChanged = { isListening = it }
        )

        listener.onError(SpeechRecognizer.ERROR_NO_MATCH)

        assertFalse(isListening)
        assertEquals(SpeechRecognizer.ERROR_NO_MATCH, deliveredError)
    }
}
