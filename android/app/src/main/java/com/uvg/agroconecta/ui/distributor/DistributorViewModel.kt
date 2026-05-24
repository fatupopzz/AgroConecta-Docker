package com.uvg.agroconecta.ui.distributor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.data.models.CreateReviewRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DistributorUiState(
    val distributorName: String = "",
    val isVerified: Boolean = false,
    val rating: DistributorRatingResponse? = null,
    val reviews: List<DistributorReview> = emptyList(),
    val productos: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
    val reviewSubmitError: String? = null
)

class DistributorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DistributorUiState())
    val uiState: StateFlow<DistributorUiState> = _uiState.asStateFlow()

    fun loadAll(distributorId: Int, token: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val api = RetrofitClient.getService(token)

                // Datos del distribuidor
                val distResponse = api.getDistributorById(distributorId)
                if (distResponse.isSuccessful) {
                    val dist = distResponse.body()!!
                    _uiState.update {
                        it.copy(
                            distributorName = dist.nombreNegocio,
                            isVerified = dist.estadoVerificacion == "verificado"
                        )
                    }
                }

                // Rating
                val ratingResponse = RetrofitClient.getService().getDistributorRating(distributorId)
                if (ratingResponse.isSuccessful) {
                    _uiState.update { it.copy(rating = ratingResponse.body()) }
                }

                // Reseñas
                val reviewsResponse = RetrofitClient.getService().getDistributorReviews(
                    distributorId,
                    page = 1,
                    limit = 20
                )
                if (reviewsResponse.isSuccessful) {
                    _uiState.update {
                        it.copy(reviews = reviewsResponse.body()?.reviews ?: emptyList())
                    }
                }

                // Productos del distribuidor — filtramos del catálogo general
                val productosResponse = RetrofitClient.getService().getProducts(limit = 100)
                if (productosResponse.isSuccessful) {
                    // Traemos detalle de cada producto para ver si tiene oferta de este distribuidor
                    val todosProductos = productosResponse.body()?.products ?: emptyList()
                    val productosDelDist = mutableListOf<Product>()

                    for (producto in todosProductos) {
                        try {
                            val detalle = RetrofitClient.getService(token).getProductById(producto.id)
                            if (detalle.isSuccessful) {
                                val tieneOferta = detalle.body()?.ofertas
                                    ?.any { it.idDistribuidor == distributorId } == true
                                if (tieneOferta) productosDelDist.add(producto)
                            }
                        } catch (_: Exception) { }
                    }

                    _uiState.update { it.copy(productos = productosDelDist) }
                }

            } catch (e: Exception) {
                // loading termina igual
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitReview(
        distributorId: Int,
        calificacion: Int,
        comentario: String,
        token: String?
    ) {
        if (token == null) {
            _uiState.update { it.copy(reviewSubmitError = "Sesión inválida") }
            return
        }

        // Las reseñas de distribuidores van por producto, no directamente por distribuidor.
        // Necesitamos al menos un producto del distribuidor para dejar la reseña.
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
                val response = RetrofitClient.getService(token).createReview(
                    productoId = primerProducto.id,
                    token = "Bearer $token",
                    body = CreateReviewRequest(
                        calificacion = calificacion,
                        comentario = comentario
                    )
                )
                if (response.isSuccessful) {
                    _uiState.update { it.copy(reviewSubmitSuccess = true) }
                    // Recargar reseñas
                    val reviewsResponse = RetrofitClient.getService().getDistributorReviews(
                        distributorId, page = 1, limit = 20
                    )
                    if (reviewsResponse.isSuccessful) {
                        _uiState.update {
                            it.copy(reviews = reviewsResponse.body()?.reviews ?: emptyList())
                        }
                    }
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