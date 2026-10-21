package com.uvg.agroconecta.ui.home.voice

import android.speech.SpeechRecognizer

internal const val VOICE_PERMISSION_DENIED_MESSAGE =
    "Se necesita permiso de micrófono para buscar por voz."
internal const val VOICE_NO_MATCH_MESSAGE =
    "No se entendió la frase. Puedes intentarlo de nuevo o escribir tu búsqueda."
internal const val VOICE_SEARCH_START_ERROR_MESSAGE =
    "No se pudo iniciar la búsqueda por voz. Puedes seguir escribiendo."

internal fun voiceSearchErrorMessage(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VOICE_NO_MATCH_MESSAGE

    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER,
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
        "No se pudo conectar al reconocimiento de voz. Revisa tu conexión."

    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VOICE_PERMISSION_DENIED_MESSAGE

    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        "El reconocimiento en español no está disponible en este dispositivo."

    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
        "El reconocimiento de voz está ocupado. Intenta nuevamente."

    else -> VOICE_SEARCH_START_ERROR_MESSAGE
}
