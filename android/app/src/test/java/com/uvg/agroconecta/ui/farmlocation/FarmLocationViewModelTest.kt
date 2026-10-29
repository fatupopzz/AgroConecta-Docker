package com.uvg.agroconecta.ui.farmlocation

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.FarmLocation
import com.uvg.agroconecta.data.models.SaveFarmLocationRequest
import com.uvg.agroconecta.data.repository.FarmLocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class FarmLocationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock estricto: cualquier llamada al servidor que el test no prepare falla.
    private val api = mockk<ApiService>()

    private val tecpan = FarmLocation(
        departamento = "Chimaltenango",
        municipio = "Tecpán Guatemala",
        referencia = "Frente a la escuela",
        latitude = 14.76123,
        longitude = -91.00123
    )

    private class FakeGps(
        private val coordinates: GeoCoordinates? = null,
        private val error: Throwable? = null
    ) : CurrentLocationProvider {
        override suspend fun getCurrentCoordinates(): GeoCoordinates? {
            error?.let { throw it }
            return coordinates
        }
    }

    private fun viewModel(gps: CurrentLocationProvider = FakeGps()) =
        FarmLocationViewModel(FarmLocationRepository(api), gps)

    @Test
    fun `carga la ubicación guardada en el formulario`() {
        coEvery { api.getFarmLocation() } returns Response.success(tecpan)

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Chimaltenango", state.departamento)
        assertEquals("Tecpán Guatemala", state.municipio)
        assertEquals("Frente a la escuela", state.referencia)
        assertEquals(GeoCoordinates(14.76123, -91.00123), state.coordinates)
    }

    @Test
    fun `si todavía no la guardó muestra el formulario vacío sin error`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("", state.departamento)
        assertNull(state.error)
        assertFalse(state.canSave)
    }

    @Test
    fun `al cambiar de departamento se borra un municipio que no le pertenece`() {
        coEvery { api.getFarmLocation() } returns Response.success(tecpan)
        val viewModel = viewModel()

        viewModel.onDepartamentoChange("Sololá")

        assertEquals("Sololá", viewModel.uiState.value.departamento)
        assertEquals("", viewModel.uiState.value.municipio)
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `marca la finca con el GPS`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())
        val viewModel = viewModel(FakeGps(GeoCoordinates(14.6, -90.5)))

        viewModel.useCurrentLocation()

        assertEquals(GeoCoordinates(14.6, -90.5), viewModel.uiState.value.coordinates)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `sin permiso de GPS explica que puede guardar solo el municipio`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())
        val viewModel = viewModel(FakeGps(error = SecurityException("sin permiso")))

        viewModel.useCurrentLocation()

        assertTrue(viewModel.uiState.value.locationError!!.contains("solo el municipio"))
        assertNull(viewModel.uiState.value.coordinates)
    }

    @Test
    fun `guarda municipio, referencia y coordenadas`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())
        val expected = SaveFarmLocationRequest(
            departamento = "Chimaltenango",
            municipio = "Tecpán Guatemala",
            referencia = "Frente a la escuela",
            latitude = 14.6,
            longitude = -90.5
        )
        coEvery { api.saveFarmLocation(expected) } returns Response.success(tecpan)
        val viewModel = viewModel(FakeGps(GeoCoordinates(14.6, -90.5)))

        viewModel.onDepartamentoChange("Chimaltenango")
        viewModel.onMunicipioChange("Tecpán Guatemala")
        viewModel.onReferenciaChange("  Frente a la escuela  ".trim())
        viewModel.useCurrentLocation()
        viewModel.save()

        assertTrue(viewModel.uiState.value.saved)
        coVerify(exactly = 1) { api.saveFarmLocation(expected) }
    }

    @Test
    fun `sin referencia ni GPS manda nulos en vez de textos vacíos`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())
        val expected = SaveFarmLocationRequest("Sololá", "Panajachel", null, null, null)
        coEvery { api.saveFarmLocation(expected) } returns
            Response.success(FarmLocation("Sololá", "Panajachel"))
        val viewModel = viewModel()

        viewModel.onDepartamentoChange("Sololá")
        viewModel.onMunicipioChange("Panajachel")
        viewModel.onReferenciaChange("   ")
        viewModel.save()

        coVerify(exactly = 1) { api.saveFarmLocation(expected) }
    }

    @Test
    fun `si no se pudo guardar lo avisa y no marca como guardada`() {
        coEvery { api.getFarmLocation() } returns Response.error(404, "".toResponseBody())
        coEvery { api.saveFarmLocation(any()) } returns Response.error(500, "".toResponseBody())
        val viewModel = viewModel()

        viewModel.onDepartamentoChange("Sololá")
        viewModel.onMunicipioChange("Panajachel")
        viewModel.save()

        assertFalse(viewModel.uiState.value.saved)
        assertTrue(viewModel.uiState.value.error!!.contains("No se pudo guardar"))
    }

    @Test
    fun `la dirección de entrega va de lo específico a lo general`() {
        assertEquals("Frente a la escuela, Tecpán Guatemala, Chimaltenango", tecpan.deliveryText())
        assertEquals("Panajachel, Sololá", FarmLocation("Sololá", "Panajachel", referencia = " ").deliveryText())
    }
}
