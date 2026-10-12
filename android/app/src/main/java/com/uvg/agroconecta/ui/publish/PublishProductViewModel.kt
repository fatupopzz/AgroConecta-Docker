package com.uvg.agroconecta.ui.publish

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.images.ProductImageRules
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.CreateInventoryRequest
import com.uvg.agroconecta.data.models.CreateProductRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PublishState {
    object Idle : PublishState()
    object Loading : PublishState()
    object Success : PublishState()

    /**
     * El producto y el inventario sí quedaron creados; solo falló la foto.
     * Es un estado aparte de Error porque reintentar la publicación duplicaría
     * el producto, y la pantalla debe salir igual que en un éxito.
     */
    data class PublishedWithoutImage(val message: String) : PublishState()
    data class Error(val message: String) : PublishState()
}

data class PublishUiState(
    val categorias: List<Category> = emptyList(),
    val isLoadingCategorias: Boolean = false,
    val publishState: PublishState = PublishState.Idle,
    val imagen: SelectedProductImage? = null,
    val isReadingImage: Boolean = false,
    val imageError: String? = null
)

@HiltViewModel
class PublishProductViewModel @Inject constructor(
    private val api: ApiService,
    private val imageReader: ProductImageReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    init {
        loadCategorias()
    }

    private fun loadCategorias() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategorias = true) }
            try {
                val response = api.getCategories()
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

    /**
     * Recibe lo que devuelve el PhotoPicker. Null significa que el usuario lo
     * cerró sin elegir, y en ese caso se conserva la foto que ya tenía.
     */
    fun onImageSelected(uri: Uri?) {
        if (uri == null) return

        _uiState.update { it.copy(isReadingImage = true, imageError = null) }
        viewModelScope.launch {
            when (val result = imageReader.read(uri)) {
                is ProductImageReadResult.Success -> _uiState.update {
                    it.copy(imagen = result.image, isReadingImage = false)
                }
                is ProductImageReadResult.Invalid -> _uiState.update {
                    it.copy(imageError = result.message, isReadingImage = false)
                }
            }
        }
    }

    fun removeImage() {
        _uiState.update { it.copy(imagen = null) }
    }

    fun clearImageError() {
        _uiState.update { it.copy(imageError = null) }
    }

    fun publishProduct(
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
                // Paso 1: crear el producto
                val productResponse = api.createProduct(
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
                val inventoryResponse = api.createInventory(
                    request = CreateInventoryRequest(
                        idProducto = idProducto,
                        precio = precio,
                        stock = stock,
                        unidadMedida = unidadMedida.trim().ifBlank { null },
                        tiempoEntrega = tiempoEntrega
                    )
                )

                if (!inventoryResponse.isSuccessful) {
                    _uiState.update {
                        it.copy(publishState = PublishState.Error(
                            "Producto creado pero error en inventario (${inventoryResponse.code()})"
                        ))
                    }
                    return@launch
                }

                // Paso 3: la foto va al final porque el backend solo deja subirla
                // a quien ya tiene el producto en su inventario.
                val imagen = _uiState.value.imagen
                val imageFailure = imagen?.let { uploadImage(idProducto, it) }
                _uiState.update {
                    it.copy(
                        publishState = if (imageFailure == null) {
                            PublishState.Success
                        } else {
                            PublishState.PublishedWithoutImage(imageFailure)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(publishState = PublishState.Error("Error de conexión: ${e.localizedMessage}"))
                }
            }
        }
    }

    /**
     * Devuelve null si la foto subió, o el mensaje para el usuario si no.
     *
     * Atrapa sus propios errores: una excepción aquí no debe caer en el catch
     * general, que diría "Error de conexión" como si el producto no existiera.
     */
    private suspend fun uploadImage(idProducto: Int, imagen: SelectedProductImage): String? {
        val response = try {
            api.uploadProductImage(idProducto, imagen.toMultipartPart())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return "Producto publicado, pero no se pudo subir la foto por un problema de conexión"
        }

        if (response.isSuccessful) return null

        val reason = ProductImageRules.uploadFailureReason(response.code())
        return "Producto publicado, pero no se pudo subir la foto: $reason"
    }

    fun resetState() {
        _uiState.update { it.copy(publishState = PublishState.Idle) }
    }
}
