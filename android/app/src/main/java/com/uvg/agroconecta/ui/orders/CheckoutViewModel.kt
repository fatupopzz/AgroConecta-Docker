package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderInput
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderService
import com.uvg.agroconecta.ui.orders.checkout.CheckoutError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    val error: CheckoutError? = null,
    val createdOrderId: Int? = null,
    val completedFarmerId: Int? = null
) {
    val errorMessage: String? get() = error?.message
    val canConfirm: Boolean get() = !isCreatingOrder && completedFarmerId == null && error == null
    val canRetry: Boolean get() = !isCreatingOrder && completedFarmerId == null && error?.retryable == true
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val api: ApiService,
    private val checkoutOrderService: CheckoutOrderService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var pickupAddressJob: Job? = null
    private var failedInput: CheckoutOrderInput? = null

    fun setInitialDeliveryAddress(address: String?) {
        if (!address.isNullOrBlank() && _uiState.value.deliveryAddress.isBlank()) {
            onDeliveryAddressChange(address)
        }
    }

    fun onDeliveryAddressChange(address: String) {
        if (_uiState.value.isCreatingOrder || _uiState.value.deliveryAddress == address) return
        clearError()
        _uiState.update { it.copy(deliveryAddress = address) }
    }

    fun onDeliveryTypeChange(deliveryType: String) {
        if (_uiState.value.isCreatingOrder || _uiState.value.deliveryType == deliveryType) return
        clearError()
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
            if (_uiState.value.deliveryType == "recogida" && _uiState.value.pickupAddress != address) {
                clearError()
            }
            _uiState.update {
                it.copy(
                    pickupAddress = address,
                    isLoadingPickupAddress = false
                )
            }
        }
    }

    fun onCartItemsChange(items: List<CartItemUI>) {
        if (failedInput?.items != null && failedInput?.items != items) clearError()
    }

    fun clearError() {
        if (_uiState.value.isCreatingOrder) return
        failedInput = null
        _uiState.update { it.copy(error = null) }
    }

    fun createCashOrder(idAgricultor: Int, items: List<CartItemUI>) {
        val state = _uiState.value
        if (!state.canConfirm) return
        val input = CheckoutOrderInput(
            idAgricultor = idAgricultor,
            // CartItemUI has only immutable values; copy the list to detach it from the caller.
            items = items.toList(),
            direccionEntrega = if (state.deliveryType == "recogida") {
                state.pickupAddress.orEmpty()
            } else {
                state.deliveryAddress
            },
            tipoEntrega = state.deliveryType
        )
        val validationError = checkoutOrderService.validationError(input)
        if (validationError != null) {
            failedInput = input
            _uiState.update { it.copy(error = CheckoutError(validationError)) }
            return
        }
        submitOrder(input, retry = false)
    }

    fun retryOrder() {
        val input = failedInput ?: return
        submitOrder(input, retry = true)
    }

    private fun submitOrder(input: CheckoutOrderInput, retry: Boolean) {
        if (!markOrderCreationStarted(retry)) return
        failedInput = null
        viewModelScope.launch {
            try {
                val response = checkoutOrderService.createOrder(input)
                val orderId = response.body()?.pedido?.id
                if (response.isSuccessful && orderId != null && orderId > 0) {
                    _uiState.update {
                        it.copy(
                            createdOrderId = orderId,
                            completedFarmerId = input.idAgricultor,
                            successMessage = "Pedido creado exitosamente"
                        )
                    }
                } else {
                    failedInput = input
                    val error = if (response.isSuccessful) {
                        CheckoutError("No pudimos confirmar la respuesta del pedido. Revisa tu historial antes de volver a confirmar.")
                    } else {
                        CheckoutError.fromHttp(response.code())
                    }
                    _uiState.update { it.copy(error = error) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                failedInput = input
                _uiState.update { it.copy(error = CheckoutError.fromException(exception)) }
            } finally {
                _uiState.update { it.copy(isCreatingOrder = false) }
            }
        }
    }

    // Consume success atomically, without suspending between claiming it and its UI effects.
    // completedFarmerId remains set so stale callbacks cannot submit again after success.
    fun completeOrder(clearCart: (Int) -> Unit, navigate: (Int) -> Unit) {
        while (true) {
            val current = _uiState.value
            val farmerId = current.completedFarmerId ?: return
            val orderId = current.createdOrderId ?: return
            if (_uiState.compareAndSet(current, current.copy(createdOrderId = null, successMessage = null))) {
                clearCart(farmerId)
                navigate(orderId)
                return
            }
        }
    }

    private fun markOrderCreationStarted(retry: Boolean): Boolean {
        while (true) {
            val current = _uiState.value
            if (if (retry) !current.canRetry else !current.canConfirm) return false
            if (_uiState.compareAndSet(current, current.copy(isCreatingOrder = true, error = null))) {
                return true
            }
        }
    }
}
