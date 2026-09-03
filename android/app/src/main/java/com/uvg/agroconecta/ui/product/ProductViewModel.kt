package com.uvg.agroconecta.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    // ── Catálogo ──────────────────────────────────────────────────────────────

    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products

    private val _isLoadingCatalog = MutableLiveData(false)
    val isLoadingCatalog: LiveData<Boolean> = _isLoadingCatalog

    private val _catalogError = MutableLiveData<String?>()
    val catalogError: LiveData<String?> = _catalogError

    fun loadProducts(
        nombre: String? = null,
        idCategoria: Int? = null
    ) {
        _isLoadingCatalog.value = true
        viewModelScope.launch {
            try {
                val response = api.getProducts(
                    nombre = nombre,
                    idCategoria = idCategoria
                )
                if (response.isSuccessful) {
                    _products.value = response.body()?.products ?: emptyList()
                } else {
                    _catalogError.value = "Error al cargar productos (${response.code()})"
                }
            } catch (e: Exception) {
                _catalogError.value = "Sin conexión. Verificá tu red."
            } finally {
                _isLoadingCatalog.value = false
            }
        }
    }

    // ── Detalle de producto ───────────────────────────────────────────────────

    private val _productDetail = MutableLiveData<ProductDetail?>()
    val productDetail: LiveData<ProductDetail?> = _productDetail

    private val _comparison = MutableLiveData<PriceComparison?>()
    val comparison: LiveData<PriceComparison?> = _comparison

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedOffer = MutableLiveData<DistributorOffer?>()
    val selectedOffer: LiveData<DistributorOffer?> = _selectedOffer

    private val _distributorRating = MutableLiveData<DistributorRatingResponse?>()
    val distributorRating: LiveData<DistributorRatingResponse?> = _distributorRating

    private val _distributorReviews = MutableLiveData<List<DistributorReview>>(emptyList())
    val distributorReviews: LiveData<List<DistributorReview>> = _distributorReviews

    private val _isLoadingDistributorRating = MutableLiveData(false)
    val isLoadingDistributorRating: LiveData<Boolean> = _isLoadingDistributorRating

    private val _cartSuccess = MutableLiveData<String?>()
    val cartSuccess: LiveData<String?> = _cartSuccess

    private val _isAddingToCart = MutableLiveData(false)
    val isAddingToCart: LiveData<Boolean> = _isAddingToCart

    private val _isFollowingPrice = MutableLiveData(false)
    val isFollowingPrice: LiveData<Boolean> = _isFollowingPrice

    private val _isUpdatingFollow = MutableLiveData(false)
    val isUpdatingFollow: LiveData<Boolean> = _isUpdatingFollow

    private val _followPriceMessage = MutableLiveData<String?>()
    val followPriceMessage: LiveData<String?> = _followPriceMessage

    fun loadProduct(id: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = api.getProductById(id)
                if (response.isSuccessful) {
                    val product = response.body()!!
                    _productDetail.value = product
                    _selectedOffer.value = product.ofertas.minByOrNull { it.precio }
                } else {
                    _error.value = "Error al cargar el producto (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Sin conexión. Verificá tu red."
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Sin sesion el backend contesta 401/403 y el producto queda como no
    // seguido, que es justo lo que se muestra: no hace falta chequear antes.
    fun loadFollowStatus(productoId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getProductFollowStatus(productoId)
                if (response.isSuccessful) {
                    _isFollowingPrice.value = response.body()?.siguiendo == true
                } else if (response.code() == 401 || response.code() == 403) {
                    _isFollowingPrice.value = false
                } else {
                    _isFollowingPrice.value = false
                }
            } catch (_: Exception) {
                _isFollowingPrice.value = false
            }
        }
    }

    fun toggleFollowPrice(productoId: Int) {
        val shouldFollow = _isFollowingPrice.value != true
        _isUpdatingFollow.value = true

        viewModelScope.launch {
            try {
                val response = if (shouldFollow) {
                    api.followProductPrice(productoId)
                } else {
                    api.unfollowProductPrice(productoId)
                }

                if (response.isSuccessful) {
                    _isFollowingPrice.value = response.body()?.siguiendo ?: shouldFollow
                    _followPriceMessage.value = if (shouldFollow) {
                        "Ahora seguís el precio de este producto"
                    } else {
                        "Dejaste de seguir el precio"
                    }
                } else {
                    _error.value = when (response.code()) {
                        401 -> "Sesión expirada. Volvé a iniciar sesión."
                        403 -> "Solo agricultores pueden seguir precios."
                        409 -> "Este producto aún no tiene precio disponible."
                        else -> "No se pudo actualizar el seguimiento (${response.code()})"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Sin conexión. Verificá tu red."
            } finally {
                _isUpdatingFollow.value = false
            }
        }
    }

    fun loadComparison(productoId: Int) {
        viewModelScope.launch {
            try {
                val response = api.compareProductPrices(productoId)
                if (response.isSuccessful) {
                    _comparison.value = response.body()
                }
            } catch (_: Exception) { }
        }
    }

    fun selectOffer(offer: DistributorOffer) {
        _selectedOffer.value = offer
    }

    fun loadDistributorRating(distributorId: Int) {
        val requestedDistributorId = distributorId
        _isLoadingDistributorRating.value = true
        viewModelScope.launch {
            try {
                val ratingResponse = api.getDistributorRating(requestedDistributorId)
                val reviewsResponse = api.getDistributorReviews(
                    requestedDistributorId,
                    page = 1,
                    limit = 5
                )

                if (_selectedOffer.value?.idDistribuidor != requestedDistributorId) {
                    return@launch
                }

                if (ratingResponse.isSuccessful) {
                    _distributorRating.value = ratingResponse.body()
                } else {
                    _distributorRating.value = null
                }

                if (reviewsResponse.isSuccessful) {
                    _distributorReviews.value = reviewsResponse.body()?.reviews ?: emptyList()
                } else {
                    _distributorReviews.value = emptyList()
                }
            } catch (e: Exception) {
                if (_selectedOffer.value?.idDistribuidor == requestedDistributorId) {
                    _distributorRating.value = null
                    _distributorReviews.value = emptyList()
                }
            } finally {
                if (_selectedOffer.value?.idDistribuidor == requestedDistributorId) {
                    _isLoadingDistributorRating.value = false
                }
            }
        }
    }

    fun addToCart(idAgricultor: Int) {
        val offer = _selectedOffer.value ?: return
        _isAddingToCart.value = true
        viewModelScope.launch {
            try {
                val response = api.addToCart(
                    idAgricultor = idAgricultor,
                    request = AddItemRequest(
                        idInventario = offer.idInventario,
                        cantidad = 1
                    )
                )
                if (response.isSuccessful) {
                    _cartSuccess.value = "✓ Producto agregado al carrito"
                } else {
                    _error.value = "No se pudo agregar al carrito (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Sin conexión. Verificá tu red."
            } finally {
                _isAddingToCart.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _cartSuccess.value = null
        _followPriceMessage.value = null
    }

    // ── Reseñas (KAN-48) ─────────────────────────────────────────────────────

    private val _reviews = MutableLiveData<List<Review>>(emptyList())
    val reviews: LiveData<List<Review>> = _reviews

    private val _reviewsPromedio = MutableLiveData<Double?>(null)
    val reviewsPromedio: LiveData<Double?> = _reviewsPromedio

    private val _reviewsLoading = MutableLiveData(false)
    val reviewsLoading: LiveData<Boolean> = _reviewsLoading

    private val _reviewSubmitState = MutableLiveData<ReviewSubmitState>(ReviewSubmitState.Idle)
    val reviewSubmitState: LiveData<ReviewSubmitState> = _reviewSubmitState

    fun loadReviews(productoId: Int) {
        _reviewsLoading.value = true
        viewModelScope.launch {
            try {
                val response = api.getReviews(productoId)
                if (response.isSuccessful) {
                    val body = response.body()
                    _reviews.value         = body?.reviews ?: emptyList()
                    _reviewsPromedio.value = body?.promedio
                } else {
                    _reviews.value = emptyList()
                }
            } catch (e: Exception) {
                _reviews.value = emptyList()
            } finally {
                _reviewsLoading.value = false
            }
        }
    }

    fun submitReview(productoId: Int, calificacion: Int, comentario: String) {
        _reviewSubmitState.value = ReviewSubmitState.Loading
        viewModelScope.launch {
            try {
                val response = api.createReview(
                    productoId = productoId,
                    body       = CreateReviewRequest(calificacion = calificacion, comentario = comentario)
                )
                if (response.isSuccessful) {
                    _reviewSubmitState.value = ReviewSubmitState.Success
                    loadReviews(productoId)
                } else {
                    val msg = when (response.code()) {
                        409  -> "Ya enviaste una reseña para este producto."
                        401  -> "Sesión expirada. Volvé a iniciar sesión."
                        else -> "No se pudo enviar la reseña (${response.code()})."
                    }
                    _reviewSubmitState.value = ReviewSubmitState.Error(msg)
                }
            } catch (e: Exception) {
                _reviewSubmitState.value = ReviewSubmitState.Error("Error de conexión. Intentá de nuevo.")
            }
        }
    }

    fun clearReviewMessages() {
        _reviewSubmitState.value = ReviewSubmitState.Idle
    }

}
