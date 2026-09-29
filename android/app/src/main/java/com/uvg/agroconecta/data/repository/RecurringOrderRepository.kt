package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateRecurringOrderRequest
import com.uvg.agroconecta.data.models.CartItem
import com.uvg.agroconecta.data.models.OrderDetailDto
import com.uvg.agroconecta.data.models.RecurringOrder
import com.uvg.agroconecta.data.models.UpdateRecurringOrderRequest
import com.uvg.agroconecta.data.models.toDomain
import retrofit2.Response
import javax.inject.Inject

class RecurringOrderRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(userId: Int): List<RecurringOrder> =
        api.getRecurringOrders().requireBody().map { it.toDomain() }
            .filter { it.userId == userId }

    suspend fun orderDetail(id: Int): OrderDetailDto = api.getOrderById(id).requireBody()

    suspend fun cart(farmerId: Int): List<CartItem> = api.getCart(farmerId).requireBody().items

    suspend fun create(request: CreateRecurringOrderRequest): RecurringOrder =
        api.createRecurringOrder(request).requireBody().order.toDomain()

    suspend fun update(id: Int, request: UpdateRecurringOrderRequest): RecurringOrder =
        api.updateRecurringOrder(id, request).requireBody().order.toDomain()
}

private fun <T> Response<T>.requireBody(): T {
    if (!isSuccessful) {
        throw IllegalStateException(
            when (code()) {
                400 -> "Revisa los datos de la recurrencia."
                401, 403 -> "Tu sesión no permite gestionar estos pedidos."
                404 -> "El pedido o producto ya no está disponible."
                409 -> "Esta recurrencia ya no puede modificarse."
                else -> "No se pudo completar la solicitud (${code()})."
            }
        )
    }
    return body() ?: throw IllegalStateException("La respuesta del servidor está vacía.")
}
