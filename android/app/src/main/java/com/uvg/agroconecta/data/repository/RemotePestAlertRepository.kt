package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.NearbyPestAlertsResponse
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestAlertReportResponse
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.SuggestedPestProductsResponse
import retrofit2.Response

internal interface PestAlertApi {
    suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Response<NearbyPestAlertsResponse>

    suspend fun reportPest(
        request: PestAlertReportRequest
    ): Response<PestAlertReportResponse>

    suspend fun getSuggestedProducts(
        alertId: Int
    ): Response<SuggestedPestProductsResponse>
}

internal class RetrofitPestAlertApi(
    private val service: ApiService
) : PestAlertApi {
    override suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Response<NearbyPestAlertsResponse> = service.getNearbyPestAlerts(
        latitude = latitude,
        longitude = longitude,
        radiusKm = radiusKm
    )

    override suspend fun reportPest(
        request: PestAlertReportRequest
    ): Response<PestAlertReportResponse> = service.reportPestAlert(request)

    override suspend fun getSuggestedProducts(
        alertId: Int
    ): Response<SuggestedPestProductsResponse> = service.getSuggestedPestProducts(alertId)
}

class RemotePestAlertRepository internal constructor(
    private val api: PestAlertApi
) : PestAlertRepository {

    constructor(service: ApiService) : this(RetrofitPestAlertApi(service))

    override suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<PestAlert> = api.getNearbyAlerts(
        latitude = latitude,
        longitude = longitude,
        radiusKm = radiusKm
    ).requireBody("No se pudieron cargar las alertas cercanas").alertas

    override suspend fun reportPest(request: PestAlertReportRequest): PestAlert =
        api.reportPest(request)
            .requireBody("No se pudo reportar la plaga")
            .alerta

    override suspend fun getSuggestedProducts(alertId: Int): List<PestSuggestedProduct> =
        api.getSuggestedProducts(alertId)
            .requireBody("No se pudieron cargar los productos sugeridos")
            .products
}

private fun <T> Response<T>.requireBody(errorMessage: String): T {
    if (!isSuccessful) {
        error("$errorMessage (${code()})")
    }
    return body() ?: error("$errorMessage: respuesta vacía")
}

