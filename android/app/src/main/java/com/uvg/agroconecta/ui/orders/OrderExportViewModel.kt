package com.uvg.agroconecta.ui.orders

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.repository.OrderPdfRepository
import com.uvg.agroconecta.data.repository.OrderPdfException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed interface OrderExportState {
    data object Idle : OrderExportState
    data object Downloading : OrderExportState
    data class Completed(val uri: Uri) : OrderExportState
    data class Error(val message: String) : OrderExportState
}

@HiltViewModel
class OrderExportViewModel @Inject constructor(
    private val repository: OrderPdfRepository
) : ViewModel() {
    private val _state = MutableStateFlow<OrderExportState>(OrderExportState.Idle)
    val state: StateFlow<OrderExportState> = _state

    fun export() {
        val current = _state.value
        if (current is OrderExportState.Downloading ||
            !_state.compareAndSet(current, OrderExportState.Downloading)) return
        viewModelScope.launch {
            try {
                _state.value = OrderExportState.Completed(repository.download())
            } catch (error: CancellationException) {
                _state.value = OrderExportState.Idle
                throw error
            } catch (error: Exception) {
                val message = when (error) {
                    is OrderPdfException -> error.message ?: "No se pudo descargar el PDF."
                    is IOException -> "No se pudo descargar el PDF. Revisa tu conexión y vuelve a intentarlo."
                    else -> "No se pudo guardar el PDF. Comprueba el espacio disponible e inténtalo de nuevo."
                }
                _state.value = OrderExportState.Error(message)
            }
        }
    }

    fun clearResult() {
        if (_state.value !is OrderExportState.Downloading) _state.value = OrderExportState.Idle
    }
}
