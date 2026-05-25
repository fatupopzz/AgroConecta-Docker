package com.uvg.agroconecta.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {

    private var currentFarmerId: Int = -1
    private var currentToken: String = ""

    private val _cartItems = MutableStateFlow<List<CartItemUI>>(emptyList())
    val cartItems: StateFlow<List<CartItemUI>> = _cartItems

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadCart(idAgricultor: Int, token: String) {
        currentFarmerId = idAgricultor
        currentToken = token

        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(token).getCart(idAgricultor)

                if (response.isSuccessful) {
                    val cart = response.body()

                    _cartItems.value = cart?.items?.map {
                        CartItemUI(
                            id = it.idItem,
                            idInventario = it.idInventario,
                            idDistribuidor = it.idDistribuidor,
                            nombre = it.producto,
                            distribuidor = it.distribuidor,
                            cantidad = it.cantidad,
                            precio = it.precioUnitario
                        )
                    } ?: emptyList()

                    _total.value = cart?.total ?: 0.0
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "No se pudo cargar el carrito (${response.code()})"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Error de conexión al cargar el carrito"
            }
        }
    }

    fun increaseQuantity(idItem: Int) {
        val item = _cartItems.value.firstOrNull { it.id == idItem } ?: return
        updateQuantity(idItem, item.cantidad + 1)
    }

    fun decreaseQuantity(idItem: Int) {
        val item = _cartItems.value.firstOrNull { it.id == idItem } ?: return

        if (item.cantidad <= 1) {
            removeItem(idItem)
        } else {
            updateQuantity(idItem, item.cantidad - 1)
        }
    }

    fun removeItem(idItem: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(currentToken)
                    .removeCartItem(currentFarmerId, idItem)

                if (response.isSuccessful) {
                    loadCart(currentFarmerId, currentToken)
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "No se pudo eliminar el producto"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Error de conexión al eliminar el producto"
            }
        }
    }

    private fun updateQuantity(idItem: Int, cantidad: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.getService(currentToken).updateCartItem(
                    idAgricultor = currentFarmerId,
                    idItem = idItem,
                    body = mapOf("cantidad" to cantidad)
                )

                if (response.isSuccessful) {
                    loadCart(currentFarmerId, currentToken)
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "No se pudo actualizar la cantidad"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Error de conexión al actualizar el carrito"
            }
        }
    }
}