package com.uvg.agroconecta.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.uvg.agroconecta.MainActivity

object PestAlertNotificationIntents {
    const val EXTRA_ALERT_ID = "pestAlertId"
    const val ACTION_VIEW_PEST_ALERT = "com.uvg.agroconecta.VIEW_PEST_ALERT"

    fun createPendingIntent(context: Context, alertId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_PEST_ALERT
            data = "agroconecta://pest-alerts/$alertId".toUri()
            putExtra(EXTRA_ALERT_ID, alertId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            alertId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun extractAlertId(intent: Intent): Int? {
        val isPestAlertIntent = intent.action == ACTION_VIEW_PEST_ALERT ||
            intent.data?.host == "pest-alerts"
        if (!isPestAlertIntent) return null

        return intent.getIntExtra(EXTRA_ALERT_ID, -1)
            .takeIf { it > 0 }
            ?: intent.data?.lastPathSegment?.toIntOrNull()?.takeIf { it > 0 }
    }
}
