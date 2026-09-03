package com.uvg.agroconecta.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.AdviceMessage
import com.uvg.agroconecta.data.models.SendAdviceMessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdviceViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {
    private val _messages = MutableStateFlow<List<AdviceMessage>>(emptyList())
    val messages: StateFlow<List<AdviceMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var pollingJob: Job? = null

    fun startPolling(orderId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var firstLoad = true
            while (isActive) {
                loadMessages(orderId, showLoading = firstLoad)
                firstLoad = false
                delay(5_000)
            }
        }
    }

    fun retry(orderId: Int) {
        viewModelScope.launch { loadMessages(orderId, showLoading = true) }
    }

    fun sendMessage(orderId: Int, text: String) {
        val message = text.trim()
        if (message.isEmpty() || message.length > 1000 || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            try {
                val response = api.sendAdviceMessage(
                    orderId,
                    SendAdviceMessageRequest(message)
                )
                if (response.isSuccessful) {
                    response.body()?.let { sent ->
                        if (_messages.value.none { it.id == sent.id }) {
                            _messages.value = _messages.value + sent
                        }
                    }
                } else {
                    _errorMessage.value = response.toUserMessage("No se pudo enviar el mensaje")
                }
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Error al enviar el mensaje"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun loadMessages(orderId: Int, showLoading: Boolean) {
        if (showLoading) _isLoading.value = true
        try {
            val response = api.getAdviceMessages(orderId)
            if (response.isSuccessful) {
                _messages.value = response.body()?.mensajes.orEmpty()
                _errorMessage.value = null
            } else if (_messages.value.isEmpty()) {
                _errorMessage.value = response.toUserMessage("No se pudo cargar la asesoría")
            }
        } catch (error: Exception) {
            if (_messages.value.isEmpty()) {
                _errorMessage.value = error.message ?: "Error al cargar la asesoría"
            }
        } finally {
            if (showLoading) _isLoading.value = false
        }
    }

    private fun retrofit2.Response<*>.toUserMessage(defaultMessage: String): String =
        when (code()) {
            403 -> "No tienes permiso para participar en esta asesoría"
            404 -> "El pedido no existe"
            else -> "$defaultMessage (${code()})"
        }
}
