package com.uvg.agroconecta.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `agrega el bearer de la sesion cuando hay token guardado`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request()).execute().close()

        assertEquals(
            "Bearer token-de-sesion",
            server.takeRequest().getHeader("Authorization")
        )
    }

    @Test
    fun `deja pasar el request sin cabecera cuando no hay sesion`() {
        val client = clientWithToken(null)
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `un token en blanco tampoco genera cabecera`() {
        val client = clientWithToken("   ")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request()).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `respeta la Authorization que ya trae el request`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        val explicito = request().newBuilder()
            .header("Authorization", "Bearer token-explicito")
            .build()
        client.newCall(explicito).execute().close()

        val received = server.takeRequest()
        assertEquals("Bearer token-explicito", received.getHeader("Authorization"))
        // Una segunda Authorization haria que el backend rechace el request.
        assertEquals(1, received.headers.values("Authorization").size)
    }

    @Test
    fun `no manda el token al endpoint de login`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(401))

        client.newCall(request("/api/auth/login")).execute().close()

        // Con la cabecera puesta, un 401 por contrasena mal escrita haria que
        // UnauthorizedInterceptor borre la sesion que quedo del uso anterior.
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `no manda el token al endpoint de registro`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request("/api/auth/register")).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `el resto de los endpoints de auth si llevan token`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request("/api/auth/me")).execute().close()

        assertEquals(
            "Bearer token-de-sesion",
            server.takeRequest().getHeader("Authorization")
        )
    }

    @Test
    fun `el token inyectado llega intacto al servidor aunque el log lo redacte`() {
        // Regresion: se reporto que el interceptor mandaba la cabecera
        // enmascarada. redactHeader solo toca el texto del log, nunca el
        // request; este test fija que lo que viaja es el token de verdad.
        server.enqueue(MockResponse().setResponseCode(200))
        val logs = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { "token-de-sesion" })
            .addInterceptor(
                ApiClientFactory.createLoggingInterceptor(
                    logger = HttpLoggingInterceptor.Logger { logs += it },
                    level = HttpLoggingInterceptor.Level.HEADERS
                )
            )
            .build()

        client.newCall(request()).execute().close()

        assertEquals(
            "Bearer token-de-sesion",
            server.takeRequest().getHeader("Authorization")
        )
        val output = logs.joinToString("\n")
        assertTrue(output.contains("Authorization: ██"))
        assertFalse(output.contains("token-de-sesion"))
    }

    @Test
    fun `si falla la lectura del token el request sale sin cabecera en vez de morir`() {
        // DataStore corrupto: preferimos un 401, que limpia la sesion y manda a
        // Login, antes que reventar cada llamada con un error de red falso.
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { throw IOException("DataStore corrupto") })
            .build()
        server.enqueue(MockResponse().setResponseCode(401))

        val response = client.newCall(request()).execute()

        assertEquals(401, response.code)
        response.close()
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `un path que solo contiene auth-login como subcadena si lleva token`() {
        // Con el match por `contains` este endpoint se quedaba sin token.
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(request("/api/admin/auth/login-history")).execute().close()

        assertEquals(
            "Bearer token-de-sesion",
            server.takeRequest().getHeader("Authorization")
        )
    }

    @Test
    fun `el match de publicos no depende del prefijo de la URL base`() {
        val client = clientWithToken("token-de-sesion")
        server.enqueue(MockResponse().setResponseCode(200))

        // Sin el /api/ de por medio tiene que seguir reconociendose publico.
        client.newCall(request("/auth/register")).execute().close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    private fun clientWithToken(token: String?): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
            .build()

    private fun request(path: String = "/products"): Request =
        Request.Builder().url(server.url(path)).build()
}
