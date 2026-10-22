package com.uvg.agroconecta.ui.diagnosis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.diagnosis.PlantDiagnosisResult
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseClassifier
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseLabel
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.repository.CartAddResult
import com.uvg.agroconecta.data.repository.DiagnosisRejectedException
import com.uvg.agroconecta.data.repository.DiagnosisRepository
import com.uvg.agroconecta.data.repository.DiagnosisSubmission
import com.uvg.agroconecta.data.repository.DiagnosisUpload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val UNCLEAR_PHOTO_MESSAGE =
    "La foto no es lo bastante clara para dar un diagnóstico. Toma otra de una sola hoja, de cerca y con buena luz."
const val PRODUCTS_WAITING_MESSAGE = "Los productos recomendados aparecerán cuando tengas señal."
const val ALERT_SENT_MESSAGE = "Avisamos a los agricultores cercanos"

sealed interface PlantDiagnosisUiState {
    data object Capture : PlantDiagnosisUiState
    data object Analyzing : PlantDiagnosisUiState
    data class UnclearPhoto(val confidence: Float) : PlantDiagnosisUiState

    data class Result(
        val label: PlantDiseaseLabel,
        val confidence: Float,
        val upload: UploadState = UploadState.Sending,
        val products: ProductsState,
        val hasLocation: Boolean = false,
        val alert: AlertState = AlertState.Idle,
        val addingToCart: Set<Int> = emptySet()
    ) : PlantDiagnosisUiState
}

sealed interface UploadState {
    data object Sending : UploadState
    data class Sent(val diagnosisId: Int) : UploadState
    data class Pending(val localId: String) : UploadState
    data class Failed(val message: String) : UploadState
}

sealed interface ProductsState {
    /** Hoja sana: no se recomienda nada. */
    data object NotApplicable : ProductsState
    data object WaitingForSignal : ProductsState
    data object Loading : ProductsState
    data class Loaded(
        val products: List<Product>,
        val recommendation: String?,
        val activeIngredients: List<String>
    ) : ProductsState
    data class Error(val message: String) : ProductsState
}

sealed interface AlertState {
    data object Idle : AlertState
    data object Sending : AlertState
    data object Sent : AlertState
}

