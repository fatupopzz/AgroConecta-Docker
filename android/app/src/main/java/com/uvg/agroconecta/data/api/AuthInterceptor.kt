package com.uvg.agroconecta.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

private const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Adjunta el token de la sesion a cada request que sale de la app.
 *
 * Antes cada ViewModel armaba la cabecera y la pasaba como `@Header`, asi que
 * un endpoint nuevo se podia colar sin auth con solo olvidar el parametro
 * (KAN-79). Centralizarlo aca deja a los ViewModels sin nada que saber del
 * token.
 *
 * Recibe el token con un lambda suspend en vez del DataStore directo para que
 * el interceptor se pueda probar sin Context ni Hilt; quien lo arma es
 * `di.NetworkModule`.
 */
class AuthInterceptor(
    private val tokenProvider: suspend () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Si el llamador puso su propia Authorization, manda esa: duplicar la
        // cabecera hace que el backend rechace el request.
        if (request.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(request)
        }

        // DataStore solo entrega el token como Flow y esto corre en el hilo de
        // red de OkHttp, nunca en el principal, asi que bloquear aca no congela
        // la UI. Es el patron habitual para leer credenciales en un interceptor.
        val token = runBlocking { tokenProvider() }
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "Bearer $token")
                .build()
        )
    }
}
