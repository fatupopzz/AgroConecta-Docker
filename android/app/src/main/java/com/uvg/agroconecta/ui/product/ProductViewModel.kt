package com.uvg.agroconecta.ui.product

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.PriceComparison
import com.uvg.agroconecta.data.models.ProductDetail
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {

    private val _productDetail = MutableLiveData<ProductDetail?>()
    val productDetail: LiveData<ProductDetail?> = _productDetail

    private val _comparison = MutableLiveData<PriceComparison?>()
    val comparison: LiveData<PriceComparison?> = _comparison

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadProduct(id: Int, token: String?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.getProductById(id)
                if (response.isSuccessful) {
                    _productDetail.value = response.body()
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
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.compareProductPrices(id)
                if (response.isSuccessful) {
                    _comparison.value = response.body()
                } else {
                    _error.value = "Error al comparar precios"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
