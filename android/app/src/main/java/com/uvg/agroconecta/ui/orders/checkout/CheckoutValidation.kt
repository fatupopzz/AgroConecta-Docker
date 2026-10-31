package com.uvg.agroconecta.ui.orders.checkout

import com.uvg.agroconecta.ui.cart.CartItemUI

object CheckoutValidation {
    /**
     * Cada pedido sale de un solo distribuidor. El carrito lo avisa antes de
     * llegar a confirmar, para que el rechazo no llegue al final del proceso.
     */
    fun hasMultipleDistributors(items: List<CartItemUI>): Boolean =
        items.map { it.idDistribuidor }.distinct().size > 1

    fun isDeliveryAddressTooShort(address: String): Boolean =
        address.isNotBlank() && address.length < 5

    fun errorFor(input: CheckoutOrderInput): String? {
        if (input.items.isEmpty()) {
            return "El carrito está vacío"
        }

        if (input.tipoEntrega !in listOf("domicilio", "recogida")) {
            return "Selecciona un tipo de entrega válido"
        }

        if (input.tipoEntrega == "domicilio" && input.direccionEntrega.isBlank()) {
            return "La dirección de entrega es obligatoria"
        }

        if (input.esUrgente && input.tipoPlaga.isNullOrBlank()) {
            return "Selecciona el tipo de plaga"
        }

        val distributorId = input.items.first().idDistribuidor
        if (input.items.any { it.idDistribuidor != distributorId }) {
            return "Todos los productos deben ser del mismo distribuidor"
        }

        return null
    }

    fun canConfirm(
        hasItems: Boolean,
        deliveryAddress: String,
        pickupAddress: String?,
        isLoadingPickupAddress: Boolean,
        deliveryType: String
    ): Boolean = hasItems && when (deliveryType) {
        "domicilio" -> deliveryAddress.length >= 5
        "recogida" -> !isLoadingPickupAddress && !pickupAddress.isNullOrBlank()
        else -> false
    }
}
