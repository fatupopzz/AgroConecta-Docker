package com.uvg.agroconecta.data.api

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DistributorApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sends farmer coordinates and reads distance in kilometers`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [{
                      "id_distribuidor": 2,
                      "nombre_negocio": "Agro Cercano",
                      "departamento": "Guatemala",
                      "estado_verificacion": "verificado",
                      "distancia_km": 3.42
                    }]
                    """.trimIndent()
                )
        )

        val response = api.getVerifiedDistributors(14.6349, -90.5069)

        assertEquals(3.42, response.body()?.single()?.distanciaKm ?: 0.0, 0.0)
        assertEquals(
            "/distribuidores?lat=14.6349&lng=-90.5069",
            server.takeRequest().path
        )
    }

    @Test
    fun `keeps coordinates optional for legacy requests`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [{
                      "id_distribuidor": 8,
                      "nombre_negocio": "Sin ubicación",
                      "departamento": null,
                      "estado_verificacion": "verificado",
                      "distancia_km": null
                    }]
                    """.trimIndent()
                )
        )

        val response = api.getVerifiedDistributors()

        assertNull(response.body()?.single()?.distanciaKm)
        assertEquals("/distribuidores", server.takeRequest().path)
    }
}
