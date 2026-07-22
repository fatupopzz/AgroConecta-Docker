package com.uvg.agroconecta.ui.cart

import com.uvg.agroconecta.MainDispatcherRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        viewModel = CartViewModel()
    }

    @Test
    fun `estado inicial del carrito es lista vacia`() {
        assertTrue(viewModel.cartItems.value.isEmpty())
    }

    @Test
    fun `total inicial es cero`() {
        assertEquals(0.0, viewModel.total.value, 0.01)
    }

    @Test
    fun `error message inicial es null`() {
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `increaseQuantity con item inexistente no cambia estado`() {
        val itemsAntes = viewModel.cartItems.value.size
        viewModel.increaseQuantity(999)
        assertEquals(itemsAntes, viewModel.cartItems.value.size)
    }

    @Test
    fun `decreaseQuantity con item inexistente no cambia estado`() {
        val itemsAntes = viewModel.cartItems.value.size
        viewModel.decreaseQuantity(999)
        assertEquals(itemsAntes, viewModel.cartItems.value.size)
    }

    @Test
    fun `removeItem con item inexistente no cambia estado`() {
        val totalAntes = viewModel.total.value
        viewModel.removeItem(999)
        assertEquals(totalAntes, viewModel.total.value, 0.01)
    }
}