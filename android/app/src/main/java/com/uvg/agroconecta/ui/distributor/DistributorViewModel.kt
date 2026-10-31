package com.uvg.agroconecta.ui.distributor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.models.CreateReviewRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DistributorUiState(
    val distributorName: String = "",
    val isVerified: Boolean = false,
    val rating: DistributorRatingResponse? = null,
    val reviews: List<DistributorReview> = emptyList(),
    val promedio: Double? = null,
    val productos: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
    val reviewSubmitError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DistributorViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistributorUiState())
    val uiState: StateFlow<DistributorUiState> = _uiState.asStateFlow()

    /**
     * Carga el perfil completo. Las cuatro consultas no dependen entre sí, así
     * que van en paralelo: el perfil tarda lo que tarda la más lenta y no la
     * suma de todas, algo que se nota con señal débil.
     */
    fun loadAll(distributorId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                coroutineScope {
                    launch { loadDistributor(distributorId) }
                    launch { reloadReviews(distributorId) }
                    launch { reloadRating(distributorId) }
                    launch { loadProducts(distributorId) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error al cargar el perfil del distribuidor")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadDistributor(distributorId: Int) {
        val response = api.getDistributorById(distributorId)
        val dist = response.body()
        if (response.isSuccessful && dist != null) {
            _uiState.update {
                it.copy(
                    distributorName = dist.nombreNegocio,
                    isVerified = dist.estadoVerificacion == "verificado"
                )
            }
        }
    }

    // Una sola llamada. Antes se bajaban 100 productos del catálogo y se pedía
    // el detalle de cada uno para ver si este distribuidor lo vendía.
    private suspend fun loadProducts(distributorId: Int) {
        val response = api.getDistributorProducts(distributorId)
        if (response.isSuccessful) {
            _uiState.update { it.copy(productos = response.body().orEmpty()) }
        }
    }

    private suspend fun reloadReviews(distributorId: Int) {
        try {
            val reviewsResponse = api.getDistributorReviews(
                distributorId,
                page = 1,
                limit = 20
            )
            if (reviewsResponse.isSuccessful) {
                _uiState.update {
                    it.copy(reviews = reviewsResponse.body()?.reviews ?: emptyList())
                }
            }
        } catch (_: Exception) { }
    }

    // El promedio real viene del servidor; antes se calculaba con las primeras
    // 20 reseñas descargadas y no coincidía con el de Inicio.
    private suspend fun reloadRating(distributorId: Int) {
        try {
            val response = api.getDistributorRating(distributorId)
            if (response.isSuccessful) {
                _uiState.update { it.copy(rating = response.body()) }
            }
        } catch (_: Exception) { }
    }

    fun submitReview(
        distributorId: Int,
        calificacion: Int,
        comentario: String
    ) {
        _uiState.update { it.copy(isSubmittingReview = true) }
        viewModelScope.launch {
            try {
                val response = api.createDistributorReview(
                    id = distributorId,
                    body = CreateReviewRequest(
                        calificacion = calificacion,
                        comentario = comentario
                    )
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(reviewSubmitSuccess = true) }
                    reloadReviews(distributorId)
                    reloadRating(distributorId)
                } else {
                    val msg = when (response.code()) {
                        409 -> "Ya dejaste una reseña para este distribuidor"
                        401 -> "Sesión expirada, volvé a iniciar sesión"
                        else -> "No se pudo enviar la reseña (${response.code()})"
                    }
                    _uiState.update { it.copy(reviewSubmitError = msg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(reviewSubmitError = "Error de conexión") }
            } finally {
                _uiState.update { it.copy(isSubmittingReview = false) }
            }
        }
    }

    fun clearReviewMessages() {
        _uiState.update { it.copy(reviewSubmitSuccess = false, reviewSubmitError = null) }
    }
}