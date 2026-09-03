package com.uvg.agroconecta.ui.orders

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.ui.profile.DistributorProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class OrderViewModelPickupAddressTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock estricto: lo unico stubbeado es la consulta del distribuidor, asi que
    // cualquier otra llamada a la red revienta el test en vez de pasar de largo.
    private lateinit var api: ApiService
    private lateinit var viewModel: OrderViewModel

    @Before
    fun setup() {
        api = mockk()
        viewModel = OrderViewModel(api)
    }

    @Test
    fun `carga la direccion del distribuidor`() = runTest {
        coEvery { api.getDistributorById(7) } returns
            Response.success(distributor(7, "Km 15 Carretera a El Salvador"))

        viewModel.loadPickupAddress(7)

        assertEquals("Km 15 Carretera a El Salvador", viewModel.pickupAddress.value)
        assertFalse(viewModel.isLoadingPickupAddress.value)
        coVerify(exactly = 1) { api.getDistributorById(7) }
    }

    @Test
    fun `sin distribuidor limpia la direccion y no consulta la API`() = runTest {
        coEvery { api.getDistributorById(any()) } returns
            Response.success(distributor(7, "Zona 4"))
        viewModel.loadPickupAddress(7)

        viewModel.loadPickupAddress(null)

        assertNull(viewModel.pickupAddress.value)
        assertFalse(viewModel.isLoadingPickupAddress.value)
        coVerify(exactly = 1) { api.getDistributorById(any()) }
    }

    @Test
    fun `deja la direccion en null cuando el backend responde con error`() = runTest {
        coEvery { api.getDistributorById(7) } returns notFound()

        viewModel.loadPickupAddress(7)

        assertNull(viewModel.pickupAddress.value)
        assertFalse(viewModel.isLoadingPickupAddress.value)
    }

    @Test
    fun `una consulta nueva cancela la anterior y su respuesta tardia no pisa la direccion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // El distribuidor viejo contesta tarde; el nuevo, de inmediato.
            coEvery { api.getDistributorById(1) } coAnswers {
                delay(1_000)
                Response.success(distributor(1, "Bodega anterior"))
            }
            coEvery { api.getDistributorById(2) } returns
                Response.success(distributor(2, "Bodega actual"))

            viewModel.loadPickupAddress(1)
            viewModel.loadPickupAddress(2)
            advanceUntilIdle()

            // La consulta lenta si llego a salir: lo que se prueba es que su
            // respuesta tardia quedo descartada, no que nunca se pidio.
            coVerify { api.getDistributorById(1) }
            assertEquals("Bodega actual", viewModel.pickupAddress.value)
            assertFalse(viewModel.isLoadingPickupAddress.value)
        }

    @Test
    fun `no toca el resto del estado del pedido`() = runTest {
        coEvery { api.getDistributorById(7) } returns
            Response.success(distributor(7, "Zona 4"))

        viewModel.loadPickupAddress(7)

        assertNull(viewModel.createdOrderId.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    private fun distributor(id: Int, direccion: String) = DistributorProfile(
        idDistribuidor = id,
        nombreNegocio = "Agroservicio $id",
        nombre = "Dueño $id",
        email = "distribuidor$id@agroconecta.gt",
        telefono = "55555555",
        departamento = "Guatemala",
        direccion = direccion,
        nit = "1234567-8",
        estadoVerificacion = "verificado",
        calificacionPromedio = 4.5
    )

    private fun notFound() = Response.error<DistributorProfile>(
        404,
        "".toResponseBody("application/json".toMediaTypeOrNull())
    )
}
