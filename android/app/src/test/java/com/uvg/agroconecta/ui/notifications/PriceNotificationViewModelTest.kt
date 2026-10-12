package com.uvg.agroconecta.ui.notifications

import com.google.gson.Gson
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.AppNotification
import com.uvg.agroconecta.data.models.NotificationContent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PriceNotificationViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val api = mockk<ApiService>()
    private val vm = PriceNotificationViewModel(api)
    private val alert = AppNotification(8, "baja_precio", NotificationContent(
        idProducto = 4, producto = "Maíz", precioAnterior = 100.0, precioNuevo = 75.0, porcentajeDescuento = 25.0))

    @Test fun parsesPriceJsonAndFormatsMoneyAndProductDestination() {
        val parsed = Gson().fromJson("""{"id_notificacion":8,"tipo":"baja_precio","contenido":{"id_producto":4,"producto":"Maíz","precio_anterior":100,"precio_nuevo":75,"porcentaje_descuento":25},"leida":false}""", AppNotification::class.java)
        assertEquals(alert, parsed)
        assertEquals("Antes Q100.00 · Ahora Q75.00 · Descuento 25.00%", priceNotificationText(parsed))
        assertEquals(4, priceNotificationProductId(parsed))
    }

    @Test fun onlyUnreadPriceAlertsAreVisibleForFarmer() = runTest {
        coEvery { api.getNotifications() } returns Response.success(listOf(alert, alert.copy(id = 9, leida = true), alert.copy(id = 10, tipo = "pedido_urgente")))
        vm.load(1, "agricultor"); advanceUntilIdle()
        assertEquals(listOf(alert), vm.state.value.notifications)
        vm.load(2, "distribuidor"); advanceUntilIdle()
        assertTrue(vm.state.value.notifications.isEmpty())
        coVerify(exactly = 1) { api.getNotifications() }
    }

    @Test fun markReadRemovesAlertAndStaleResponseCannotRestoreIt() = runTest {
        coEvery { api.getNotifications() } returns Response.success(listOf(alert))
        coEvery { api.markNotificationAsRead(8) } returns Response.success(mapOf("message" to "ok"))
        vm.load(1, "agricultor"); advanceUntilIdle()
        vm.markAsRead(alert); advanceUntilIdle()
        assertTrue(vm.state.value.notifications.isEmpty())
        vm.load(1, "agricultor"); advanceUntilIdle()
        assertTrue(vm.state.value.notifications.isEmpty())
        coVerify(exactly = 1) { api.markNotificationAsRead(8) }
    }

    @Test fun networkErrorKeepsExistingAlertsAndCanRetry() = runTest {
        coEvery { api.getNotifications() } returns Response.success(listOf(alert))
        vm.load(1, "agricultor"); advanceUntilIdle()
        coEvery { api.getNotifications() } throws IOException()
        vm.load(1, "agricultor"); advanceUntilIdle()
        assertEquals(listOf(alert), vm.state.value.notifications)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test fun switchingOwnerClearsOldAlertsBeforeRequestCompletes() = runTest {
        coEvery { api.getNotifications() } returns Response.success(listOf(alert))
        vm.load(1, "agricultor"); advanceUntilIdle()
        coEvery { api.getNotifications() } coAnswers { delay(100); Response.success(emptyList()) }
        vm.load(2, "agricultor")
        assertTrue(vm.state.value.notifications.isEmpty())
        advanceUntilIdle()
        assertEquals(2, vm.state.value.ownerId)
    }

    @Test fun failedMarkReadKeepsAlertForRetry() = runTest {
        coEvery { api.getNotifications() } returns Response.success(listOf(alert))
        coEvery { api.markNotificationAsRead(8) } throws IOException()
        vm.load(1, "agricultor"); advanceUntilIdle()
        vm.markAsRead(alert); advanceUntilIdle()
        assertEquals(listOf(alert), vm.state.value.notifications)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.readingIds.isEmpty())
    }
}
