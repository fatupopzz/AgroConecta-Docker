package com.uvg.agroconecta.ui.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.CreateInventoryRequest
import com.uvg.agroconecta.data.models.CreateProductRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class PublishState {
    object Idle : PublishState()
    object Loading : PublishState()
    object Success : PublishState()
    data class Error(val message: String) : PublishState()
}

data class PublishUiState(
    val categorias: List<Category> = emptyList(),
    val isLoadingCategorias: Boolean = false,
    val publishState: PublishState = PublishState.Idle
)

class PublishProductViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    init {
        loadCategorias()
    }

    private fun loadCategorias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategorias = true) }
            try {
                val response = RetrofitClient.getService().getCategories()
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            categorias = response.body() ?: emptyList(),
                            isLoadingCategorias = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoadingCategorias = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingCategorias = false) }
            }
        }
    }

    fun publishProduct(
        token: String,
        nombre: String,
        marca: String,
        descripcion: String,
        idCategoria: Int,
        precio: Double,
        stock: Int,
        unidadMedida: String,
        tiempoEntrega: Int?
    ) {
        _uiState.update { it.copy(publishState = PublishState.Loading) }

        viewModelScope.launch {
            try {
                val bearer = "Bearer $token"

                // Paso 1: crear el producto
                val productResponse = RetrofitClient.getService().createProduct(
                    token = bearer,
                    request = CreateProductRequest(
                        nombre = nombre.trim(),
                        marca = marca.trim().ifBlank { null },
                        descripcion = descripcion.trim().ifBlank { null },
                        idCategoria = idCategoria,
                        composicion = null,
                        dosis = null,
                        instrucciones = null
                    )
                )

                if (!productResponse.isSuccessful) {
                    _uiState.update {
                        it.copy(publishState = PublishState.Error(
                            "Error al crear producto (${productResponse.code()})"
                        ))
                    }
                    return@launch
                }

                val idProducto = productResponse.body()?.producto?.idProducto
                if (idProducto == null) {
                    _uiState.update {
                        it.copy(publishState = PublishState.Error("Error: ID de producto inválido"))
                    }
                    return@launch
                }

                // Paso 2: crear inventario
                val inventoryResponse = RetrofitClient.getService().createInventory(
                    token = bearer,
                    request = CreateInventoryRequest(
                        idProducto = idProducto,
                        precio = precio,
                        stock = stock,
                        unidadMedida = unidadMedida.trim().ifBlank { null },
                        tiempoEntrega = tiempoEntrega
                    )
                )

                if (inventoryResponse.isSuccessful) {
                    _uiState.update { it.copy(publishState = PublishState.Success) }
                } else {
                    _uiState.update {
                        it.copy(publishState = PublishState.Error(
                            "Producto creado pero error en inventario (${inventoryResponse.code()})"
                        ))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(publishState = PublishState.Error("Error de conexión: ${e.localizedMessage}"))
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { it.copy(publishState = PublishState.Idle) }
    }
}