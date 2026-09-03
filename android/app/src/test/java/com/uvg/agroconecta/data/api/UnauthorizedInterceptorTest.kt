package com.uvg.agroconecta.data.api

import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UnauthorizedInterceptorTest {

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
    fun `401 with Authorization invalidates the session`() {
        server.enqueue(MockResponse().setResponseCode(401))
        var invalidations = 0
        val client = client { invalidations++ }

        client.newCall(authorizedRequest()).execute().close()

        assertEquals(1, invalidations)
    }

    @Test
    fun `401 without Authorization does not invalidate the session`() {
        server.enqueue(MockResponse().setResponseCode(401))
        var invalidations = 0
        val client = client { invalidations++ }
        val request = Request.Builder().url(server.url("/api/auth/login")).build()

        client.newCall(request).execute().close()

        assertEquals(0, invalidations)
    }

    @Test
    fun `403 with Authorization keeps the authenticated session`() {
        server.enqueue(MockResponse().setResponseCode(403))
        var invalidations = 0
        val client = client { invalidations++ }

        client.newCall(authorizedRequest()).execute().close()

        assertEquals(0, invalidations)
    }

    @Test
    fun `un 401 con la cabecera puesta por AuthInterceptor tambien cierra la sesion`() {
        // Cubre el orden de los interceptores: si el de auth no fuera primero,
        // este request llegaria sin Authorization y el 401 pasaria de largo.
        server.enqueue(MockResponse().setResponseCode(401))
        var invalidations = 0
        val client = ApiClientFactory.createOkHttpClient(
            authInterceptor = AuthInterceptor { "token-de-sesion" },
            onUnauthorized = { invalidations++ }
        )
        val request = Request.Builder().url(server.url("/api/protected")).build()

        client.newCall(request).execute().close()

        assertEquals(1, invalidations)
    }

    private fun client(onUnauthorized: () -> Unit) =
        ApiClientFactory.createOkHttpClient(onUnauthorized = onUnauthorized)

    private fun authorizedRequest(): Request = Request.Builder()
        .url(server.url("/api/protected"))
        .header("Authorization", "Bearer token-expirado")
        .build()
}
