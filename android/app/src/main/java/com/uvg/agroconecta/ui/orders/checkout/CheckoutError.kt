package com.uvg.agroconecta.ui.orders.checkout

import java.io.IOException
import java.net.SocketTimeoutException

data class CheckoutError(val message: String, val retryable: Boolean = false) {
    companion object {
        private const val CHECK_HISTORY =
            " Revisa tu historial antes de reintentar: el pedido podría haberse creado."

        fun fromException(exception: Exception): CheckoutError = when (exception) {
            is SocketTimeoutException -> CheckoutError(
                "El servidor tardó demasiado en responder." + CHECK_HISTORY, true
            )
            is IOException -> CheckoutError(
                "Se perdió la conexión. Comprueba tu acceso a internet." + CHECK_HISTORY, true
            )
            else -> CheckoutError(
                "No pudimos confirmar la respuesta del pedido. Revisa tu historial antes de volver a confirmar."
            )
        }

        fun fromHttp(code: Int): CheckoutError = when (code) {
            408, 504 -> CheckoutError(
                "El servidor tardó demasiado en responder." + CHECK_HISTORY, true
            )
            in 500..599 -> CheckoutError(
                "El servidor tuvo un problema. Inténtalo más tarde." + CHECK_HISTORY, true
            )
            401 -> CheckoutError("Tu sesión venció. Inicia sesión de nuevo.")
            403 -> CheckoutError("No tienes permiso para crear este pedido. Revisa tu cuenta.")
            400, 404, 409, 422 -> CheckoutError(
                "Revisa la dirección, los productos y las cantidades del carrito antes de confirmar ($code)."
            )
            else -> CheckoutError(
                "No se pudo crear el pedido ($code). Revisa los datos antes de volver a confirmar."
            )
        }
    }
}
