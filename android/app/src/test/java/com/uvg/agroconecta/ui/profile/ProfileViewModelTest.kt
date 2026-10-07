package com.uvg.agroconecta.ui.profile

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.BusinessLocationState
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.MeResponse
import com.uvg.agroconecta.data.models.PerfilInfo
import com.uvg.agroconecta.data.models.UpdateMyProfileRequest
import com.uvg.agroconecta.data.models.UserInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: ApiService
    private lateinit var locationProvider: FakeCurrentLocationProvider
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        api = mockk()
        locationProvider = FakeCurrentLocationProvider()
        viewModel = ProfileViewModel(api, locationProvider)
    }

    @Test
    fun `capturar ubicacion del negocio publica las coordenadas`() {
        locationProvider.coordinates = GeoCoordinates(14.6349, -90.5069)

        viewModel.captureBusinessLocation()

        val state = viewModel.businessLocationState.value
        assertTrue(state is BusinessLocationState.Located)
        state as BusinessLocationState.Located
        assertEquals(14.6349, state.coordinates.latitude, 0.0)
        assertEquals(-90.5069, state.coordinates.longitude, 0.0)
    }

    @Test
    fun `guardar perfil envia coordenadas del distribuidor`() {
        val request = slot<UpdateMyProfileRequest>()
        coEvery { api.updateMe(capture(request)) } returns Response.success(distributorResponse())
        var saved = false

        viewModel.saveProfile(
            ProfileEditDraft(
                nombre = "Ana",
                telefono = "55551234",
                nombreNegocio = "Agro Ana",
                latitude = 14.6349,
                longitude = -90.5069
            )
        ) { saved = true }

        coVerify(exactly = 1) { api.updateMe(any()) }
        assertEquals(14.6349, request.captured.latitude!!, 0.0)
        assertEquals(-90.5069, request.captured.longitude!!, 0.0)
        assertTrue(saved)
    }

    private fun distributorResponse() = MeResponse(
        user = UserInfo(
            idUsuario = 1,
            nombre = "Ana",
            email = "ana@test.com",
            telefono = "55551234",
            tipoUsuario = "distribuidor"
        ),
        perfil = PerfilInfo(
            idDistribuidor = 2,
            nombreNegocio = "Agro Ana",
            latitude = 14.6349,
            longitude = -90.5069
        )
    )
}

private class FakeCurrentLocationProvider : CurrentLocationProvider {
    var coordinates: GeoCoordinates? = null

    override suspend fun getCurrentCoordinates(): GeoCoordinates? = coordinates
}
