package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.CropPhase
import com.uvg.agroconecta.data.models.MeResponse
import com.uvg.agroconecta.data.models.PerfilInfo
import com.uvg.agroconecta.data.models.UserInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class CropCycleRepositoryTest {

    @Test
    fun `returns first active cycle configured for profile crops`() = runTest {
        val api = FakeCropCycleApi(
            crops = listOf("aguacate", "café"),
            cycles = mapOf("café" to cycle("café", "Desarrollo del fruto"))
        )
        val repository = RemoteCropCycleRepository(api)

        val result = repository.getRelevantCycle()

        assertEquals("café", result?.cultivo)
        assertEquals("Desarrollo del fruto", result?.faseActual?.fase)
        assertEquals(listOf("aguacate", "café"), api.requestedCrops)
    }

    @Test
    fun `returns null without requesting cycles when profile has no crops`() = runTest {
        val api = FakeCropCycleApi(crops = emptyList())
        val repository = RemoteCropCycleRepository(api)

        val result = repository.getRelevantCycle()

        assertNull(result)
        assertEquals(emptyList<String>(), api.requestedCrops)
    }

    private fun cycle(crop: String, phaseName: String) = CropCycleResponse(
        cultivo = crop,
        mesActual = 8,
        faseActual = CropPhase(
            idCiclo = 1,
            fase = phaseName,
            mesInicio = 8,
            mesFin = 10,
            descripcion = "Descripción",
            productosRecomendados = listOf("Fertilizante para café")
        ),
        fasesActivas = emptyList(),
        proximaFase = null
    )

    private class FakeCropCycleApi(
        private val crops: List<String>,
        private val cycles: Map<String, CropCycleResponse> = emptyMap()
    ) : CropCycleApi {
        val requestedCrops = mutableListOf<String>()

        override suspend fun getProfile(): Response<MeResponse> =
            Response.success(
                MeResponse(
                    user = UserInfo(
                        idUsuario = 4,
                        nombre = "Ana",
                        email = null,
                        telefono = null,
                        tipoUsuario = "agricultor"
                    ),
                    perfil = PerfilInfo(idAgricultor = 7, cultivos = crops)
                )
            )

        override suspend fun getCycles(crop: String): Response<CropCycleResponse> {
            requestedCrops += crop
            return Response.success(cycles[crop])
        }
    }
}
