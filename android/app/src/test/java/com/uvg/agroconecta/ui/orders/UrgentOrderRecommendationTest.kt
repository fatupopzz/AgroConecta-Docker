package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.ui.cart.CartItemUI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrgentOrderRecommendationTest {

    private val insecticide = cartItem(1, "Insecticida de neem")
    private val fungicide = cartItem(2, "Fungicida a base de cobre")

    @Test
    fun recommendsProductUsingPestKeywords() {
        val result = recommendUrgentProduct(
            cartItems = listOf(insecticide, fungicide),
            pestType = "Hongos"
        )

        assertEquals(fungicide, result)
    }

    @Test
    fun returnsNullWhenThereIsNoKeywordMatch() {
        val result = recommendUrgentProduct(
            cartItems = listOf(insecticide, fungicide),
            pestType = "Otra plaga"
        )

        assertNull(result)
    }

    @Test
    fun returnsNullWhenKnownPestDoesNotMatchAnyProduct() {
        val result = recommendUrgentProduct(
            cartItems = listOf(fungicide),
            pestType = "Pulgón"
        )

        assertNull(result)
    }

    @Test
    fun returnsNullForEmptyCart() {
        assertNull(recommendUrgentProduct(emptyList(), "Pulgón"))
    }

    private fun cartItem(id: Int, name: String) = CartItemUI(
        id = id,
        idInventario = id,
        idDistribuidor = 5,
        nombre = name,
        distribuidor = "Agroinsumos",
        cantidad = 1,
        precio = 50.0
    )
}
