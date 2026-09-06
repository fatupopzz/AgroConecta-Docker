package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.Order
import com.uvg.agroconecta.data.models.OrderResponse
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.cart.CartViewModel
import com.uvg.agroconecta.data.models.CartItem
import com.uvg.agroconecta.data.models.CartResponse
import com.uvg.agroconecta.ui.orders.checkout.CheckoutOrderService
import com.uvg.agroconecta.ui.profile.DistributorProfile
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: ApiService
    private lateinit var viewModel: CheckoutViewModel

    @Before
    fun setup() {
        api = mockk()
        viewModel = CheckoutViewModel(api, CheckoutOrderService(api))
    }

    @Test
    fun `formulario tiene una sola fuente de estado`() {
        viewModel.setInitialDeliveryAddress("Dirección guardada")
        viewModel.onDeliveryAddressChange("Dirección editada")
        viewModel.onDeliveryTypeChange("recogida")

        assertEquals("Dirección editada", viewModel.uiState.value.deliveryAddress)
        assertEquals("recogida", viewModel.uiState.value.deliveryType)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `crea pedido normal y publica resultado de navegacion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val request = slot<CreateOrderRequest>()
            coEvery { api.createOrder(capture(request)) } returns successfulOrder()
            viewModel.onDeliveryAddressChange("Parcela norte")

            viewModel.createCashOrder(2, listOf(cartItem()))
            advanceUntilIdle()

            assertEquals("domicilio", request.captured.tipoEntrega)
            assertEquals("Parcela norte", request.captured.direccionEntrega)
            assertEquals("Pedido creado exitosamente", viewModel.uiState.value.successMessage)
            assertEquals(11, viewModel.uiState.value.createdOrderId)
            assertFalse(viewModel.uiState.value.isCreatingOrder)
        }

    @Test
    fun `recogida usa direccion cargada del distribuidor`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val request = slot<CreateOrderRequest>()
            coEvery { api.getDistributorById(3) } returns Response.success(distributor())
            coEvery { api.createOrder(capture(request)) } returns successfulOrder()

            viewModel.loadPickupAddress(3)
            advanceUntilIdle()
            viewModel.onDeliveryTypeChange("recogida")
            viewModel.createCashOrder(2, listOf(cartItem()))
            advanceUntilIdle()

            assertEquals("recogida", request.captured.tipoEntrega)
            assertEquals("Bodega central, Guatemala", request.captured.direccionEntrega)
        }

    @Test
    fun `carrito vacio conserva mensaje y no ejecuta solicitud`() {
        viewModel.onDeliveryAddressChange("Parcela norte")

        viewModel.createCashOrder(2, emptyList())

        assertEquals("El carrito está vacío", viewModel.uiState.value.errorMessage)
        verify { api wasNot Called }
    }

    @Test
    fun `rechaza confirmaciones repetidas mientras crea el pedido`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { api.createOrder(any()) } coAnswers {
                delay(1_000)
                successfulOrder()
            }
            viewModel.onDeliveryAddressChange("Parcela norte")

            viewModel.createCashOrder(2, listOf(cartItem()))
            viewModel.createCashOrder(2, listOf(cartItem()))
            advanceUntilIdle()

            coVerify(exactly = 1) { api.createOrder(any()) }
        }

    @Test
    fun `error HTTP exige corregir datos y no ofrece reintento`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { api.createOrder(any()) } returns Response.error(
            409,
            "".toResponseBody("application/json".toMediaType())
        )
        viewModel.onDeliveryAddressChange("Parcela norte")

        viewModel.createCashOrder(2, listOf(cartItem()))
        advanceUntilIdle()

        assertEquals(
            "Revisa la dirección, los productos y las cantidades del carrito antes de confirmar (409).",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun `red timeout y servidor muestran error persistente y solo reintentan explicitamente`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val failures = listOf<Any>(IOException("sensitive details"), SocketTimeoutException(), 500)
            val expected = listOf("Se perdió la conexión", "tardó demasiado", "servidor tuvo un problema")
            failures.forEachIndexed { index, failure ->
                val service = mockk<ApiService>()
                val vm = CheckoutViewModel(service, CheckoutOrderService(service))
                coEvery { service.createOrder(any()) } coAnswers {
                    if (failure is Exception) throw failure
                    Response.error(500, "".toResponseBody())
                }
                vm.onDeliveryAddressChange("Parcela norte")
                vm.createCashOrder(2, listOf(cartItem()))
                advanceUntilIdle()
                val message = vm.uiState.value.errorMessage
                // Re-entering composition after rotation must not replace the edited address or error.
                vm.setInitialDeliveryAddress("Dirección guardada anteriormente")
                assertEquals("Parcela norte", vm.uiState.value.deliveryAddress)
                assertEquals(message, vm.uiState.value.errorMessage)
                assertTrue(message!!.contains(expected[index]))
                assertFalse(message.contains("sensitive details"))
                assertTrue(vm.uiState.value.canRetry)
                assertFalse(vm.uiState.value.canConfirm)
                vm.completeOrder({ error("No debe limpiar el carrito") }, { error("No debe navegar") })
                vm.createCashOrder(2, listOf(cartItem()))
                advanceUntilIdle()
                assertEquals(message, vm.uiState.value.errorMessage)
                coVerify(exactly = 1) { service.createOrder(any()) }
                coVerify(exactly = 0) { service.clearCart(any()) }

                coEvery { service.createOrder(any()) } coAnswers {
                    delay(1000)
                    successfulOrder()
                }
                vm.retryOrder()
                assertNull(vm.uiState.value.errorMessage)
                assertTrue(vm.uiState.value.isCreatingOrder)
                assertFalse(vm.uiState.value.canConfirm)
                assertFalse(vm.uiState.value.canRetry)
                vm.retryOrder()
                vm.createCashOrder(2, listOf(cartItem()))
                advanceUntilIdle()
                coVerify(exactly = 2) { service.createOrder(any()) }
                var clearCount = 0
                var navigationCount = 0
                repeat(2) {
                    vm.completeOrder(
                        { assertEquals(2, it); clearCount++ },
                        { assertEquals(11, it); navigationCount++ }
                    )
                }
                assertEquals(1, clearCount)
                assertEquals(1, navigationCount)
                assertNull(vm.uiState.value.errorMessage)
                vm.createCashOrder(2, listOf(cartItem()))
                vm.retryOrder()
                advanceUntilIdle()
                coVerify(exactly = 2) { service.createOrder(any()) }
            }
        }

    @Test
    fun `confirmar y reintentar no pueden enviar durante carga ni modificar el intento`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { api.createOrder(any()) } coAnswers { delay(1000); successfulOrder() }
            viewModel.onDeliveryAddressChange("Parcela norte")
            viewModel.createCashOrder(2, listOf(cartItem()))
            assertTrue(viewModel.uiState.value.isCreatingOrder)
            assertFalse(viewModel.uiState.value.canConfirm)
            assertFalse(viewModel.uiState.value.canRetry)
            viewModel.onDeliveryAddressChange("Otra dirección")
            viewModel.onDeliveryTypeChange("recogida")
            viewModel.createCashOrder(2, emptyList())
            viewModel.retryOrder()
            assertNull(viewModel.uiState.value.errorMessage)
            assertEquals("Parcela norte", viewModel.uiState.value.deliveryAddress)
            assertEquals("domicilio", viewModel.uiState.value.deliveryType)
            advanceUntilIdle()
            coVerify(exactly = 1) { api.createOrder(any()) }
        }

    @Test
    fun `reintento conserva copia estable aunque cambie la lista original`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<CreateOrderRequest>()
            coEvery { api.createOrder(capture(requests)) } throws IOException()
            val items = mutableListOf(cartItem())
            viewModel.onDeliveryAddressChange("Parcela norte")
            viewModel.createCashOrder(2, items)
            advanceUntilIdle()
            items[0] = items[0].copy(cantidad = 99)
            viewModel.retryOrder()
            advanceUntilIdle()
            assertEquals(2, requests.size)
            assertEquals(requests[0], requests[1])
            assertEquals(2, requests[1].productos.single().cantidad)
        }

    @Test
    fun `errores de validacion no son reintentables y corregir datos habilita confirmar`() =
        runTest(mainDispatcherRule.testDispatcher) {
            for (code in listOf(400, 401, 403, 404, 409, 422)) {
                val service = mockk<ApiService>()
                val vm = CheckoutViewModel(service, CheckoutOrderService(service))
                coEvery { service.createOrder(any()) } returns Response.error(code, "".toResponseBody())
                vm.onDeliveryAddressChange("Parcela norte")
                vm.createCashOrder(2, listOf(cartItem()))
                advanceUntilIdle()
                assertFalse(vm.uiState.value.canRetry)
                assertFalse(vm.uiState.value.canConfirm)
                vm.retryOrder()
                vm.completeOrder({ error("No debe limpiar") }, { error("No debe navegar") })
                coVerify(exactly = 1) { service.createOrder(any()) }
                vm.onDeliveryAddressChange("Parcela sur")
                assertNull(vm.uiState.value.errorMessage)
                assertTrue(vm.uiState.value.canConfirm)
                vm.retryOrder()
                coVerify(exactly = 1) { service.createOrder(any()) }
            }
        }

    @Test
    fun `cambiar modalidad productos o abandonar borra error e intento anterior`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { api.createOrder(any()) } throws IOException()
            val changes: List<() -> Unit> = listOf(
                { viewModel.onDeliveryTypeChange("recogida") },
                { viewModel.onCartItemsChange(listOf(cartItem().copy(cantidad = 3))) },
                { viewModel.clearError() }
            )
            changes.forEach { change ->
                viewModel.onDeliveryTypeChange("domicilio")
                viewModel.onDeliveryAddressChange("Parcela norte")
                viewModel.createCashOrder(2, listOf(cartItem()))
                advanceUntilIdle()
                viewModel.onDeliveryAddressChange("Parcela norte")
                assertTrue(viewModel.uiState.value.canRetry)
                change()
                assertNull(viewModel.uiState.value.errorMessage)
                assertFalse(viewModel.uiState.value.canRetry)
                viewModel.retryOrder()
            }
            coVerify(exactly = 3) { api.createOrder(any()) }
        }

    @Test
    fun `respuesta incompleta y cancelacion no completan ni habilitan reintento`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { api.createOrder(any()) } returns Response.success(null)
            viewModel.onDeliveryAddressChange("Parcela norte")
            viewModel.createCashOrder(2, listOf(cartItem()))
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.canRetry)
            assertTrue(viewModel.uiState.value.errorMessage!!.contains("historial"))
            viewModel.completeOrder({ error("No debe limpiar") }, { error("No debe navegar") })
            viewModel.clearError()
            coEvery { api.createOrder(any()) } throws CancellationException()
            viewModel.createCashOrder(2, listOf(cartItem()))
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isCreatingOrder)
            viewModel.completeOrder({ error("No debe limpiar") }, { error("No debe navegar") })
        }

    @Test
    fun `carrito real conserva productos tras fallo y se limpia solo una vez tras exito`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val cart = CartViewModel(api)
            coEvery { api.getCart(2) } returns Response.success(
                CartResponse(
                    idCarrito = 4, total = 14.14,
                    items = listOf(CartItem(
                        idItem = 1, idInventario = 8, idDistribuidor = 3,
                        cantidad = 2, precioUnitario = 7.07, subtotal = 14.14,
                        producto = "Fertilizante", marca = null, distribuidor = "Agroinsumos",
                        stock = 20, unidadMedida = "unidad"
                    ))
                )
            )
            coEvery { api.clearCart(2) } returns Response.success(mapOf("message" to "ok"))
            coEvery { api.createOrder(any()) } throws IOException()
            cart.loadCart(2)
            advanceUntilIdle()
            val original = cart.cartItems.value
            viewModel.onDeliveryAddressChange("Parcela norte")
            viewModel.createCashOrder(2, original)
            advanceUntilIdle()
            var navigations = 0
            viewModel.completeOrder(cart::clearCart) { navigations++ }
            advanceUntilIdle()
            assertEquals(original, cart.cartItems.value)
            assertEquals(14.14, cart.total.value, 0.001)
            coVerify(exactly = 0) { api.clearCart(any()) }
            assertEquals(0, navigations)
            coEvery { api.createOrder(any()) } returns successfulOrder()
            viewModel.retryOrder()
            advanceUntilIdle()
            repeat(2) { viewModel.completeOrder(cart::clearCart) { navigations++ } }
            advanceUntilIdle()
            assertTrue(cart.cartItems.value.isEmpty())
            assertEquals(0.0, cart.total.value, 0.001)
            coVerify(exactly = 1) { api.clearCart(2) }
            assertEquals(1, navigations)
        }

    private fun cartItem() = CartItemUI(
        id = 1,
        idInventario = 8,
        idDistribuidor = 3,
        nombre = "Fertilizante",
        distribuidor = "Agroinsumos",
        cantidad = 2,
        precio = 7.07,
        subtotal = 14.14
    )

    private fun distributor() = DistributorProfile(
        idDistribuidor = 3,
        nombreNegocio = "Agroinsumos",
        nombre = "Dueño",
        email = "distribuidor@agroconecta.gt",
        telefono = "55555555",
        departamento = "Guatemala",
        direccion = "Bodega central, Guatemala",
        nit = "1234567-8",
        estadoVerificacion = "verificado",
        calificacionPromedio = 4.5
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
