package com.uvg.agroconecta.ui.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.BusinessLocationState
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.RegisterRequest
import com.uvg.agroconecta.data.models.TipoCuenta
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class AuthViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Mock estricto a proposito: no lleva `relaxed = true`, asi que cualquier
     * llamada a la red que no este explicitamente stubbeada revienta el test en
     * lugar de salir al backend real.
     */
    private lateinit var api: ApiService
    private lateinit var locationProvider: FakeCurrentLocationProvider
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        api = mockk()
        locationProvider = FakeCurrentLocationProvider()
        viewModel = AuthViewModel(api, locationProvider)
    }

    @Test
    fun `estado inicial de login es Idle`() {
        assertEquals(AuthState.Idle, viewModel.loginState.value)
    }

    @Test
    fun `estado inicial de registro es Idle`() {
        assertEquals(AuthState.Idle, viewModel.registerState.value)
    }

    @Test
    fun `nombre usuario inicia vacio`() {
        assertEquals("", viewModel.nombreUsuario.value)
    }

    @Test
    fun `draft inicial tiene tipo agricultor por defecto`() {
        assertEquals(TipoCuenta.AGRICULTOR, viewModel.registerDraft.value?.tipoCuenta)
    }

    @Test
    fun `updateDraft actualiza nombre correctamente`() {
        viewModel.updateDraft { it.copy(nombre = "Fatima") }
        assertEquals("Fatima", viewModel.registerDraft.value?.nombre)
    }

    @Test
    fun `updateDraft actualiza email correctamente`() {
        viewModel.updateDraft { it.copy(email = "fatima@test.com") }
        assertEquals("fatima@test.com", viewModel.registerDraft.value?.email)
    }

    @Test
    fun `updateDraft cambia tipo de cuenta a distribuidor`() {
        viewModel.updateDraft { it.copy(tipoCuenta = TipoCuenta.DISTRIBUIDOR) }
        assertEquals(TipoCuenta.DISTRIBUIDOR, viewModel.registerDraft.value?.tipoCuenta)
    }

    @Test
    fun `updateDraft actualiza multiples campos`() {
        viewModel.updateDraft {
            it.copy(
                nombre = "Juan",
                email = "juan@test.com",
                telefono = "55551234",
                departamento = "Guatemala"
            )
        }
        val draft = viewModel.registerDraft.value
        assertNotNull(draft)
        assertEquals("Juan", draft?.nombre)
        assertEquals("juan@test.com", draft?.email)
        assertEquals("55551234", draft?.telefono)
        assertEquals("Guatemala", draft?.departamento)
    }

    @Test
    fun `resetRegister limpia draft y restaura estado`() {
        viewModel.updateDraft { it.copy(nombre = "Test", email = "test@test.com") }
        viewModel.resetRegister()

        val draft = viewModel.registerDraft.value
        assertEquals("", draft?.nombre)
        assertEquals("", draft?.email)
        assertEquals(AuthState.Idle, viewModel.registerState.value)
        // Editar y limpiar el draft es estado local: no debe tocar la red.
        verify { api wasNot Called }
    }

    @Test
    fun `resetLogin restaura estado a Idle y limpia nombre`() {
        viewModel.resetLogin()
        assertEquals(AuthState.Idle, viewModel.loginState.value)
        assertEquals("", viewModel.nombreUsuario.value)
        verify { api wasNot Called }
    }

    @Test
    fun `capturar ubicacion la guarda en el draft del distribuidor`() {
        locationProvider.coordinates = GeoCoordinates(14.6349, -90.5069)

        viewModel.captureBusinessLocation()

        assertEquals(14.6349, viewModel.registerDraft.value?.latitude!!, 0.0)
        assertEquals(-90.5069, viewModel.registerDraft.value?.longitude!!, 0.0)
        assertTrue(viewModel.businessLocationState.value is BusinessLocationState.Located)
    }

    @Test
    fun `registro envia coordenadas del negocio al backend`() {
        val request = slot<RegisterRequest>()
        coEvery { api.register(capture(request)) } returns Response.success(emptyMap())
        viewModel.updateDraft {
            it.copy(
                tipoCuenta = TipoCuenta.DISTRIBUIDOR,
                nombre = "Ana",
                telefono = "55551234",
                email = "ana@test.com",
                password = "secreto",
                nombreNegocio = "Agro Ana",
                latitude = 14.6349,
                longitude = -90.5069
            )
        }

        viewModel.submitRegister()

        coVerify(exactly = 1) { api.register(any()) }
        assertEquals(14.6349, request.captured.latitude!!, 0.0)
        assertEquals(-90.5069, request.captured.longitude!!, 0.0)
        assertEquals(AuthState.Success, viewModel.registerState.value)
    }
}

private class FakeCurrentLocationProvider : CurrentLocationProvider {
    var coordinates: GeoCoordinates? = null

    override suspend fun getCurrentCoordinates(): GeoCoordinates? = coordinates
}
