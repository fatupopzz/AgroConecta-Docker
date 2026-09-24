package com.uvg.agroconecta.notifications

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PestAlertInstallationStoreTest {

    @Test
    fun `stores and replaces the latest Firebase installation id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        PestAlertInstallationStore.save(context, "installation-initial")
        PestAlertInstallationStore.save(context, "installation-renewed")

        assertEquals("installation-renewed", PestAlertInstallationStore.get(context))
    }

    @Test
    fun `clears the Firebase installation id when unregistered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PestAlertInstallationStore.save(context, "installation-id")

        PestAlertInstallationStore.clear(context)

        assertNull(PestAlertInstallationStore.get(context))
    }
}
