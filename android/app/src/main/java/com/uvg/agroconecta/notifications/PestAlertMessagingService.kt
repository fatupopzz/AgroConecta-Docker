package com.uvg.agroconecta.notifications

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// AGP 8.3 todavía busca el callback heredado onNewToken. Firebase Messaging
// 25 usa onRegistered para entregar y renovar el Firebase Installation ID.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class PestAlertMessagingService : FirebaseMessagingService() {

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        PestAlertInstallationStore.save(this, installationId)
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)
        PestAlertInstallationStore.clear(this)
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
