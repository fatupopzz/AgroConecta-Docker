package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.api.toAuthHeader
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.OrderProduct
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.data.models.OrderTrackingResponse
import com.uvg.agroconecta.ui.cart.CartItemUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val api: ApiService
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

    private val _pickupAddress = MutableStateFlow<String?>(null)
    val pickupAddress: StateFlow<String?> = _pickupAddress

    private val _isLoadingPickupAddress = MutableStateFlow(false)
    val isLoadingPickupAddress: StateFlow<Boolean> = _isLoadingPickupAddress

    /**
     * Direccion del distribuidor, para mostrarla cuando la entrega es por
     * recogida. Recibe el id como nullable porque la pantalla lo saca del
     * carrito y puede venir vacio.
     */
    fun loadPickupAddress(idDistribuidor: Int?, token: String?) {
        if (idDistribuidor == null) {
            _pickupAddress.value = null
            return
        }

        viewModelScope.launch {
            _isLoadingPickupAddress.value = true
            try {
                val response = api.getDistributorById(idDistribuidor, token.toAuthHeader())
                _pickupAddress.value = if (response.isSuccessful) {
                    response.body()?.direccion
                } else {
                    null
                }
            } catch (e: Exception) {
                _pickupAddress.value = null
            } finally {
                _isLoadingPickupAddress.value = false
            }
        }
    }

    fun createCashOrder(
        idAgricultor: Int,
        items: List<CartItemUI>,
        direccionEntrega: String,
        tipoEntrega: String,
        token: String,
        esUrgente: Boolean = false,
        tipoPlaga: String? = null
    ) {
        if (items.isEmpty()) {
            _errorMessage.value = "El carrito está vacío"
            return
        }

        if (tipoEntrega !in listOf("domicilio", "recogida")) {
            _errorMessage.value = "Selecciona un tipo de entrega válido"
            return
        }

        if (tipoEntrega == "domicilio" && direccionEntrega.isBlank()) {
            _errorMessage.value = "La dirección de entrega es obligatoria"
            return
        }

        if (esUrgente && tipoPlaga.isNullOrBlank()) {
            _errorMessage.value = "Selecciona el tipo de plaga"
            return
        }

        val idDistribuidor = items.first().idDistribuidor
        val hasSingleDistributor = items.all { it.idDistribuidor == idDistribuidor }

        if (!hasSingleDistributor) {
            _errorMessage.value = "Todos los productos deben ser del mismo distribuidor"
            return
        }

        if (!_isLoading.compareAndSet(expect = false, update = true)) {
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null

                val request = CreateOrderRequest(
                    idAgricultor = idAgricultor,
                    idDistribuidor = idDistribuidor,
                    direccionEntrega = direccionEntrega,
                    tipoEntrega = tipoEntrega,
                    metodoPago = "efectivo",
                    esUrgente = esUrgente,
                    tipoPlaga = tipoPlaga?.trim(),
                    productos = items.map {
                        OrderProduct(
                            idInventario = it.idInventario,
                            cantidad = it.cantidad
                        )
                    }
                )

                val response = api.createOrder(token.toAuthHeader(), request)

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

    fun loadOrdersByFarmer(idAgricultor: Int, token: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = api.getOrdersByFarmer(idAgricultor, token.toAuthHeader())

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

    fun loadOrdersByDistributor(idDistribuidor: Int, token: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = api.getOrdersByDistributor(idDistribuidor, token.toAuthHeader())

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

    fun loadOrderTracking(orderId: Int, token: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _tracking.value = null

                val response = api.getOrderTracking(orderId, token.toAuthHeader())

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