@HiltViewModel
class PlantDiagnosisViewModel @Inject constructor(
    private val imageReader: ProductImageReader,
    private val classifier: PlantDiseaseClassifier,
    private val repository: DiagnosisRepository,
    private val locationProvider: CurrentLocationProvider,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlantDiagnosisUiState>(PlantDiagnosisUiState.Capture)
    val uiState: StateFlow<PlantDiagnosisUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var userId = 0
    private var farmerId = 0
    private var diagnosisJob: Job? = null

    /** La pantalla lee la sesión y la pasa aquí, igual que el calendario de cultivo. */
    fun start(userId: Int, farmerId: Int) {
        this.userId = userId
        this.farmerId = farmerId
    }

    fun onImageSelected(uri: Uri?) {
        // Cerrar la cámara o la galería sin elegir no cambia nada.
        if (uri == null || _uiState.value == PlantDiagnosisUiState.Analyzing) return
        diagnosisJob?.cancel()
        _uiState.value = PlantDiagnosisUiState.Analyzing
        diagnosisJob = viewModelScope.launch { diagnose(uri) }
    }

    fun takeAnotherPhoto() {
        diagnosisJob?.cancel()
        _uiState.value = PlantDiagnosisUiState.Capture
    }

    fun retryProducts() {
        val diagnosisId = sentDiagnosisId() ?: return
        viewModelScope.launch { loadProducts(diagnosisId) }
    }

    fun alertNeighbors() {
        val current = _uiState.value as? PlantDiagnosisUiState.Result ?: return
        val diagnosisId = sentDiagnosisId() ?: return
        if (current.label.isHealthy || !current.hasLocation || current.alert != AlertState.Idle) return

        updateResult { it.copy(alert = AlertState.Sending) }
        viewModelScope.launch {
            try {
                repository.publishAlert(diagnosisId)
                updateResult { it.copy(alert = AlertState.Sent) }
                _message.value = ALERT_SENT_MESSAGE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateResult { it.copy(alert = AlertState.Idle) }
                _message.value = "No se pudo avisar a tus vecinos. Intenta de nuevo."
            }
        }
    }

    fun addToCart(product: Product) {
        val current = _uiState.value as? PlantDiagnosisUiState.Result ?: return
        if (product.id in current.addingToCart) return
        if (farmerId <= 0) {
            _message.value = "Inicia sesión como agricultor para agregar productos al carrito."
            return
        }

        updateResult { it.copy(addingToCart = it.addingToCart + product.id) }
        viewModelScope.launch {
            _message.value = try {
                when (val result = repository.addToCart(farmerId, product.id)) {
                    is CartAddResult.Added -> "${product.nombre} agregado al carrito (${result.distributor})"
                    CartAddResult.NoStock -> "Ningún distribuidor tiene ${product.nombre} disponible por ahora"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "No se pudo agregar ${product.nombre} al carrito. Revisa tu conexión."
            } finally {
                updateResult { it.copy(addingToCart = it.addingToCart - product.id) }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun diagnose(uri: Uri) {
        // Se reutiliza la lectura de Mis productos: reduce la foto, la endereza y
        // descarta el GPS del EXIF. La ubicación se pide aparte y solo se usa
        // para el aviso a los vecinos.
        val image = when (val read = imageReader.read(uri)) {
            is ProductImageReadResult.Success -> read.image
            is ProductImageReadResult.Invalid -> {
                _uiState.value = PlantDiagnosisUiState.Capture
                _message.value = read.message
                return
            }
        }

        val result = try {
            classifier.classify(image.bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = PlantDiagnosisUiState.Capture
            _message.value = "No se pudo analizar la foto. Prueba con otra."
            return
        }

        val diagnosis = when (result) {
            is PlantDiagnosisResult.UnclearPhoto -> {
                _uiState.value = PlantDiagnosisUiState.UnclearPhoto(result.bestConfidence)
                return
            }
            is PlantDiagnosisResult.Diagnosed -> result
        }

        // El diagnóstico ya se muestra; guardar y pedir productos sigue detrás.
        _uiState.value = PlantDiagnosisUiState.Result(
            label = diagnosis.label,
            confidence = diagnosis.confidence,
            products = if (diagnosis.label.isHealthy) ProductsState.NotApplicable else ProductsState.Loading
        )

        val location = currentLocation()
        updateResult { it.copy(hasLocation = location != null) }

        val upload = try {
            repository.submit(
                DiagnosisSubmission(
                    userId = userId,
                    crop = diagnosis.label.crop,
                    diseaseLabel = diagnosis.label.label,
                    confidence = diagnosis.confidence,
                    latitude = location?.latitude,
                    longitude = location?.longitude
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: DiagnosisRejectedException) {
            markUploadFailed("El servidor no aceptó el diagnóstico.")
            return
        } catch (e: Exception) {
            markUploadFailed("No se pudo guardar el diagnóstico.")
            return
        }

        when (upload) {
            is DiagnosisUpload.Sent -> onSent(upload.diagnosisId, diagnosis.label)
            is DiagnosisUpload.Pending -> {
                updateResult {
                    it.copy(
                        upload = UploadState.Pending(upload.localId),
                        products = if (it.label.isHealthy) ProductsState.NotApplicable else ProductsState.WaitingForSignal
                    )
                }
                waitForSignal(upload.localId, diagnosis.label)
            }
        }
    }

    /**
     * Si la pantalla sigue abierta cuando vuelve la señal, se envía en ese
     * momento y aparecen los productos sin que el agricultor tenga que hacer
     * nada. Si la cierra, el worker lo envía igual.
     */
    private suspend fun waitForSignal(localId: String, label: PlantDiseaseLabel) {
        val diagnosisId = connectivityMonitor.status
            .filter { online -> online }
            .mapNotNull { retryPending(localId) }
            .first()
        onSent(diagnosisId, label)
    }

    private suspend fun retryPending(localId: String): Int? = try {
        repository.retryPending(localId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun onSent(diagnosisId: Int, label: PlantDiseaseLabel) {
        updateResult { it.copy(upload = UploadState.Sent(diagnosisId)) }
        if (!label.isHealthy) loadProducts(diagnosisId)
    }

    private suspend fun loadProducts(diagnosisId: Int) {
        updateResult { it.copy(products = ProductsState.Loading) }
        val products = try {
            val response = repository.recommendedProducts(diagnosisId)
            ProductsState.Loaded(
                products = response.products,
                recommendation = response.generalRecommendation,
                activeIngredients = response.activeIngredients
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ProductsState.Error("No se pudieron cargar los productos recomendados. Revisa tu conexión.")
        }
        updateResult { it.copy(products = products) }
    }

    private suspend fun currentLocation(): GeoCoordinates? = try {
        locationProvider.getCurrentCoordinates()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sin permiso o sin GPS el diagnóstico sigue; solo no se podrá avisar a los vecinos.
        null
    }

    private fun markUploadFailed(message: String) {
        updateResult {
            it.copy(
                upload = UploadState.Failed(message),
                products = if (it.label.isHealthy) ProductsState.NotApplicable else ProductsState.Error(message)
            )
        }
    }

    private fun sentDiagnosisId(): Int? =
        ((_uiState.value as? PlantDiagnosisUiState.Result)?.upload as? UploadState.Sent)?.diagnosisId

    private fun updateResult(transform: (PlantDiagnosisUiState.Result) -> PlantDiagnosisUiState.Result) {
        _uiState.update { state -> if (state is PlantDiagnosisUiState.Result) transform(state) else state }
    }
}
