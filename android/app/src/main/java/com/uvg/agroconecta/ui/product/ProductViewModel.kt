package com.uvg.agroconecta.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.*
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    // ── Catálogo ──────────────────────────────────────────────────────────────

    private val _products = MutableLiveData<List<Product>>(emptyList())
    val products: LiveData<List<Product>> = _products

    private val _isLoadingCatalog = MutableLiveData(false)
    val isLoadingCatalog: LiveData<Boolean> = _isLoadingCatalog

    private val _catalogError = MutableLiveData<String?>()
    val catalogError: LiveData<String?> = _catalogError

    fun loadProducts(
        token: String?,
        nombre: String? = null,
        idCategoria: Int? = null
    ) {
        _isLoadingCatalog.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token)
                    .getProducts(nombre = nombre, idCategoria = idCategoria)
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

    private val _cartSuccess = MutableLiveData<String?>()
    val cartSuccess: LiveData<String?> = _cartSuccess

    private val _isAddingToCart = MutableLiveData(false)
    val isAddingToCart: LiveData<Boolean> = _isAddingToCart

    fun loadProduct(id: Int, token: String?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).getProductById(id)
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

    fun loadComparison(productoId: Int, token: String?) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).compareProductPrices(productoId)
                if (response.isSuccessful) {
                    _comparison.value = response.body()
                }
            } catch (_: Exception) { }
        }
    }

    fun selectOffer(offer: DistributorOffer) {
        _selectedOffer.value = offer
    }

    fun addToCart(idAgricultor: Int, token: String) {
        val offer = _selectedOffer.value ?: return
        _isAddingToCart.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).addToCart(
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

    fun loadReviews(productoId: Int, token: String?) {
        _reviewsLoading.value = true
        viewModelScope.launch {
            try {
                val auth = token?.let { "Bearer $it" }
                val response = RetrofitClient.getService(token).getReviews(productoId, auth)
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

    fun submitReview(productoId: Int, calificacion: Int, comentario: String, token: String?) {
        if (token == null) {
            _reviewSubmitState.value = ReviewSubmitState.Error("Sesión inválida, volvé a iniciar sesión.")
            return
        }
        _reviewSubmitState.value = ReviewSubmitState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).createReview(
                    productoId = productoId,
                    token      = "Bearer $token",
                    body       = CreateReviewRequest(calificacion = calificacion, comentario = comentario)
                )
                if (response.isSuccessful) {
                    _reviewSubmitState.value = ReviewSubmitState.Success
                    loadReviews(productoId, token)
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