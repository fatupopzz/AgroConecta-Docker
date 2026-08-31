package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.MeResponse
import retrofit2.Response

interface CropCycleRepository {
    suspend fun getRelevantCycle(token: String): CropCycleResponse?
}

internal interface CropCycleApi {
    suspend fun getProfile(token: String): Response<MeResponse>
    suspend fun getCycles(crop: String): Response<CropCycleResponse>
}

private class RetrofitCropCycleApi(
    private val service: ApiService
) : CropCycleApi {
    override suspend fun getProfile(token: String): Response<MeResponse> =
        service.getMe("Bearer $token")

    override suspend fun getCycles(crop: String): Response<CropCycleResponse> =
        service.getCropCycles(crop)
}

class RemoteCropCycleRepository internal constructor(
    private val api: CropCycleApi
) : CropCycleRepository {

    // El constructor real recibe el ApiService de Hilt; el interno con
    // CropCycleApi existe para poder falsear la red en los tests.
    constructor(service: ApiService) : this(RetrofitCropCycleApi(service))

    override suspend fun getRelevantCycle(token: String): CropCycleResponse? {
        val profileResponse = api.getProfile(token)
        if (!profileResponse.isSuccessful) {
            error("No se pudo consultar el perfil del agricultor")
        }

        val crops = profileResponse.body()?.perfil?.cultivos.orEmpty()
        for (crop in crops) {
            val cycleResponse = api.getCycles(crop)
            val cycle = cycleResponse.body()
            if (cycleResponse.isSuccessful && cycle?.faseActual != null) {
                return cycle
            }
        }

        return null
    }
}
