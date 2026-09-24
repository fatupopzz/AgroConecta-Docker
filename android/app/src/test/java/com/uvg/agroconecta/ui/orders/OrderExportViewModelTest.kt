package com.uvg.agroconecta.ui.orders

import android.net.Uri
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.repository.OrderPdfRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OrderExportViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun `dos pulsaciones inician una sola descarga y terminan con URI`() =
        runTest(dispatcher.testDispatcher) {
            val repository = mockk<OrderPdfRepository>()
            val uri = Uri.parse("content://downloads/my_downloads/1")
            val gate = CompletableDeferred<Uri>()
            coEvery { repository.download() } coAnswers { gate.await() }
            val viewModel = OrderExportViewModel(repository)

            viewModel.export()
            viewModel.export()
            assertTrue(viewModel.state.value is OrderExportState.Downloading)
            gate.complete(uri)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.download() }
            assertEquals(OrderExportState.Completed(uri), viewModel.state.value)
        }

    @Test fun `fallo deja mensaje comprensible y permite reintentar`() =
        runTest(dispatcher.testDispatcher) {
            val repository = mockk<OrderPdfRepository>()
            coEvery { repository.download() } throws java.io.IOException("Sin conexión")
            val viewModel = OrderExportViewModel(repository)
            viewModel.export()
            advanceUntilIdle()
            assertEquals(
                OrderExportState.Error("No se pudo descargar el PDF. Revisa tu conexión y vuelve a intentarlo."),
                viewModel.state.value
            )
            viewModel.export()
            advanceUntilIdle()
            coVerify(exactly = 2) { repository.download() }
        }
}
