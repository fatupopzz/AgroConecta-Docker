package com.uvg.agroconecta.ui.home.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

internal const val VOICE_SEARCH_LANGUAGE_TAG = "es-GT"

internal interface VoiceSearchRecognizerSession {
    fun startListening()
    fun destroy()
}

internal class AndroidVoiceSearchRecognizerSession(
    internal val recognizer: SpeechRecognizer,
    recognitionListener: RecognitionListener,
    private val recognitionIntent: () -> Intent = ::createVoiceSearchIntent
) : VoiceSearchRecognizerSession {

    init {
        recognizer.setRecognitionListener(recognitionListener)
    }

    override fun startListening() {
        recognizer.startListening(recognitionIntent())
    }

    override fun destroy() {
        recognizer.destroy()
    }
}

internal class VoiceSearchRecognitionListener(
    private val onFinalResults: (List<String>) -> Unit = {},
    private val onRecognitionError: (Int) -> Unit = {}
) : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onError(error: Int) = onRecognitionError(error)
    override fun onResults(results: Bundle?) {
        onFinalResults(
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        )
    }
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

internal fun createVoiceSearchIntent(): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, VOICE_SEARCH_LANGUAGE_TAG)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

internal class VoiceSearchRecognizerManager(
    private val createSession: () -> VoiceSearchRecognizerSession
) {
    private var session: VoiceSearchRecognizerSession? = null

    fun getOrCreate(): VoiceSearchRecognizerSession =
        session ?: createSession().also { session = it }

    fun startListening() {
        getOrCreate().startListening()
    }

    fun destroy() {
        session?.destroy()
        session = null
    }
}
