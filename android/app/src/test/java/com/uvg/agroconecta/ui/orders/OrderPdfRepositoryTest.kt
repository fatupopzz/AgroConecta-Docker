package com.uvg.agroconecta.ui.orders

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.repository.OrderPdfRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class OrderPdfRepositoryTest {
    @Test fun `respuesta HTTP fallida no crea archivo`() = runBlocking {
        val api = mockk<ApiService>()
        coEvery { api.exportOrderHistoryPdf() } returns Response.error(503, "fallo".toResponseBody())
        val repository = OrderPdfRepository(api, mockk<Context>())
        val error = runCatching { repository.download() }.exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(error!!.message!!.contains("503"))
    }

    @Test fun `archivo vacío o inválido se rechaza`() = runBlocking {
        val api = mockk<ApiService>()
        coEvery { api.exportOrderHistoryPdf() } returns Response.success("".toResponseBody())
        val repository = OrderPdfRepository(api, mockk<Context>())
        val error = runCatching { repository.download() }.exceptionOrNull()
        assertTrue(error is IOException)
    }

    @Test fun `Intent abre URI de contenido con MIME y permiso temporal`() {
        val uri = Uri.parse("content://downloads/my_downloads/1")
        val intent = OrderPdfOpener.intent(uri)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals(uri, intent.data)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test fun `sin lector PDF devuelve false y conserva el archivo`() {
        val context = mockk<Context>()
        every { context.startActivity(any()) } throws ActivityNotFoundException()
        assertFalse(OrderPdfOpener.open(context, Uri.parse("content://downloads/my_downloads/1")))
    }

    @Test @Config(sdk = [28]) fun `Android anterior a Q guarda URI de contenido segura`() = runBlocking {
        val api = mockk<ApiService>()
        coEvery { api.exportOrderHistoryPdf() } returns Response.success(
            ("%PDF-" + "x".repeat(100)).toResponseBody()
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = OrderPdfRepository(api, context).download()
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.readBytes().isNotEmpty() })
    }
}
