package com.uvg.agroconecta.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uvg.agroconecta.R

object PestAlertNotifications {
    const val CHANNEL_ID = "pest_alerts_nearby"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.pest_alert_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.pest_alert_notification_channel_description)
            enableVibration(true)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context, payload: PestAlertPushPayload): Boolean {
        if (!context.canPostNotifications()) return false

        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_catalog)
            .setColor(ContextCompat.getColor(context, R.color.green_primary))
            .setContentTitle(payload.notificationTitle())
            .setContentText(payload.notificationBody())
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.notificationBody()))
            .setContentIntent(
                PestAlertNotificationIntents.createPendingIntent(context, payload.alertId)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(payload.alertId, notification)
        return true
    }
}

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
