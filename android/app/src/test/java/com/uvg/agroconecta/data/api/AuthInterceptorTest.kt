package com.uvg.agroconecta.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun clientWithToken(token: String?): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { token })
            .build()

    private fun request(): Request =
        Request.Builder().url(server.url("/products")).build()
}
