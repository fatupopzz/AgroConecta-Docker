package com.uvg.agroconecta.ui.distributor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.images.ProductImageReadResult
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.images.ProductImageRules
import com.uvg.agroconecta.data.models.InventoryProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val UNVERIFIED_ACCOUNT_MESSAGE =
    "Tu cuenta aún no está verificada. Podrás cambiar las fotos cuando un administrador la apruebe."

data class MyProductsUiState(
    val products: List<InventoryProduct> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    // Arranca en true y solo pasa a false si el backend dice otra cosa: con un
    // backend viejo que no manda el estado no se bloquea a nadie, y si la
    // cuenta de verdad no está verificada el backend igual responde 403.
    val isVerified: Boolean = true,
    // Producto para el que se abrió el PhotoPicker. Vive aquí y no en la
    // pantalla para no perderlo si el sistema recrea la Activity mientras el
    // picker está abierto (por ejemplo, al girar el teléfono).
    val pendingPhotoProductId: Int? = null,
    val uploadingProductId: Int? = null,
    val message: String? = null
)

@HiltViewModel
class MyProductsViewModel @Inject constructor(
    private val api: ApiService,
    private val imageReader: ProductImageReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProductsUiState())
    val uiState: StateFlow<MyProductsUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val error = try {
                val response = api.getMyInventory()
                val body = response.body()
                when {
                    response.isSuccessful && body != null -> {
                        val estado = body.distribuidor?.estadoVerificacion
                        _uiState.update {
                            it.copy(
                                products = body.inventario,
                                isLoading = false,
                                isVerified = estado == null || estado == "verificado"
                            )
                        }
                        null
                    }
                    response.code() == 403 -> "Solo los distribuidores tienen productos publicados"
                    else -> "No se pudieron cargar tus productos (${response.code()})"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "No se pudieron cargar tus productos. Revisa tu conexión."
            }
            if (error != null) {
                _uiState.update { it.copy(isLoading = false, loadError = error) }
            }
        }
    }

    /**
     * Devuelve si se puede abrir el PhotoPicker. Con la cuenta sin verificar no
     * tiene sentido dejar elegir una foto que el backend va a rechazar.
     */
    fun onChangePhotoClick(productId: Int): Boolean {
        if (!_uiState.value.isVerified) {
            _uiState.update { it.copy(message = UNVERIFIED_ACCOUNT_MESSAGE) }
            return false
        }
        _uiState.update { it.copy(pendingPhotoProductId = productId) }
        return true
    }

    /**
     * Recibe lo que devuelve el PhotoPicker. Null es que el usuario lo cerró sin
     * elegir, y en ese caso no se toca nada.
     */
    fun onImagePicked(uri: Uri?) {
        val productId = _uiState.value.pendingPhotoProductId
        _uiState.update { it.copy(pendingPhotoProductId = null) }
        if (uri == null || productId == null) return

        // Una subida a la vez: con dos en paralelo el mensaje de una taparía al
        // de la otra y no quedaría claro cuál falló.
        if (_uiState.value.uploadingProductId != null) return

        _uiState.update { it.copy(uploadingProductId = productId) }
        viewModelScope.launch {
            val message = uploadPhoto(productId, uri)
            _uiState.update { it.copy(uploadingProductId = null, message = message) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun uploadPhoto(productId: Int, uri: Uri): String {
        val image = when (val result = imageReader.read(uri)) {
            is ProductImageReadResult.Success -> result.image
            is ProductImageReadResult.Invalid -> return result.message
        }

        val response = try {
            api.uploadProductImage(productId, image.toMultipartPart())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return "No se pudo cambiar la foto por un problema de conexión"
        }

        val body = response.body()
        if (!response.isSuccessful || body == null) {
            return "No se pudo cambiar la foto: ${ProductImageRules.uploadFailureReason(response.code())}"
        }

        // Se actualiza solo ese producto con la URL nueva en vez de recargar la
        // lista: es una sola fila y así la foto cambia al instante.
        _uiState.update { state ->
            state.copy(
                products = state.products.map { product ->
                    if (product.idProducto == productId) {
                        product.copy(imagenUrl = body.producto.imagenUrl)
                    } else {
                        product
                    }
                }
            )
        }
        return "Foto actualizada"
    }
}
