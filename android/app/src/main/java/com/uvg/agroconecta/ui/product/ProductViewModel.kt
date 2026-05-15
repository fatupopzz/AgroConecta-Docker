package com.uvg.agroconecta.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.*
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _productDetail = MutableLiveData<ProductDetail?>()
    val productDetail: LiveData<ProductDetail?> = _productDetail

    private val _comparison = MutableLiveData<PriceComparison?>()
    val comparison: LiveData<PriceComparison?> = _comparison

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // oferta seleccionada en el comparador (por defecto la más barata)
    private val _selectedOffer = MutableLiveData<DistributorOffer?>()
    val selectedOffer: LiveData<DistributorOffer?> = _selectedOffer

    // feedback del carrito
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
                    // preseleccionar la oferta más barata
                    _selectedOffer.value = product.ofertas.minByOrNull { it.precio }
                } else {
                    _error.value = "Producto no encontrado"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadComparison(id: Int, token: String?) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).compareProductPrices(id)
                if (response.isSuccessful) {
                    _comparison.value = response.body()
                }
            } catch (e: Exception) {
                // fallo silencioso — la ficha técnica sigue siendo útil
            }
        }
    }

    fun selectOffer(offer: DistributorOffer) {
        _selectedOffer.value = offer
    }

    fun addToCart(idAgricultor: Int, token: String) {
        val offer = _selectedOffer.value ?: run {
            _error.value = "Seleccioná un distribuidor primero"
            return
        }
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
                    _cartSuccess.value = "¡Producto agregado al carrito!"
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
}