package com.uvg.agroconecta.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Invalida la sesión cuando el backend rechaza un token enviado por la app.
 *
 * Un 401 sin Authorization puede ser un login fallido, por eso no debe cerrar
 * una sesión existente. Los 403 tampoco invalidan la sesión: representan un
 * usuario autenticado que no tiene permiso para la operación solicitada.
 */
internal class UnauthorizedInterceptor(
    private val onUnauthorized: () -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401 && request.header("Authorization") != null) {
            runCatching(onUnauthorized)
        }

        return response
    }
}
