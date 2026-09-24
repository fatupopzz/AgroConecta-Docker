package com.uvg.agroconecta.notifications

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PestAlertNotificationsTest {

    @Test
    fun `creates high priority channel and posts pest alert`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)

        PestAlertNotifications.createChannel(context)
        val posted = PestAlertNotifications.show(
            context,
            PestAlertPushPayload(
                alertId = 29,
                pestType = "pulgon",
                crop = "Frijol",
                distanceKm = 3.4
            )
        )

        val channel = manager.getNotificationChannel(PestAlertNotifications.CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(posted)
    }
}
