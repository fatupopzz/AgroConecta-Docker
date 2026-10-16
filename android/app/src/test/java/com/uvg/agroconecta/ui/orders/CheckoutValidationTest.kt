package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderInput
import com.uvg.agroconecta.ui.orders.checkout.CheckoutValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutValidationTest {

    @Test
    fun `detecta carrito con productos de varios distribuidores`() {
        val deAgroCentro = CartItemUI(1, 10, 3, "Urea", "Agro Centro", 1, 50.0, 50.0)
        val otroDeAgroCentro = deAgroCentro.copy(id = 2, idInventario = 11)
        val deOtroDistribuidor = deAgroCentro.copy(id = 3, idInventario = 12, idDistribuidor = 4)

        assertFalse(CheckoutValidation.hasMultipleDistributors(listOf(deAgroCentro, otroDeAgroCentro)))
        assertTrue(CheckoutValidation.hasMultipleDistributors(listOf(deAgroCentro, deOtroDistribuidor)))
        assertFalse(CheckoutValidation.hasMultipleDistributors(emptyList()))
    }

    @Test
    fun `habilitacion conserva reglas visibles de domicilio y recogida`() {
        assertFalse(canConfirm(hasItems = false, address = "Aldea El Tablón"))
        assertFalse(canConfirm(address = "1234"))
        assertTrue(canConfirm(address = "12345"))
        assertFalse(canConfirm(deliveryType = "otro", address = "12345"))
        assertFalse(canConfirm(deliveryType = "recogida", pickupAddress = null))
        assertFalse(
            canConfirm(
                deliveryType = "recogida",
                pickupAddress = "Bodega central",
                isLoadingPickupAddress = true
            )
        )
        assertTrue(
            canConfirm(
                deliveryType = "recogida",
                pickupAddress = "Bodega central"
            )
        )
        assertFalse(CheckoutValidation.isDeliveryAddressTooShort(""))
        assertTrue(CheckoutValidation.isDeliveryAddressTooShort("1234"))
        assertFalse(CheckoutValidation.isDeliveryAddressTooShort("12345"))
    }

    @Test
    fun `validacion de solicitud conserva orden y mensajes`() {
        assertEquals("El carrito está vacío", validationError(items = emptyList()))
        assertEquals(
            "Selecciona un tipo de entrega válido",
            validationError(deliveryType = "otro")
        )
        assertEquals(
            "La dirección de entrega es obligatoria",
            validationError(address = " ")
        )
        assertEquals(
            "Selecciona el tipo de plaga",
            validationError(urgent = true, pestType = " ")
        )
        assertEquals(
            "Todos los productos deben ser del mismo distribuidor",
            validationError(items = listOf(cartItem(3), cartItem(4)))
        )
        assertNull(validationError())
    }

    private fun canConfirm(
        hasItems: Boolean = true,
        address: String = "",
        pickupAddress: String? = null,
        isLoadingPickupAddress: Boolean = false,
        deliveryType: String = "domicilio"
    ) = CheckoutValidation.canConfirm(
        hasItems = hasItems,
        deliveryAddress = address,
        pickupAddress = pickupAddress,
        isLoadingPickupAddress = isLoadingPickupAddress,
        deliveryType = deliveryType
    )

    private fun validationError(
        items: List<CartItemUI> = listOf(cartItem(3)),
        address: String = "Parcela norte",
        deliveryType: String = "domicilio",
        urgent: Boolean = false,
        pestType: String? = null
    ) = CheckoutValidation.errorFor(
        CheckoutOrderInput(
            idAgricultor = 2,
            items = items,
            direccionEntrega = address,
            tipoEntrega = deliveryType,
            esUrgente = urgent,
            tipoPlaga = pestType
        )
    )

    private fun cartItem(distributorId: Int) = CartItemUI(
        id = distributorId,
        idInventario = distributorId,
        idDistribuidor = distributorId,
        nombre = "Producto",
        distribuidor = "Distribuidor",
        cantidad = 1,
        precio = 10.0,
        subtotal = 10.0
    )
}
