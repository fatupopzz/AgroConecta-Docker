package com.uvg.agroconecta.ui.connectivity

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConnectivityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val status = MutableStateFlow(true)

    // Mock estricto: si el ViewModel empezara a pedir algo más que el flujo de
    // estado (por ejemplo isOnline() en un bucle), el test lo delata.
    private val monitor = mockk<ConnectivityMonitor> {
        every { status } returns this@ConnectivityViewModelTest.status
    }

    @Test
    fun `starts online so the banner does not flash on launch`() {
        val viewModel = ConnectivityViewModel(monitor)

        assertFalse(viewModel.isOffline.value)
    }

    @Test
    fun `follows the monitor while the banner is observed`() = runTest {
        val viewModel = ConnectivityViewModel(monitor)
        val collector = launch(mainDispatcherRule.testDispatcher) { viewModel.isOffline.collect {} }

        status.value = false
        assertTrue(viewModel.isOffline.value)

        status.value = true
        assertFalse(viewModel.isOffline.value)

        collector.cancel()
    }
}
