package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.data.models.OrderTrackingResponse
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderInput
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val api: ApiService,
    private val checkoutOrderService: CheckoutOrderService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _orders = MutableStateFlow<List<OrderSummary>>(emptyList())
    val orders: StateFlow<List<OrderSummary>> = _orders

    private val _tracking = MutableStateFlow<OrderTrackingResponse?>(null)
    val tracking: StateFlow<OrderTrackingResponse?> = _tracking

    private val _createdOrderId = MutableStateFlow<Int?>(null)
    val createdOrderId: StateFlow<Int?> = _createdOrderId

    fun createCashOrder(
        idAgricultor: Int,
        items: List<CartItemUI>,
        direccionEntrega: String,
        tipoEntrega: String,
        esUrgente: Boolean = false,
        tipoPlaga: String? = null
    ) {
        val input = CheckoutOrderInput(
            idAgricultor = idAgricultor,
            items = items,
            direccionEntrega = direccionEntrega,
            tipoEntrega = tipoEntrega,
            esUrgente = esUrgente,
            tipoPlaga = tipoPlaga
        )
        val validationError = checkoutOrderService.validationError(input)
        if (validationError != null) {
            _errorMessage.value = validationError
            return
        }

        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null

                val response = checkoutOrderService.createOrder(input)

                if (response.isSuccessful) {
                    _createdOrderId.value = response.body()?.pedido?.id
                    _successMessage.value = if (esUrgente) {
                        "Pedido urgente enviado al distribuidor"
                    } else {
                        "Pedido creado exitosamente"
                    }
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

                val response = api.getOrdersByFarmer(idAgricultor)

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

    fun loadOrdersByDistributor(idDistribuidor: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = api.getOrdersByDistributor(idDistribuidor)

                if (response.isSuccessful) {
                    _orders.value = response.body().orEmpty()
                } else {
                    _errorMessage.value = "No se pudieron cargar los pedidos recibidos"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error inesperado al cargar pedidos"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOrderTracking(orderId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _tracking.value = null

                val response = api.getOrderTracking(orderId)

                if (response.isSuccessful) {
                    _tracking.value = response.body()
                } else {
                    _errorMessage.value = "No se pudo cargar el seguimiento del pedido"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error inesperado al cargar seguimiento"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearCreatedOrderId() {
        _createdOrderId.value = null
    }
}
