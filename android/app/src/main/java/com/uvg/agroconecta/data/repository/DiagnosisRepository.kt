package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.local.PendingDiagnosisDao
import com.uvg.agroconecta.data.local.PendingDiagnosisEntity
import com.uvg.agroconecta.data.models.AddItemRequest
import com.uvg.agroconecta.data.models.DiagnosisAlertRequest
import com.uvg.agroconecta.data.models.DiagnosisRequest
import com.uvg.agroconecta.data.models.Product
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

/** Diagnóstico listo para guardar. [diseaseLabel] es la etiqueta original del modelo. */
data class DiagnosisSubmission(
    val userId: Int,
    val crop: String,
    val diseaseLabel: String,
    val confidence: Float,
    val latitude: Double?,
    val longitude: Double?
)

sealed interface DiagnosisUpload {
    data class Sent(val diagnosisId: Int) : DiagnosisUpload

    /** Quedó guardado en el teléfono y se envía cuando haya señal. */
    data class Pending(val localId: String) : DiagnosisUpload
}

data class DiagnosisProducts(
    val products: List<Product>,
    val activeIngredients: List<String>,
    val generalRecommendation: String?
)

sealed interface CartAddResult {
    data class Added(val distributor: String) : CartAddResult

    /** Ningún distribuidor tiene existencias del producto. */
    data object NoStock : CartAddResult
}

/** El backend rechazó el diagnóstico con 400: reintentarlo no lo va a arreglar. */
class DiagnosisRejectedException(message: String) : Exception(message)

interface DiagnosisRepository {
    suspend fun submit(submission: DiagnosisSubmission): DiagnosisUpload

    /** Id del servidor si el pendiente ya llegó o acaba de enviarse; null si sigue sin señal. */
    suspend fun retryPending(localId: String): Int?

    /** Envía los pendientes del usuario y devuelve cuántos siguen sin enviar. */
    suspend fun syncPending(userId: Int): Int

    suspend fun recommendedProducts(diagnosisId: Int): DiagnosisProducts

    suspend fun publishAlert(diagnosisId: Int)

    suspend fun addToCart(farmerId: Int, productId: Int): CartAddResult
}

/** Programa el envío en segundo plano; es una interfaz para no depender de WorkManager en los tests. */
fun interface DiagnosisSyncScheduler {
    fun schedule()
}

class RemoteDiagnosisRepository(
    private val api: ApiService,
    private val dao: PendingDiagnosisDao,
    private val connectivity: ConnectivityMonitor,
    private val syncScheduler: DiagnosisSyncScheduler,
    private val now: () -> Long = System::currentTimeMillis,
    private val newLocalId: () -> String = { UUID.randomUUID().toString() }
) : DiagnosisRepository {

    // La pantalla y el worker pueden intentar enviar el mismo pendiente a la
    // vez al volver la señal; sin el mutex quedaría duplicado en el historial.
    private val sendMutex = Mutex()

    override suspend fun submit(submission: DiagnosisSubmission): DiagnosisUpload {
        val pending = PendingDiagnosisEntity(
            localId = newLocalId(),
            userId = submission.userId,
            crop = submission.crop,
            diseaseLabel = submission.diseaseLabel,
            confidence = submission.confidence,
            latitude = submission.latitude,
            longitude = submission.longitude,
            createdAt = now()
        )
        // Se guarda antes de intentar el envío: si la app se cierra a medio
        // camino, el diagnóstico no se pierde.
        dao.insert(pending)

        if (!connectivity.isOnline()) return keepPending(pending.localId)
        return try {
            DiagnosisUpload.Sent(send(pending))
        } catch (e: IOException) {
            keepPending(pending.localId)
        }
    }

    override suspend fun retryPending(localId: String): Int? {
        val pending = dao.get(localId) ?: return null
        pending.remoteId?.let { return it }
        return try {
            send(pending)
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun syncPending(userId: Int): Int {
        dao.deleteSentBefore(now() - SENT_RETENTION_MILLIS)
        var remaining = 0
        for (pending in dao.unsent(userId)) {
            try {
                send(pending)
            } catch (e: IOException) {
                remaining += 1
            } catch (e: DiagnosisRejectedException) {
                // Ya se borró de la cola; no tiene sentido detener el resto.
            }
        }
        return remaining
    }

    override suspend fun recommendedProducts(diagnosisId: Int): DiagnosisProducts {
        val response = api.getDiagnosisProducts(diagnosisId)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            throw IOException("No se pudieron cargar los productos (${response.code()})")
        }
        return DiagnosisProducts(
            products = body.productos,
            activeIngredients = body.ingredientesActivos,
            generalRecommendation = body.recomendacionGeneral
        )
    }

    override suspend fun publishAlert(diagnosisId: Int) {
        val response = api.publishDiagnosisAlert(diagnosisId, DiagnosisAlertRequest())
        if (!response.isSuccessful) {
            throw IOException("No se pudo publicar la alerta (${response.code()})")
        }
    }

    /**
     * Los productos del diagnóstico son del catálogo, sin distribuidor. El
     * carrito necesita una oferta concreta, así que se elige la más barata con
     * existencias; el backend ya las devuelve ordenadas por precio.
     */
    override suspend fun addToCart(farmerId: Int, productId: Int): CartAddResult {
        val detail = api.getProductById(productId)
        val offers = detail.body()?.ofertas
        if (!detail.isSuccessful || offers == null) {
            throw IOException("No se pudo consultar el producto (${detail.code()})")
        }
        val offer = offers.firstOrNull { it.stock > 0 } ?: return CartAddResult.NoStock

        val response = api.addToCart(farmerId, AddItemRequest(idInventario = offer.idInventario, cantidad = 1))
        if (!response.isSuccessful) {
            throw IOException("No se pudo agregar al carrito (${response.code()})")
        }
        return CartAddResult.Added(distributor = offer.distribuidor)
    }

    private fun keepPending(localId: String): DiagnosisUpload.Pending {
        syncScheduler.schedule()
        return DiagnosisUpload.Pending(localId)
    }

    private suspend fun send(pending: PendingDiagnosisEntity): Int = sendMutex.withLock {
        // Se vuelve a leer dentro del mutex: otro envío pudo terminar mientras
        // este esperaba.
        val current = dao.get(pending.localId) ?: throw DiagnosisRejectedException("El diagnóstico ya no está pendiente")
        current.remoteId?.let { return@withLock it }

        val response = api.createDiagnosis(
            DiagnosisRequest(
                cultivo = current.crop,
                enfermedad = current.diseaseLabel,
                confianza = current.confidence.toDouble(),
                foto = "$LOCAL_PHOTO_PREFIX${current.localId}",
                latitud = current.latitude,
                longitud = current.longitude
            )
        )
        val body = response.body()
        when {
            response.isSuccessful && body != null -> {
                dao.markSent(current.localId, body.id)
                body.id
            }
            response.code() == 400 -> {
                dao.delete(current.localId)
                throw DiagnosisRejectedException("El servidor rechazó el diagnóstico")
            }
            // 401 y 5xx se reintentan: la sesión puede renovarse y el servidor volver.
            else -> throw IOException("No se pudo enviar el diagnóstico (${response.code()})")
        }
    }

    companion object {
        const val LOCAL_PHOTO_PREFIX = "local://diagnostico/"
        private const val SENT_RETENTION_MILLIS = 24 * 60 * 60 * 1000L
    }
}
