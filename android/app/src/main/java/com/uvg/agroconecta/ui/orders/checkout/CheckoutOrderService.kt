package com.uvg.agroconecta.ui.orders.checkout

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.OrderProduct
import com.uvg.agroconecta.data.models.OrderResponse
import retrofit2.Response
import javax.inject.Inject

class CheckoutOrderService @Inject constructor(
    private val api: ApiService
) {
    fun validationError(input: CheckoutOrderInput): String? =
        CheckoutValidation.errorFor(input)

    fun prepareRequest(input: CheckoutOrderInput): CreateOrderRequest {
        val distributorId = input.items.first().idDistribuidor

        return CreateOrderRequest(
            idAgricultor = input.idAgricultor,
            idDistribuidor = distributorId,
            direccionEntrega = input.direccionEntrega,
            tipoEntrega = input.tipoEntrega,
            metodoPago = "efectivo",
            esUrgente = input.esUrgente,
            tipoPlaga = input.tipoPlaga?.trim(),
            productos = input.items.map {
                OrderProduct(
                    idInventario = it.idInventario,
                    cantidad = it.cantidad
                )
            }
        )
    }

    suspend fun createOrder(input: CheckoutOrderInput): Response<OrderResponse> =
        api.createOrder(prepareRequest(input))
}
