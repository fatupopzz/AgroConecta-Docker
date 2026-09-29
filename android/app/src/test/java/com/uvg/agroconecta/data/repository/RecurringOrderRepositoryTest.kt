package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class RecurringOrderRepositoryTest {
    private val api = mockk<ApiService>()
    private val repository = RecurringOrderRepository(api)
    private val dto = RecurringOrderDto(5, 17, "semanal", listOf(RecurringProductDto(8, 2)),
        3, "Parcela norte", "contra_entrega", "2030-10-01T12:00:00Z", "activo")

    @Test fun `list keeps only authenticated user and maps DTO`() = runTest {
        coEvery { api.getRecurringOrders() } returns Response.success(listOf(dto, dto.copy(id = 6, userId = 99)))
        val orders = repository.list(17)
        assertEquals(1, orders.size)
        assertEquals(RecurringFrequency.WEEKLY, orders.single().frequency)
    }

    @Test fun `create and update send KAN-89 payloads`() = runTest {
        coEvery { api.createRecurringOrder(any()) } returns Response.success(RecurringOrderResponse("Creado", dto))
        coEvery { api.updateRecurringOrder(5, any()) } returns Response.success(RecurringOrderResponse("Actualizado", dto.copy(status = "pausado")))
        val request = CreateRecurringOrderRequest("semanal", dto.products, 3, "Parcela norte",
            nextDate = dto.nextDate)
        repository.create(request)
        val updated = repository.update(5, UpdateRecurringOrderRequest(action = "pausar"))
        coVerify(exactly = 1) { api.createRecurringOrder(request) }
        coVerify(exactly = 1) { api.updateRecurringOrder(5, UpdateRecurringOrderRequest(action = "pausar")) }
        assertEquals(RecurringStatus.PAUSED, updated.status)
    }
}
