package com.uvg.agroconecta.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class PriceNotificationState(
    val ownerId: Int? = null,
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = false,
    val readingIds: Set<Int> = emptySet(),
    val error: String? = null
)

internal fun priceNotificationProductId(notification: AppNotification): Int? =
    notification.contenido.idProducto?.takeIf { it > 0 && notification.tipo == "baja_precio" }

internal fun priceNotificationText(notification: AppNotification): String {
    val content = notification.contenido
    return String.format(Locale.US, "Antes Q%.2f · Ahora Q%.2f · Descuento %.2f%%",
        content.precioAnterior, content.precioNuevo, content.porcentajeDescuento)
}

@HiltViewModel
class PriceNotificationViewModel @Inject constructor(private val api: ApiService) : ViewModel() {
    private val _state = MutableStateFlow(PriceNotificationState())
    val state: StateFlow<PriceNotificationState> = _state
    private var generation = 0
    private var loadJob: Job? = null
    private val readIds = mutableSetOf<Int>()

    fun load(userId: Int?, role: String?) {
        val owner = userId?.takeIf { it > 0 && role == "agricultor" }
        val version = ++generation
        loadJob?.cancel()
        if (_state.value.ownerId != owner) {
            readIds.clear()
            _state.value = PriceNotificationState(ownerId = owner)
        }
        if (owner == null) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        loadJob = viewModelScope.launch {
            try {
                val response = api.getNotifications()
                if (version != generation) return@launch
                if (!response.isSuccessful) throw IllegalStateException()
                val unread = response.body().orEmpty().filter {
                    it.tipo == "baja_precio" && !it.leida && it.id !in readIds &&
                        priceNotificationProductId(it) != null &&
                        !it.contenido.producto.isNullOrBlank() &&
                        it.contenido.precioAnterior?.isFinite() == true &&
                        it.contenido.precioNuevo?.isFinite() == true &&
                        it.contenido.porcentajeDescuento?.isFinite() == true
                }
                _state.value = _state.value.copy(notifications = unread, isLoading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (version == generation) _state.value = _state.value.copy(
                    isLoading = false, error = "No pudimos cargar tus alertas de precios. Intenta de nuevo.")
            }
        }
    }

    fun markAsRead(notification: AppNotification) {
        val owner = _state.value.ownerId ?: return
        if (notification !in _state.value.notifications || notification.id in _state.value.readingIds) return
        _state.value = _state.value.copy(readingIds = _state.value.readingIds + notification.id, error = null)
        viewModelScope.launch {
            try {
                val response = api.markNotificationAsRead(notification.id)
                if (_state.value.ownerId != owner) return@launch
                if (!response.isSuccessful) throw IllegalStateException()
                readIds.add(notification.id)
                _state.value = _state.value.copy(notifications = _state.value.notifications.filterNot { it.id == notification.id })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_state.value.ownerId == owner) _state.value = _state.value.copy(
                    error = "No pudimos marcar la alerta como leída. Intenta de nuevo.")
            } finally {
                if (_state.value.ownerId == owner) _state.value = _state.value.copy(readingIds = _state.value.readingIds - notification.id)
            }
        }
    }
}
