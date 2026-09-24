package com.uvg.agroconecta.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PestAlertMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PestAlertTokenStore.save(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PestAlertPushPayload.from(
            data = message.data,
            notificationTitle = message.notification?.title,
            notificationBody = message.notification?.body
        ) ?: return

        PestAlertNotifications.show(this, payload)
    }
}
