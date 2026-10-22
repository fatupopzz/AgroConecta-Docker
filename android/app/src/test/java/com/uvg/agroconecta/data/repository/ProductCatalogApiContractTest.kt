package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductCatalogApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ProductCatalogApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
        api = RetrofitProductCatalogApi(service)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `envia nombre codificado y categoria con los nombres esperados por el backend`() = runTest {
        enqueueEmptyCatalog()

        api.getProducts(
            ProductRequest(
                page = 2,
                limit = 10,
                name = "abono foliar",
                categoryId = 4
            )
        )

        assertEquals(
            "/products?page=2&limit=10&nombre=abono%20foliar&id_categoria=4",
            server.takeRequest().path
        )
    }

    @Test
    fun `omite filtros opcionales al cargar el catalogo completo`() = runTest {
        enqueueEmptyCatalog()

        api.getProducts(ProductRequest(page = 1, limit = 10))

        assertEquals(
            "/products?page=1&limit=10",
            server.takeRequest().path
        )
    }

    private fun enqueueEmptyCatalog() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "page": 1,
                      "limit": 10,
                      "total": 0,
                      "products": []
                    }
                    """.trimIndent()
                )
        )
    }
}
