package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderInput
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val deliveryAddress: String = "",
    val deliveryType: String = "domicilio",
    val pickupAddress: String? = null,
    val isLoadingPickupAddress: Boolean = false,
    val isCreatingOrder: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val createdOrderId: Int? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val api: ApiService,
    private val checkoutOrderService: CheckoutOrderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var pickupAddressJob: Job? = null

    fun setInitialDeliveryAddress(address: String?) {
        if (!address.isNullOrBlank()) {
            _uiState.update { it.copy(deliveryAddress = address) }
        }
    }

    fun onDeliveryAddressChange(address: String) {
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun onDeliveryTypeChange(deliveryType: String) {
        _uiState.update { it.copy(deliveryType = deliveryType) }
    }

    fun loadPickupAddress(distributorId: Int?) {
        pickupAddressJob?.cancel()

        if (distributorId == null) {
            _uiState.update {
                it.copy(
                    pickupAddress = null,
                    isLoadingPickupAddress = false
                )
            }
            return
        }

        pickupAddressJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPickupAddress = true) }

            val address = try {
                val response = api.getDistributorById(distributorId)
                if (response.isSuccessful) response.body()?.direccion else null
            } catch (_: Exception) {
                null
            }

            ensureActive()
            _uiState.update {
                it.copy(
                    pickupAddress = address,
                    isLoadingPickupAddress = false
                )
            }
        }
    }

    fun createCashOrder(idAgricultor: Int, items: List<CartItemUI>) {
        val state = _uiState.value
        val input = CheckoutOrderInput(
            idAgricultor = idAgricultor,
            items = items,
            direccionEntrega = if (state.deliveryType == "recogida") {
                state.pickupAddress.orEmpty()
            } else {
                state.deliveryAddress
            },
            tipoEntrega = state.deliveryType
        )
        val validationError = checkoutOrderService.validationError(input)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        if (!markOrderCreationStarted()) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(errorMessage = null) }
                val response = checkoutOrderService.createOrder(input)

                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            createdOrderId = response.body()?.pedido?.id,
                            successMessage = "Pedido creado exitosamente"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "No se pudo crear el pedido (${response.code()})"
                        )
                    }
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Error inesperado")
                }
            } finally {
                _uiState.update { it.copy(isCreatingOrder = false) }
            }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearCreatedOrderId() {
        _uiState.update { it.copy(createdOrderId = null) }
    }

    private fun markOrderCreationStarted(): Boolean {
        while (true) {
            val current = _uiState.value
            if (current.isCreatingOrder) return false

            if (_uiState.compareAndSet(
                    current,
                    current.copy(isCreatingOrder = true)
                )
            ) {
                return true
            }
        }
    }
}
