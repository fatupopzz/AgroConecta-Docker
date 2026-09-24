package com.uvg.agroconecta.notifications

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PestAlertNotificationIntentsTest {

    @Test
    fun `notification pending intent opens the selected pest alert`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val pendingIntent = PestAlertNotificationIntents.createPendingIntent(context, 29)
        val intent = shadowOf(pendingIntent).savedIntent

        assertEquals(PestAlertNotificationIntents.ACTION_VIEW_PEST_ALERT, intent.action)
        assertEquals("agroconecta://pest-alerts/29", intent.data.toString())
        assertEquals(29, PestAlertNotificationIntents.extractAlertId(intent))
    }

    @Test
    fun `extracts alert id from deep link and ignores unrelated intents`() {
        val deepLink = Intent().apply {
            data = "agroconecta://pest-alerts/41".toUri()
        }

        assertEquals(41, PestAlertNotificationIntents.extractAlertId(deepLink))
        assertNull(PestAlertNotificationIntents.extractAlertId(Intent(Intent.ACTION_MAIN)))
    }
}
