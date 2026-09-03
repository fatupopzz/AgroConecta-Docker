package com.uvg.agroconecta.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

private const val AUTHORIZATION_HEADER = "Authorization"

/**
 * Endpoints que se resuelven sin sesion y que no deben llevar Authorization.
 *
 * Mandarles el token viejo tiene un efecto feo: la app siempre arranca en la
 * pantalla de Login con la sesion anterior todavia en DataStore, asi que un
 * login con la contrasena mal escrita respondia 401 con cabecera y
 * [UnauthorizedInterceptor] terminaba borrando esa sesion.
 *
 * Se guardan como los dos ultimos segmentos del path, que es con lo que se
 * comparan.
 */
private val PUBLIC_PATHS = setOf("auth/login", "auth/register")

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

        if (request.isPublic()) {
            return chain.proceed(request)
        }

        // Si el llamador puso su propia Authorization, manda esa: duplicar la
        // cabecera hace que el backend rechace el request.
        if (request.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(request)
        }

        val token = readToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION_HEADER, "Bearer $token")
                .build()
        )
    }

    /**
     * Lee el token y, si la lectura falla, sigue como si no hubiera sesion.
     *
     * Tirar la excepcion desde aca mataria el request y el usuario veria un
     * error de red que no es tal; peor, con el DataStore corrupto no habria
     * forma de salir. Yendo sin cabecera el backend responde 401,
     * [UnauthorizedInterceptor] limpia la sesion (que reescribe el archivo) y
     * la app manda a Login, asi que volver a entrar destraba el problema.
     *
     * El runBlocking es seguro aca: corre en el hilo de red de OkHttp, nunca en
     * el principal, y DataStore solo entrega el token como Flow.
     */
    private fun readToken(): String? =
        try {
            runBlocking { tokenProvider() }
        } catch (_: Exception) {
            null
        }

    // Se comparan los dos ultimos segmentos y no el path completo porque la URL
    // base trae su propio prefijo (/api/) segun el entorno. Con `contains` un
    // endpoint futuro tipo "admin/auth/login-history" se quedaria sin token sin
    // que nadie se entere.
    private fun okhttp3.Request.isPublic(): Boolean =
        url.pathSegments
            .filter { it.isNotEmpty() }
            .takeLast(2)
            .joinToString("/") in PUBLIC_PATHS
}
