package com.uvg.agroconecta.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientFactoryInterceptorTest {

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
    fun `BASIC logs request and response without exposing credentials or body`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"ok\":true}"))
        val logs = mutableListOf<String>()
        val interceptor = ApiClientFactory.createLoggingInterceptor(
            logger = HttpLoggingInterceptor.Logger { logs += it }
        )
        assertEquals(HttpLoggingInterceptor.Level.BASIC, interceptor.level)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val password = "clave-super-secreta"
        val token = "token-super-secreto"
        val body = """{"email":"juan@example.com","password":"$password"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(server.url("/api/auth/login"))
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val receivedRequest = server.takeRequest()
        assertEquals("Bearer $token", receivedRequest.getHeader("Authorization"))
        assertTrue(receivedRequest.body.readUtf8().contains(password))

        val output = logs.joinToString("\n")
        assertTrue(output.contains("--> POST"))
        assertTrue(output.contains("<-- 200"))
        assertFalse(output.contains(token))
        assertFalse(output.contains(password))
        assertFalse(output.contains("juan@example.com"))
    }

    @Test
    fun `HEADERS redacts Authorization without changing the real request`() {
        server.enqueue(MockResponse().setResponseCode(204))
        val logs = mutableListOf<String>()
        val interceptor = ApiClientFactory.createLoggingInterceptor(
            logger = HttpLoggingInterceptor.Logger { logs += it },
            level = HttpLoggingInterceptor.Level.HEADERS
        )
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val token = "token-que-no-debe-aparecer-en-logs"
        val request = Request.Builder()
            .url(server.url("/api/protected"))
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().close()

        assertEquals(
            "Bearer $token",
            server.takeRequest().getHeader("Authorization")
        )
        val output = logs.joinToString("\n")
        assertTrue(output.contains("Authorization: ██"))
        assertFalse(output.contains(token))
    }
}
