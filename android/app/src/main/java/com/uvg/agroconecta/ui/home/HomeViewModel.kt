package com.uvg.agroconecta.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.models.Product
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _distributors = MutableLiveData<List<Distributor>>()
    val distributors: LiveData<List<Distributor>> = _distributors

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData(token: String?) {
        loadProducts(token)
        loadDistributors()
    }

    fun loadProducts(token: String?, page: Int = 1) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.getProducts(page = page, limit = 10)
                if (response.isSuccessful) {
                    _products.value = response.body()?.products ?: emptyList()
                } else {
                    _error.value = "Error al cargar productos"
                }
            } catch (e: Exception) {
                _error.value = "Sin conexión: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProductsByCategory(token: String?, categoryId: Int) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.getProducts(idCategoria = categoryId, limit = 20)
                if (response.isSuccessful) {
                    _products.value = response.body()?.products ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchProducts(token: String?, query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.getProducts(nombre = query, limit = 20)
                if (response.isSuccessful) {
                    _products.value = response.body()?.products ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadDistributors() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService().getVerifiedDistributors()
                if (response.isSuccessful) {
                    _distributors.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Silently fail for distributors section
            }
        }
    }
}
