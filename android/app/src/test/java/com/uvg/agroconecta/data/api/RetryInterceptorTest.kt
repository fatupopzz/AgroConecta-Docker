package com.uvg.agroconecta.data.api

import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.OrderProduct
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetAddress
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class RetryInterceptorTest {
    private val server = MockWebServer()
    private val waits = mutableListOf<Long>()
    private var duringWait: () -> Unit = {}
    private var invalidations = 0
    private val client = ApiClientFactory.createOkHttpClient(
        authInterceptor = AuthInterceptor { "sesion-de-prueba" },
        retryInterceptor = RetryInterceptor { delay ->
            waits += delay
            duringWait()
        },
        onUnauthorized = { invalidations++ }
    )

    @Before
    fun start() = server.start(InetAddress.getByName("127.0.0.1"), 0)

    @After
    fun stop() = server.shutdown()

    // Una única ruta de loopback: localhost también puede resolver a ::1.
    private fun url(path: String) = server.url(path).newBuilder().host("127.0.0.1").build()

    private fun request(method: String = "GET", path: String = "/api/products"): Request {
        val body = if (method in setOf("POST", "PATCH", "PUT")) "{}".toRequestBody() else null
        return Request.Builder().url(url(path)).method(method, body).build()
    }

    private fun disconnect() {
        // Se corta después de recibir la solicitud completa, antes de enviar cabeceras.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
    }

    @Test
    fun `GET recupera un timeout antes de las cabeceras`() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(MockResponse().setBody("catalogo"))
        val shortTimeout = client.newBuilder().readTimeout(250, TimeUnit.MILLISECONDS).build()

        shortTimeout.newCall(request()).execute().use {
            assertEquals(200, it.code)
            assertEquals("catalogo", it.body!!.string())
        }
        assertEquals(2, server.requestCount)
        assertEquals(listOf(200L), waits)
    }

    @Test
    fun `al tercer fallo se propaga la ultima IOException`() {
        repeat(2) { disconnect() }
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(MockResponse())
        val shortTimeout = client.newBuilder().readTimeout(250, TimeUnit.MILLISECONDS).build()

        assertThrows(SocketTimeoutException::class.java) { shortTimeout.newCall(request()).execute() }
        assertEquals(3, server.requestCount)
        assertEquals(listOf(200L, 400L), waits)
    }

    @Test
    fun `createOrder transmite un solo POST aunque el servidor ya haya recibido el pedido`() {
        disconnect()
        server.enqueue(MockResponse().setBody("{}"))
        val api = ApiClientFactory.createRetrofit(client, ApiClientFactory.createGson())
            .newBuilder().baseUrl(url("/api/")).build().create(ApiService::class.java)
        val order = CreateOrderRequest(1, 2, "Parcela", "domicilio", listOf(OrderProduct(3, 1)))

        assertThrows(IOException::class.java) { runBlocking { api.createOrder(order) } }
        assertEquals(1, server.requestCount)
        val received = server.takeRequest()
        assertEquals("POST", received.method)
        assertEquals("/api/orders", received.path)
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `POST generico PATCH PUT y DELETE tampoco se repiten`() {
        for ((index, method) in listOf("POST", "PATCH", "PUT", "DELETE").withIndex()) {
            disconnect()
            assertThrows(IOException::class.java) { client.newCall(request(method)).execute() }
            assertEquals(method, index + 1, server.requestCount)
        }
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `HEAD y OPTIONS recuperan una desconexion`() {
        for ((index, method) in listOf("HEAD", "OPTIONS").withIndex()) {
            disconnect()
            server.enqueue(MockResponse())
            client.newCall(request(method)).execute().use { assertEquals(200, it.code) }
            assertEquals(method, 2 * (index + 1), server.requestCount)
        }
        assertEquals(listOf(200L, 200L), waits)
    }

    @Test
    fun `los errores HTTP llegan intactos sin reenvio`() {
        for ((index, code) in listOf(400, 408, 421, 500, 503).withIndex()) {
            server.enqueue(MockResponse().setResponseCode(code).setHeader("Retry-After", "0").setBody("error"))
            client.newCall(request()).execute().use {
                assertEquals(code, it.code)
                assertEquals(code, it.networkResponse!!.code)
                assertEquals("0", it.header("Retry-After"))
                assertEquals("error", it.body!!.string())
            }
            assertEquals(index + 1, server.requestCount)
        }
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `el cliente tampoco reenvia el pedido por indicacion HTTP`() {
        for ((index, code) in listOf(307, 308, 503).withIndex()) {
            server.enqueue(MockResponse().setResponseCode(code)
                .setHeader("Location", "/api/orders").setHeader("Retry-After", "0"))
            client.newCall(request("POST", "/api/orders")).execute().use { assertEquals(code, it.code) }
            assertEquals(index + 1, server.requestCount)
        }
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `se conserva la redireccion de una consulta segura`() {
        server.enqueue(MockResponse().setResponseCode(307).setHeader("Location", "/api/catalog"))
        server.enqueue(MockResponse().setBody("catalogo"))

        client.newCall(request()).execute().use { assertEquals("catalogo", it.body!!.string()) }
        assertEquals(2, server.requestCount)
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `cancelar durante la espera evita el siguiente envio`() {
        disconnect()
        server.enqueue(MockResponse())
        val call = client.newCall(request())
        duringWait = call::cancel

        assertThrows(IOException::class.java) { call.execute() }
        assertEquals(1, server.requestCount)
        assertEquals(listOf(200L), waits)
    }

    @Test
    fun `una llamada ya cancelada no espera ni envia`() {
        val call = client.newCall(request())
        call.cancel()
        assertThrows(IOException::class.java) { call.execute() }
        assertEquals(0, server.requestCount)
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `un fallo permanente conserva la excepcion original sin reintento`() {
        val failures = listOf(
            SSLException("TLS", EOFException()),
            IOException(CertificateException("certificado", EOFException())),
            ProtocolException("cabeceras invalidas"),
            InterruptedIOException("interrumpido"),
            IOException("fallo local")
        )
        for ((index, failure) in failures.withIndex()) {
            server.enqueue(MockResponse())
            val failingClient = client.newBuilder().addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).close()
                throw failure
            }.build()
            assertSame(failure, assertThrows(IOException::class.java) { failingClient.newCall(request()).execute() })
            assertEquals(index + 1, server.requestCount)
        }
        assertEquals(emptyList<Long>(), waits)
    }

    @Test
    fun `el GET reintentado conserva Authorization e invalida una sola vez ante 401`() {
        disconnect()
        server.enqueue(MockResponse().setResponseCode(401))

        client.newCall(request()).execute().use { assertEquals(401, it.code) }
        assertEquals(2, server.requestCount)
        repeat(2) {
            assertEquals(listOf("Bearer sesion-de-prueba"), server.takeRequest().headers.values("Authorization"))
        }
        assertEquals(1, invalidations)
        assertEquals(listOf(200L), waits)
    }
}
