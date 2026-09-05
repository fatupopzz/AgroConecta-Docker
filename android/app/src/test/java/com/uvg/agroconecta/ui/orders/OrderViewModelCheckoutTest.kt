package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.Order
import com.uvg.agroconecta.data.models.OrderResponse
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderService
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelCheckoutTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: ApiService
    private lateinit var viewModel: OrderViewModel

    @Before
    fun setup() {
        api = mockk()
        viewModel = OrderViewModel(api, CheckoutOrderService(api))
    }

    @Test
    fun `crea solicitud normal con el contrato existente`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val request = slot<CreateOrderRequest>()
            coEvery { api.createOrder(capture(request)) } returns successfulOrder()

            viewModel.createCashOrder(
                idAgricultor = 2,
                items = listOf(cartItem()),
                direccionEntrega = "Parcela norte",
                tipoEntrega = "domicilio"
            )
            advanceUntilIdle()

            assertEquals(2, request.captured.idAgricultor)
            assertEquals(3, request.captured.idDistribuidor)
            assertEquals("Parcela norte", request.captured.direccionEntrega)
            assertEquals("domicilio", request.captured.tipoEntrega)
            assertEquals("efectivo", request.captured.metodoPago)
            assertFalse(request.captured.esUrgente)
            assertNull(request.captured.tipoPlaga)
            assertEquals(listOf(8), request.captured.productos.map { it.idInventario })
            assertEquals(listOf(2), request.captured.productos.map { it.cantidad })
            assertEquals("Pedido creado exitosamente", viewModel.successMessage.value)
            assertEquals(11, viewModel.createdOrderId.value)
            assertFalse(viewModel.isLoading.value)
        }

    @Test
    fun `pedido urgente recorta plaga y conserva mensaje diferenciado`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val request = slot<CreateOrderRequest>()
            coEvery { api.createOrder(capture(request)) } returns successfulOrder()

            viewModel.createCashOrder(
                idAgricultor = 2,
                items = listOf(cartItem()),
                direccionEntrega = "Parcela norte",
                tipoEntrega = "domicilio",
                esUrgente = true,
                tipoPlaga = "  Pulgón  "
            )
            advanceUntilIdle()

            assertEquals("Pulgón", request.captured.tipoPlaga)
            assertEquals("Pedido urgente enviado al distribuidor", viewModel.successMessage.value)
        }

    @Test
    fun `validaciones conservan mensajes y evitan la llamada a API`() {
        val cases = listOf(
            CheckoutCase(emptyList(), "Parcela norte", "domicilio", false, null, "El carrito está vacío"),
            CheckoutCase(listOf(cartItem()), "Parcela norte", "envio", false, null, "Selecciona un tipo de entrega válido"),
            CheckoutCase(listOf(cartItem()), "", "domicilio", false, null, "La dirección de entrega es obligatoria"),
            CheckoutCase(listOf(cartItem()), "Parcela norte", "domicilio", true, " ", "Selecciona el tipo de plaga"),
            CheckoutCase(
                listOf(cartItem(), cartItem(id = 2, inventoryId = 9, distributorId = 4)),
                "Parcela norte",
                "domicilio",
                false,
                null,
                "Todos los productos deben ser del mismo distribuidor"
            )
        )

        cases.forEach { case ->
            viewModel.createCashOrder(
                idAgricultor = 2,
                items = case.items,
                direccionEntrega = case.address,
                tipoEntrega = case.deliveryType,
                esUrgente = case.urgent,
                tipoPlaga = case.pestType
            )

            assertEquals(case.expectedMessage, viewModel.errorMessage.value)
        }

        verify { api wasNot Called }
    }

    @Test
    fun `error HTTP conserva codigo visible`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { api.createOrder(any()) } returns Response.error(
            409,
            "".toResponseBody("application/json".toMediaType())
        )

        viewModel.createCashOrder(
            idAgricultor = 2,
            items = listOf(cartItem()),
            direccionEntrega = "Parcela norte",
            tipoEntrega = "domicilio"
        )
        advanceUntilIdle()

        assertEquals("No se pudo crear el pedido (409)", viewModel.errorMessage.value)
        coVerify(exactly = 1) { api.createOrder(any()) }
    }

    private data class CheckoutCase(
        val items: List<CartItemUI>,
        val address: String,
        val deliveryType: String,
        val urgent: Boolean,
        val pestType: String?,
        val expectedMessage: String
    )

    private fun cartItem(
        id: Int = 1,
        inventoryId: Int = 8,
        distributorId: Int = 3
    ) = CartItemUI(
        id = id,
        idInventario = inventoryId,
        idDistribuidor = distributorId,
        nombre = "Fertilizante",
        distribuidor = "Agroinsumos",
        cantidad = 2,
        precio = 7.07,
        subtotal = 14.14
    )

    private fun successfulOrder() = Response.success(
        OrderResponse(
            message = "Pedido creado correctamente",
            pedido = Order(
                id = 11,
                fecha = "2026-09-04T10:00:00Z",
                estado = "confirmado",
                tipoEntrega = "domicilio",
                direccionEntrega = "Parcela norte",
                total = 14.14,
                agricultorNombre = null,
                distribuidorNombre = null,
                metodoPago = "contra_entrega"
            )
        )
    )
}
