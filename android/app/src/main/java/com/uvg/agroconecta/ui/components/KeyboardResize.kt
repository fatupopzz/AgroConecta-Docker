package com.uvg.agroconecta.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

// Al pasar de una pantalla a otra, la nueva entra antes de que salga la anterior.
// Se cuenta cuántas lo piden para que la que sale no le quite el ajuste a la que entra.
private var screensAskingForResize = 0

/**
 * Hace que la ventana se encoja al abrir el teclado mientras esta pantalla esté
 * visible. Sin esto Android solo sube lo justo para mostrar el campo con el foco
 * y el botón de enviar queda debajo del teclado.
 *
 * Es por pantalla y no en el manifest: en el resto de la app la barra de
 * navegación inferior se subiría junto con el teclado.
 */
@Composable
fun ResizeWindowForKeyboard() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        screensAskingForResize += 1
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {
            screensAskingForResize -= 1
            if (screensAskingForResize == 0) {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
