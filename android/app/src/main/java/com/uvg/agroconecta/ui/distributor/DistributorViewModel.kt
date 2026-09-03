package com.uvg.agroconecta.ui.distributor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.models.CreateReviewRequest
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun loadAll(distributorId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Datos del distribuidor — safe body
                val distResponse = api.getDistributorById(distributorId)
                if (distResponse.isSuccessful) {
                    val dist = distResponse.body()
                    if (dist != null) {
                        _uiState.update {
                            it.copy(
                                distributorName = dist.nombreNegocio,
                                isVerified = dist.estadoVerificacion == "verificado"
                            )
                        }
                    }
                }

                // Reseñas
                reloadReviews(distributorId)

                // Productos — loop con for para poder usar suspend dentro de corrutina
                val productosResponse = api.getProducts(limit = 100)
                if (productosResponse.isSuccessful) {
                    val todosProductos = productosResponse.body()?.products ?: emptyList()
                    val productosDelDist = mutableListOf<Product>()

                    for (producto in todosProductos) {
                        try {
                            val detalle = api.getProductById(producto.id)
                            val perteneceAlDistribuidor = detalle.isSuccessful &&
                                    detalle.body()?.ofertas
                                        ?.any { it.idDistribuidor == distributorId } == true
                            if (perteneceAlDistribuidor) {
                                productosDelDist.add(producto)
                            }
                        } catch (_: Exception) {
                            // Si falla el detalle de un producto, se omite
                        }
                    }

                    _uiState.update { it.copy(productos = productosDelDist) }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error al cargar el perfil del distribuidor")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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

    fun submitReview(
        distributorId: Int,
        calificacion: Int,
        comentario: String
    ) {
        val primerProducto = _uiState.value.productos.firstOrNull()
        if (primerProducto == null) {
            _uiState.update {
                it.copy(reviewSubmitError = "Este distribuidor no tiene productos para reseñar")
            }
            return
        }

        _uiState.update { it.copy(isSubmittingReview = true) }
        viewModelScope.launch {
            try {
                val response = api.createReview(
                    productoId = primerProducto.id,
                    body = CreateReviewRequest(
                        calificacion = calificacion,
                        comentario = comentario
                    )
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(reviewSubmitSuccess = true) }
                    reloadReviews(distributorId)
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