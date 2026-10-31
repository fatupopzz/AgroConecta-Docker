package com.uvg.agroconecta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Estado de conexión para toda la app. Lo publica el NavHost para que
 * cualquier pantalla de error sepa si falta señal, sin pasarlo de mano en mano.
 */
val LocalIsOffline = compositionLocalOf { false }

/**
 * Llama a [onRetry] cuando la señal vuelve tras haberse perdido. En el campo
 * la señal se va y viene, y así nadie tiene que acordarse de tocar
 * "Reintentar". Solo actúa en ese cambio: un error del servidor con señal no
 * se reintenta solo, porque lo más probable es que vuelva a fallar.
 */
@Composable
fun RetryWhenBackOnline(onRetry: () -> Unit) {
    val isOffline = LocalIsOffline.current
    val currentOnRetry by rememberUpdatedState(onRetry)
    var wasOffline by remember { mutableStateOf(isOffline) }

    LaunchedEffect(isOffline) {
        val cameBack = wasOffline && !isOffline
        wasOffline = isOffline
        if (cameBack) {
            // Justo al reconectar la red suele tardar un poco en responder; sin
            // esta pausa el primer intento fallaba igual.
            delay(RECONNECT_GRACE_MS)
            currentOnRetry()
        }
    }
}

private const val RECONNECT_GRACE_MS = 1_500L
