package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.OrderProduct
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.ui.cart.CartItemUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _orders = MutableStateFlow<List<OrderSummary>>(emptyList())
    val orders: StateFlow<List<OrderSummary>> = _orders

    fun createCashOrder(
        idAgricultor: Int,
        items: List<CartItemUI>,
        direccionEntrega: String,
        token: String
    ) {
        if (items.isEmpty()) {
            _errorMessage.value = "El carrito está vacío"
            return
        }

        if (direccionEntrega.isBlank()) {
            _errorMessage.value = "La dirección de entrega es obligatoria"
            return
        }

        val idDistribuidor = items.first().idDistribuidor
        val hasSingleDistributor = items.all { it.idDistribuidor == idDistribuidor }

        if (!hasSingleDistributor) {
            _errorMessage.value = "Todos los productos deben ser del mismo distribuidor"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val request = CreateOrderRequest(
                    idAgricultor = idAgricultor,
                    idDistribuidor = idDistribuidor,
                    direccionEntrega = direccionEntrega,
                    metodoPago = "efectivo",
                    productos = items.map {
                        OrderProduct(
                            idInventario = it.idInventario,
                            cantidad = it.cantidad
                        )
                    }
                )

                val response = RetrofitClient.getService(token).createOrder(request)

                if (response.isSuccessful) {
                    _successMessage.value = "Pedido creado exitosamente"
                } else {
                    _errorMessage.value = "No se pudo crear el pedido (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error inesperado"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOrdersByFarmer(idAgricultor: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = RetrofitClient.getService().getOrdersByFarmer(idAgricultor)

                if (response.isSuccessful) {
                    _orders.value = response.body()?.data ?: emptyList()
                } else {
                    _errorMessage.value = "No se pudo cargar el historial de pedidos"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error inesperado al cargar pedidos"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}