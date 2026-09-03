package com.uvg.agroconecta.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.DistributorNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val URGENT_ORDER_NOTIFICATION_TYPE = "pedido_urgente"

internal fun latestUnreadUrgentNotification(
    notifications: List<DistributorNotification>
): DistributorNotification? = notifications.firstOrNull { notification ->
    notification.tipo == URGENT_ORDER_NOTIFICATION_TYPE && !notification.leida
}

@HiltViewModel
class DistributorNotificationViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _urgentNotification = MutableStateFlow<DistributorNotification?>(null)
    val urgentNotification: StateFlow<DistributorNotification?> = _urgentNotification

    fun loadUrgentNotification() {
        viewModelScope.launch {
            runCatching {
                api.getDistributorNotifications()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    _urgentNotification.value = latestUnreadUrgentNotification(
                        response.body().orEmpty()
                    )
                }
            }
        }
    }

    fun markAsRead(notification: DistributorNotification) {
        viewModelScope.launch {
            runCatching {
                api.markDistributorNotificationAsRead(notification.id)
            }.onSuccess { response ->
                if (response.isSuccessful && _urgentNotification.value?.id == notification.id) {
                    _urgentNotification.value = null
                }
            }
        }
    }
}
