package com.uvg.agroconecta.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.models.DistributorNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal const val URGENT_ORDER_NOTIFICATION_TYPE = "pedido_urgente"

internal fun latestUnreadUrgentNotification(
    notifications: List<DistributorNotification>
): DistributorNotification? = notifications.firstOrNull { notification ->
    notification.tipo == URGENT_ORDER_NOTIFICATION_TYPE && !notification.leida
}

class DistributorNotificationViewModel : ViewModel() {

    private val _urgentNotification = MutableStateFlow<DistributorNotification?>(null)
    val urgentNotification: StateFlow<DistributorNotification?> = _urgentNotification

    fun loadUrgentNotification(token: String) {
        viewModelScope.launch {
            runCatching {
                RetrofitClient.getService(token).getDistributorNotifications()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    _urgentNotification.value = latestUnreadUrgentNotification(
                        response.body().orEmpty()
                    )
                }
            }
        }
    }

    fun markAsRead(notification: DistributorNotification, token: String) {
        viewModelScope.launch {
            runCatching {
                RetrofitClient.getService(token)
                    .markDistributorNotificationAsRead(notification.id)
            }.onSuccess { response ->
                if (response.isSuccessful && _urgentNotification.value?.id == notification.id) {
                    _urgentNotification.value = null
                }
            }
        }
    }
}
