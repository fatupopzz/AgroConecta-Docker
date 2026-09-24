package com.uvg.agroconecta.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PestAlertTokenStoreTest {

    @Test
    fun `stores the latest FCM registration token`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        PestAlertTokenStore.save(context, "token-initial")
        PestAlertTokenStore.save(context, "token-renewed")

        assertEquals("token-renewed", PestAlertTokenStore.get(context))
    }
}
