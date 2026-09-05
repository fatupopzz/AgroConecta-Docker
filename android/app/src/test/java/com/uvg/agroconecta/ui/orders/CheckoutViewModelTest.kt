package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.Order
import com.uvg.agroconecta.data.models.OrderResponse
import com.uvg.agroconecta.ui.cart.CartItemUI
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
import org.junit.Assert.assertNull
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
    fun `error HTTP conserva codigo visible`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { api.createOrder(any()) } returns Response.error(
            409,
            "".toResponseBody("application/json".toMediaType())
        )
        viewModel.onDeliveryAddressChange("Parcela norte")

        viewModel.createCashOrder(2, listOf(cartItem()))
        advanceUntilIdle()

        assertEquals(
            "No se pudo crear el pedido (409)",
            viewModel.uiState.value.errorMessage
        )
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
