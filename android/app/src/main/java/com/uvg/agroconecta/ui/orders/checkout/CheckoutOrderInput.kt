package com.uvg.agroconecta.ui.orders.checkout

import com.uvg.agroconecta.ui.cart.CartItemUI

data class CheckoutOrderInput(
    val idAgricultor: Int,
    val items: List<CartItemUI>,
    val direccionEntrega: String,
    val tipoEntrega: String,
    val esUrgente: Boolean = false,
    val tipoPlaga: String? = null
)
