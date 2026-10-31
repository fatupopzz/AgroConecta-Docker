package com.uvg.agroconecta.ui.components

internal const val NO_SIGNAL_MESSAGE =
    "No hay señal o el servidor no responde. Revisa tu conexión e inténtalo otra vez."
internal const val SLOW_CONNECTION_MESSAGE =
    "La conexión está muy lenta. Inténtalo otra vez en un momento."
internal const val GENERIC_ERROR_MESSAGE =
    "Algo salió mal. Inténtalo otra vez en un momento."

private val NO_SIGNAL_HINTS = listOf(
    "unable to resolve host",
    "failed to connect",
    "no address associated",
    "network is unreachable",
    "connection refused",
    "connection reset",
    "software caused connection abort"
)

private val SLOW_CONNECTION_HINTS = listOf("timeout", "timed out", "tiempo de espera")

private val TECHNICAL_HINTS = listOf("exception", "http ", "java.", "kotlin.", "retrofit", "okhttp", "{")

// "No se pudo cargar el carrito (500)": el número no le dice nada a quien
// compra y lo hace parecer más grave de lo que es.
private val TRAILING_STATUS_CODE = Regex("""\s*\((?:HTTP\s*)?\d{3}\)\s*$""", RegexOption.IGNORE_CASE)

/**
 * Convierte el mensaje de error que llega a la pantalla en algo que se
 * entienda sin saber de tecnología.
 *
 * Algunos ViewModels pasan tal cual el texto de la excepción (en inglés, con
 * nombres de clases). Esos se cambian por una explicación simple. Los que ya
 * están en español se dejan igual, solo sin el código HTTP.
 */
fun friendlyErrorMessage(raw: String?, fallback: String = GENERIC_ERROR_MESSAGE): String {
    val message = raw?.trim().orEmpty()
    if (message.isEmpty()) return fallback

    val lower = message.lowercase()
    return when {
        NO_SIGNAL_HINTS.any { it in lower } -> NO_SIGNAL_MESSAGE
        SLOW_CONNECTION_HINTS.any { it in lower } -> SLOW_CONNECTION_MESSAGE
        TECHNICAL_HINTS.any { it in lower } -> fallback
        else -> message.replace(TRAILING_STATUS_CODE, "").ifBlank { fallback }
    }
}
