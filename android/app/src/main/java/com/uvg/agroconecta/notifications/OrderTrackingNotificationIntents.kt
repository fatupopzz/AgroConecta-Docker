package com.uvg.agroconecta.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.uvg.agroconecta.MainActivity

object OrderTrackingNotificationIntents {
    const val EXTRA_ORDER_ID = "orderId"
    const val ACTION_VIEW_ORDER_TRACKING = "com.uvg.agroconecta.VIEW_ORDER_TRACKING"

    fun createPendingIntent(context: Context, orderId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_ORDER_TRACKING
            data = "agroconecta://orders/$orderId".toUri()
            putExtra(EXTRA_ORDER_ID, orderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            context,
            orderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
