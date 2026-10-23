package com.uvg.agroconecta.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Vibraciones cortas para confirmar acciones sin tener que mirar la pantalla,
 * algo útil a pleno sol, cuando cuesta leer.
 *
 * Se usa la vista en lugar de LocalHapticFeedback porque Compose 1.6 solo
 * ofrece dos tipos de vibración. Además, View.performHapticFeedback respeta
 * el ajuste del sistema: si la persona apagó la vibración al tocar, no vibra.
 */
class AgroHaptics internal constructor(private val view: View) {

    /** Algo salió bien: se agregó al carrito, se envió el pedido. */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
    )

    /** La acción no se pudo hacer, por ejemplo un formulario con errores. */
    fun reject() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.LONG_PRESS
        }
    )

    /** Toque leve para cambios pequeños: sumar uno, marcar un favorito. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    private fun perform(constant: Int) {
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberAgroHaptics(): AgroHaptics {
    val view = LocalView.current
    return remember(view) { AgroHaptics(view) }
}
