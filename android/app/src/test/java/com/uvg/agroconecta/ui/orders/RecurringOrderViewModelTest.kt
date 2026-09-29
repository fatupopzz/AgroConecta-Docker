package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.repository.RecurringOrderRepository
import com.uvg.agroconecta.notifications.RecurringReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringOrderViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<RecurringOrderRepository>()
    private val reminders = mockk<RecurringReminderScheduler>(relaxed = true)
    private val active = RecurringOrder(5, 17, RecurringFrequency.WEEKLY,
        listOf(RecurringProductDto(8, 2)), 3, "Parcela norte", Instant.parse("2030-10-01T12:00:00Z"), RecurringStatus.ACTIVE)

    @Test fun `list retains content on network failure and reconciles only success`() = runTest(main.testDispatcher) {
        coEvery { repository.list(17) } returns listOf(active) andThenThrows IllegalStateException("Sin conexión")
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.load(17)
        advanceUntilIdle()
        assertEquals(listOf(active), vm.list.value.orders)
        verify { reminders.reconcile(listOf(active), 17) }
        vm.load(17)
        advanceUntilIdle()
        assertEquals(listOf(active), vm.list.value.orders)
        assertEquals("Sin conexión", vm.list.value.error)
    }

    @Test fun `create from existing order and block double submit until server confirms`() = runTest(main.testDispatcher) {
        coEvery { repository.orderDetail(44) } returns OrderDetailDto(44, 3, "Parcela norte",
            listOf(OrderDetailProductDto(8, 2)))
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.create(any()) } coAnswers { gate.await(); active }
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.prepare("order", 44, 17, 2)
        advanceUntilIdle()
        assertEquals(8, vm.form.value.products.single().inventoryId)
        vm.setNextDate("2030-10-01 06:00")
        vm.submit()
        vm.submit()
        runCurrent()
        coVerify(exactly = 1) { repository.create(any()) }
        assertTrue(vm.submitting.value)
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.form.value.complete)
        assertEquals(listOf(active), vm.list.value.orders)
    }

    @Test fun `new recurrence uses existing cart and validates empty address`() = runTest(main.testDispatcher) {
        coEvery { repository.cart(2) } returns listOf(CartItem(1, 8, 3, 2, 5.0, 10.0,
            "Semilla", null, "Distribuidor", 20, null))
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.prepare("cart", 0, 17, 2)
        advanceUntilIdle()
        vm.submit()
        assertTrue(vm.form.value.error!!.contains("dirección"))
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test fun `new recurrence submits cart products after validation`() = runTest(main.testDispatcher) {
        coEvery { repository.cart(2) } returns listOf(CartItem(1, 8, 3, 2, 5.0, 10.0,
            "Semilla", null, "Distribuidor", 20, null))
        coEvery { repository.create(any()) } returns active
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.prepare("cart", 0, 17, 2)
        advanceUntilIdle()
        vm.setAddress("Parcela norte")
        vm.setNextDate("2030-10-01 06:00")
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.create(match {
            it.products == listOf(RecurringProductDto(8, 2)) && it.distributorId == 3 &&
                it.paymentMethod == "contra_entrega"
        }) }
        assertTrue(vm.form.value.complete)
    }

    @Test fun `edit sends only frequency and next date and keeps original on failure`() = runTest(main.testDispatcher) {
        coEvery { repository.list(17) } returns listOf(active)
        coEvery { repository.update(5, any()) } throws IllegalStateException("Servidor no disponible")
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.load(17); advanceUntilIdle()
        vm.prepare("edit", 5, 17, 2); advanceUntilIdle()
        vm.setFrequency(RecurringFrequency.MONTHLY)
        vm.setNextDate("2030-11-01 06:00")
        vm.submit(); advanceUntilIdle()
        coVerify(exactly = 1) { repository.update(5, match {
            it.frequency == "mensual" && it.nextDate != null && it.action == null
        }) }
        assertEquals(active, vm.list.value.orders.single())
        assertEquals("Servidor no disponible", vm.form.value.error)
    }

    @Test fun `pause remains unchanged until confirmed and failure keeps old order`() = runTest(main.testDispatcher) {
        coEvery { repository.list(17) } returns listOf(active)
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.update(5, any()) } coAnswers {
            gate.await()
            throw IllegalStateException("Sin conexión")
        }
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.load(17)
        advanceUntilIdle()
        vm.action(active, RecurringAction.PAUSE)
        vm.action(active, RecurringAction.PAUSE)
        runCurrent()
        assertEquals(RecurringStatus.ACTIVE, vm.list.value.orders.single().status)
        coVerify(exactly = 1) { repository.update(5, any()) }
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(RecurringStatus.ACTIVE, vm.list.value.orders.single().status)
        assertEquals("Sin conexión", vm.list.value.error)
    }

    @Test fun `confirmed pause resume and cancel update list`() = runTest(main.testDispatcher) {
        coEvery { repository.list(17) } returns listOf(active)
        coEvery { repository.update(5, any()) } coAnswers {
            val action = secondArg<UpdateRecurringOrderRequest>().action
            active.copy(status = when (action) {
                "pausar" -> RecurringStatus.PAUSED
                "reanudar" -> RecurringStatus.ACTIVE
                else -> RecurringStatus.CANCELED
            })
        }
        val vm = RecurringOrderViewModel(repository, reminders)
        vm.load(17); advanceUntilIdle()
        vm.action(active, RecurringAction.PAUSE); advanceUntilIdle()
        assertEquals(RecurringStatus.PAUSED, vm.list.value.orders.single().status)
        vm.action(vm.list.value.orders.single(), RecurringAction.RESUME); advanceUntilIdle()
        assertEquals(RecurringStatus.ACTIVE, vm.list.value.orders.single().status)
        vm.action(vm.list.value.orders.single(), RecurringAction.CANCEL); advanceUntilIdle()
        assertEquals(RecurringStatus.CANCELED, vm.list.value.orders.single().status)
        verify { reminders.reconcile(listOf(active.copy(status = RecurringStatus.CANCELED)), 17) }
    }
}
