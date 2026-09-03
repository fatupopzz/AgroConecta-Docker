package com.uvg.agroconecta.ui.cart

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Mock estricto a proposito: no lleva `relaxed = true`, asi que cualquier
     * llamada a la red que no este explicitamente stubbeada revienta el test en
     * lugar de salir al backend real.
     */
    private lateinit var api: ApiService
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        api = mockk()
        viewModel = CartViewModel(api)
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
        // Corta antes de llegar al service: el item no esta en el carrito local.
        verify { api wasNot Called }
    }

    @Test
    fun `decreaseQuantity con item inexistente no cambia estado`() {
        val itemsAntes = viewModel.cartItems.value.size
        viewModel.decreaseQuantity(999)
        assertEquals(itemsAntes, viewModel.cartItems.value.size)
        verify { api wasNot Called }
    }

    @Test
    fun `removeItem con item inexistente no cambia estado`() {
        // A diferencia de increase/decrease, removeItem si llama al backend sin
        // validar antes, asi que hay que stubbear la respuesta.
        coEvery { api.removeCartItem(any(), any()) } returns notFound()

        val totalAntes = viewModel.total.value
        viewModel.removeItem(999)

        assertEquals(totalAntes, viewModel.total.value, 0.01)
        assertEquals("No se pudo eliminar el producto", viewModel.errorMessage.value)
        // Sin loadCart previo el ViewModel todavia no tiene agricultor; el
        // token ya no viaja por aca, lo pone AuthInterceptor.
        coVerify(exactly = 1) {
            api.removeCartItem(idAgricultor = -1, idItem = 999)
        }
    }

    private fun <T> notFound(): Response<T> =
        Response.error(404, "".toResponseBody("application/json".toMediaTypeOrNull()))
}
