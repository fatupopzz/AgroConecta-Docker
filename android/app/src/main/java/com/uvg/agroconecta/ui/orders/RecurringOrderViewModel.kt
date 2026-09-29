package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.repository.RecurringOrderRepository
import com.uvg.agroconecta.notifications.RecurringReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class RecurringFormState(
    val source: String = "",
    val editingId: Int? = null,
    val products: List<RecurringProductDto> = emptyList(),
    val distributorId: Int = 0,
    val address: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.WEEKLY,
    val nextDate: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val complete: Boolean = false
)

data class RecurringListState(
    val orders: List<RecurringOrder> = emptyList(),
    val loading: Boolean = false,
    val pendingId: Int? = null,
    val error: String? = null
)

@HiltViewModel
class RecurringOrderViewModel @Inject constructor(
    private val repository: RecurringOrderRepository,
    private val reminders: RecurringReminderScheduler
) : ViewModel() {
    private val _list = MutableStateFlow(RecurringListState())
    val list: StateFlow<RecurringListState> = _list
    private val _form = MutableStateFlow(RecurringFormState())
    val form: StateFlow<RecurringFormState> = _form
    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting
    private var currentUserId = -1
    private var loadJob: Job? = null

    fun onSessionChanged(userId: Int?) {
        val next = userId?.takeIf { it > 0 } ?: -1
        if (currentUserId == next) return
        loadJob?.cancel()
        currentUserId = next
        _list.value = RecurringListState()
        if (next > 0) reminders.clearOtherUsers(next)
        else reminders.reconcile(emptyList(), -1)
    }

    fun load(userId: Int) {
        if (userId <= 0) return
        onSessionChanged(userId)
        if (_list.value.loading) return
        loadJob = viewModelScope.launch {
            _list.value = _list.value.copy(loading = true, error = null)
            try {
                val orders = repository.list(userId)
                if (currentUserId != userId) return@launch
                _list.value = _list.value.copy(orders = orders, error = null)
                reminders.reconcile(orders, userId)
            } catch (error: Exception) {
                if (currentUserId == userId) {
                    _list.value = _list.value.copy(error = error.message ?: "No se pudieron cargar los pedidos recurrentes.")
                }
            } finally {
                if (currentUserId == userId) _list.value = _list.value.copy(loading = false)
            }
        }
    }

    fun prepare(source: String, id: Int, userId: Int, farmerId: Int) {
        if (userId <= 0 || (source == "cart" && farmerId <= 0)) {
            _form.value = RecurringFormState(error = "Inicia sesión como agricultor para continuar.")
            return
        }
        onSessionChanged(userId)
        _form.value = RecurringFormState(source = source, loading = true)
        viewModelScope.launch {
            try {
                val products: List<RecurringProductDto>
                val distributorId: Int
                val address: String
                val existing: RecurringOrder?
                when (source) {
                    "order" -> {
                        val detail = repository.orderDetail(id)
                        products = detail.products.map { RecurringProductDto(it.inventoryId, it.quantity) }
                        distributorId = detail.distributorId
                        address = detail.deliveryAddress.orEmpty()
                        existing = null
                    }
                    "cart" -> {
                        val items = repository.cart(farmerId)
                        products = items.map { RecurringProductDto(it.idInventario, it.cantidad) }
                        distributorId = items.firstOrNull()?.idDistribuidor ?: 0
                        if (items.any { it.idDistribuidor != distributorId }) {
                            throw IllegalArgumentException("El carrito debe tener productos de un solo distribuidor.")
                        }
                        address = ""
                        existing = null
                    }
                    "edit" -> {
                        existing = _list.value.orders.firstOrNull { it.id == id }
                            ?: repository.list(userId).firstOrNull { it.id == id }
                            ?: throw IllegalArgumentException("No se encontró esta recurrencia.")
                        products = existing.products
                        distributorId = existing.distributorId
                        address = existing.deliveryAddress
                    }
                    else -> throw IllegalArgumentException("Origen de formulario inválido.")
                }
                if (products.isEmpty()) throw IllegalArgumentException("No hay productos compatibles para crear la recurrencia.")
                _form.value = RecurringFormState(
                    source = source,
                    editingId = existing?.id,
                    products = products,
                    distributorId = distributorId,
                    address = address,
                    frequency = existing?.frequency ?: RecurringFrequency.WEEKLY,
                    nextDate = RecurringDates.display(existing?.nextAt ?: Instant.now().plus(2, ChronoUnit.DAYS))
                )
            } catch (error: Exception) {
                _form.value = _form.value.copy(loading = false, error = error.message ?: "No se pudieron cargar los datos.")
            }
        }
    }

    fun setFrequency(value: RecurringFrequency) { _form.value = _form.value.copy(frequency = value, error = null) }
    fun setAddress(value: String) { _form.value = _form.value.copy(address = value, error = null) }
    fun setNextDate(value: String) { _form.value = _form.value.copy(nextDate = value, error = null) }
    fun clearFormCompletion() { _form.value = _form.value.copy(complete = false) }
    fun onNotificationPermissionGranted() {
        if (currentUserId > 0) reminders.refreshAfterPermissionGranted(_list.value.orders, currentUserId)
    }

    fun submit() {
        if (_form.value.complete) return
        if (!_submitting.compareAndSet(false, true)) return
        val form = _form.value
        val next = try { RecurringDates.parseLocal(form.nextDate) } catch (_: Exception) { null }
        val validation = when {
            form.loading -> "Espera a que terminen de cargar los datos."
            form.products.isEmpty() -> "Agrega al menos un producto."
            form.distributorId <= 0 -> "El distribuidor no es válido."
            form.products.any { it.inventoryId <= 0 || it.quantity <= 0 } -> "Revisa los productos y cantidades."
            form.editingId == null && form.address.trim().length < 5 -> "Ingresa una dirección de al menos cinco caracteres."
            next == null || !next.isAfter(Instant.now()) -> "Ingresa una fecha futura con formato aaaa-MM-dd HH:mm."
            else -> null
        }
        if (validation != null) {
            _form.value = form.copy(error = validation)
            _submitting.value = false
            return
        }
        viewModelScope.launch {
            try {
                val confirmed = if (form.editingId != null) {
                    repository.update(form.editingId, UpdateRecurringOrderRequest(
                        frequency = form.frequency.apiValue,
                        nextDate = next!!.toString()
                    ))
                } else {
                    repository.create(CreateRecurringOrderRequest(
                        frequency = form.frequency.apiValue,
                        products = form.products,
                        distributorId = form.distributorId,
                        deliveryAddress = form.address.trim(),
                        nextDate = next!!.toString()
                    ))
                }
                applyConfirmed(confirmed)
                _form.value = _form.value.copy(complete = true, error = null)
            } catch (error: Exception) {
                _form.value = _form.value.copy(error = error.message ?: "No se pudo guardar la recurrencia.")
            } finally {
                _submitting.value = false
            }
        }
    }

    fun action(order: RecurringOrder, action: RecurringAction) {
        if (_list.value.pendingId != null || order.status == RecurringStatus.CANCELED) return
        if (action == RecurringAction.PAUSE && order.status != RecurringStatus.ACTIVE) return
        if (action == RecurringAction.RESUME && order.status != RecurringStatus.PAUSED) return
        _list.value = _list.value.copy(pendingId = order.id, error = null)
        viewModelScope.launch {
            try {
                val confirmed = repository.update(order.id, UpdateRecurringOrderRequest(action = action.apiValue))
                applyConfirmed(confirmed)
            } catch (error: Exception) {
                _list.value = _list.value.copy(error = error.message ?: "No se pudo actualizar la recurrencia.")
            } finally {
                _list.value = _list.value.copy(pendingId = null)
            }
        }
    }

    private fun applyConfirmed(order: RecurringOrder) {
        if (order.userId != currentUserId) return
        val remaining = _list.value.orders.filterNot { it.id == order.id }
        _list.value = _list.value.copy(orders = listOf(order) + remaining, error = null)
        reminders.reconcile(_list.value.orders, currentUserId)
    }
}
