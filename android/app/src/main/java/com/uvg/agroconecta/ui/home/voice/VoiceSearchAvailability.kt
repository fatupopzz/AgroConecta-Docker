package com.uvg.agroconecta.ui.home.voice

import android.content.Context
import android.speech.SpeechRecognizer

internal fun interface VoiceSearchAvailability {
    fun isAvailable(): Boolean
}

internal class AndroidVoiceSearchAvailability(
    context: Context,
    private val availabilityCheck: (Context) -> Boolean = {
        SpeechRecognizer.isRecognitionAvailable(it)
    }
) : VoiceSearchAvailability {

    private val applicationContext = context.applicationContext

    override fun isAvailable(): Boolean = runCatching {
        availabilityCheck(applicationContext)
    }.getOrDefault(false)
}
