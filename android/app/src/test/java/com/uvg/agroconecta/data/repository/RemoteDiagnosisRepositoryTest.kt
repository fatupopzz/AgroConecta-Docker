package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.local.PendingDiagnosisDao
import com.uvg.agroconecta.data.local.PendingDiagnosisEntity
import com.uvg.agroconecta.data.models.AddItemRequest
import com.uvg.agroconecta.data.models.DiagnosisRecord
import com.uvg.agroconecta.data.models.DiagnosisRequest
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.ProductDetail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteDiagnosisRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var dao: FakePendingDiagnosisDao
    private lateinit var connectivity: FakeConnectivity
    private var scheduled = 0
    private var nextLocalId = 0

    private val submission = DiagnosisSubmission(
        userId = 7,
        crop = "Tomate",
        diseaseLabel = "Tomato___Late_blight",
        confidence = 0.91f,
        latitude = 14.63,
        longitude = -90.51
    )

    @Before
    fun setUp() {
        api = mockk()
        dao = FakePendingDiagnosisDao()
        connectivity = FakeConnectivity(online = true)
        scheduled = 0
        nextLocalId = 0
    }

    private fun repository() = RemoteDiagnosisRepository(
        api = api,
        dao = dao,
        connectivity = connectivity,
        syncScheduler = { scheduled += 1 },
        now = { 1_000L },
        newLocalId = { "local-${++nextLocalId}" }
    )

    @Test
    fun `sin señal guarda el diagnóstico y programa el envío sin llamar al backend`() = runTest {
        connectivity.online = false

        val upload = repository().submit(submission)

        assertEquals(DiagnosisUpload.Pending("local-1"), upload)
        assertEquals(listOf("local-1"), dao.unsent(7).map { it.localId })
        assertEquals(1, scheduled)
        confirmVerified(api)
    }

    @Test
    fun `con señal lo envía con la foto local y la ubicación`() = runTest {
        val request = DiagnosisRequest(
            cultivo = "Tomate",
            enfermedad = "Tomato___Late_blight",
            confianza = 0.91f.toDouble(),
            foto = "local://diagnostico/local-1",
            latitud = 14.63,
            longitud = -90.51
        )
        coEvery { api.createDiagnosis(request) } returns Response.success(record(15))

        val upload = repository().submit(submission)

        assertEquals(DiagnosisUpload.Sent(15), upload)
        assertEquals(15, dao.get("local-1")?.remoteId)
        assertEquals(0, scheduled)
        coVerify(exactly = 1) { api.createDiagnosis(request) }
        confirmVerified(api)
    }

    @Test
    fun `si la red falla al enviar queda pendiente`() = runTest {
        coEvery { api.createDiagnosis(any()) } throws IOException("sin red")

        val upload = repository().submit(submission)

        assertEquals(DiagnosisUpload.Pending("local-1"), upload)
        assertNull(dao.get("local-1")?.remoteId)
        assertEquals(1, scheduled)
    }

    @Test
    fun `reintentar dos veces el mismo pendiente lo envía una sola vez`() = runTest {
        connectivity.online = false
        val repository = repository()
        repository.submit(submission)
        connectivity.online = true
        coEvery { api.createDiagnosis(any()) } returns Response.success(record(15))

        assertEquals(15, repository.retryPending("local-1"))
        assertEquals(15, repository.retryPending("local-1"))

        coVerify(exactly = 1) { api.createDiagnosis(any()) }
    }

    @Test
    fun `un 400 descarta el pendiente porque reintentar no lo arregla`() = runTest {
        coEvery { api.createDiagnosis(any()) } returns Response.error(
            400,
            "{}".toResponseBody("application/json".toMediaTypeOrNull())
        )
        val repository = repository()

        val error = runCatching { repository.submit(submission) }.exceptionOrNull()

        assertTrue(error is DiagnosisRejectedException)
        assertNull(dao.get("local-1"))
    }

    @Test
    fun `sincroniza solo los pendientes del usuario y cuenta los que siguen sin enviar`() = runTest {
        connectivity.online = false
        val repository = repository()
        repository.submit(submission)
        repository.submit(submission.copy(userId = 99))
        repository.submit(submission.copy(diseaseLabel = "Tomato___Early_blight"))
        coEvery { api.createDiagnosis(match { it.enfermedad == "Tomato___Late_blight" }) } returns
            Response.success(record(15))
        coEvery { api.createDiagnosis(match { it.enfermedad == "Tomato___Early_blight" }) } throws
            IOException("se cortó")

        val remaining = repository.syncPending(userId = 7)

        assertEquals(1, remaining)
        assertEquals(15, dao.get("local-1")?.remoteId)
        assertNull(dao.get("local-2")?.remoteId)
        coVerify(exactly = 2) { api.createDiagnosis(any()) }
    }

    @Test
    fun `agrega al carrito la primera oferta con existencias`() = runTest {
        coEvery { api.getProductById(4) } returns Response.success(
            productDetail(listOf(offer(idInventario = 30, stock = 0, distribuidor = "Agro Uno"), offer(31, 5, "Agro Dos")))
        )
        coEvery { api.addToCart(12, AddItemRequest(idInventario = 31, cantidad = 1)) } returns
            Response.success(emptyMap())

        val result = repository().addToCart(farmerId = 12, productId = 4)

        assertEquals(CartAddResult.Added("Agro Dos"), result)
        coVerify(exactly = 1) { api.addToCart(12, AddItemRequest(31, 1)) }
    }

    @Test
    fun `sin existencias no intenta agregar al carrito`() = runTest {
        coEvery { api.getProductById(4) } returns Response.success(productDetail(emptyList()))

        assertEquals(CartAddResult.NoStock, repository().addToCart(farmerId = 12, productId = 4))
        coVerify(exactly = 1) { api.getProductById(4) }
        confirmVerified(api)
    }

    private fun record(id: Int) = DiagnosisRecord(
        id = id,
        cultivo = "Tomate",
        enfermedad = "Tomato___Late_blight",
        confianza = 0.91,
        foto = "local://diagnostico/local-1"
    )

    private fun offer(idInventario: Int, stock: Int, distribuidor: String) = DistributorOffer(
        idInventario = idInventario,
        precio = 50.0,
        stock = stock,
        unidadMedida = "L",
        idDistribuidor = 1,
        distribuidor = distribuidor,
        calificacionDistribuidor = 4.5,
        estadoVerificacion = "verificado"
    )

    private fun productDetail(offers: List<DistributorOffer>) = ProductDetail(
        id = 4,
        nombre = "Fungicida",
        marca = null,
        descripcion = null,
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 4.0,
        categoria = "Fungicidas",
        ofertas = offers
    )
}

private class FakeConnectivity(var online: Boolean) : ConnectivityMonitor {
    override val status: Flow<Boolean> = MutableStateFlow(online)
    override fun isOnline(): Boolean = online
}

private class FakePendingDiagnosisDao : PendingDiagnosisDao {
    private val rows = linkedMapOf<String, PendingDiagnosisEntity>()

    override suspend fun insert(diagnosis: PendingDiagnosisEntity) {
        rows[diagnosis.localId] = diagnosis
    }

    override suspend fun get(localId: String): PendingDiagnosisEntity? = rows[localId]

    override suspend fun unsent(userId: Int): List<PendingDiagnosisEntity> =
        rows.values.filter { it.userId == userId && it.remoteId == null }.sortedBy { it.createdAt }

    override suspend fun markSent(localId: String, remoteId: Int) {
        rows[localId]?.let { rows[localId] = it.copy(remoteId = remoteId) }
    }

    override suspend fun delete(localId: String) {
        rows.remove(localId)
    }

    override suspend fun deleteSentBefore(before: Long) {
        rows.values.removeAll { it.remoteId != null && it.createdAt < before }
    }
}
