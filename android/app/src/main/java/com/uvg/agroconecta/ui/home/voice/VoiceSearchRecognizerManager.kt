package com.uvg.agroconecta.ui.home.voice

import android.speech.SpeechRecognizer

internal fun interface VoiceSearchRecognizerSession {
    fun destroy()
}

internal class AndroidVoiceSearchRecognizerSession(
    internal val recognizer: SpeechRecognizer
) : VoiceSearchRecognizerSession {

    override fun destroy() {
        recognizer.destroy()
    }
}

internal class VoiceSearchRecognizerManager(
    private val createSession: () -> VoiceSearchRecognizerSession
) {
    private var session: VoiceSearchRecognizerSession? = null

    fun getOrCreate(): VoiceSearchRecognizerSession =
        session ?: createSession().also { session = it }

    fun destroy() {
        session?.destroy()
        session = null
    }
}
