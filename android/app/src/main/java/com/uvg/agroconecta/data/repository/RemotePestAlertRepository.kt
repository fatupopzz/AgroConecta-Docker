package com.uvg.agroconecta.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.NearbyPestAlertsResponse
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertInstallationRequest
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestAlertReportResponse
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.SuggestedPestProductsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun registerInstallation(request: PestAlertInstallationRequest): Response<Unit>
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

    override suspend fun registerInstallation(
        request: PestAlertInstallationRequest
    ): Response<Unit> = service.registerPestAlertInstallation(request)
}

class RemotePestAlertRepository internal constructor(
    private val api: PestAlertApi,
    private val installationId: suspend () -> String? = { null }
) : PestAlertRepository {

    constructor(service: ApiService) : this(
        RetrofitPestAlertApi(service),
        ::firebaseInstallationId
    )

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

    override suspend fun syncPushInstallation(latitude: Double, longitude: Double) {
        val id = installationId() ?: return
        val response = api.registerInstallation(PestAlertInstallationRequest(id, latitude, longitude))
        if (!response.isSuccessful) error("No se pudo registrar el dispositivo (${response.code()})")
    }
}

private suspend fun firebaseInstallationId(): String = withContext(Dispatchers.IO) {
    Tasks.await(FirebaseMessaging.getInstance().register())
    Tasks.await(FirebaseInstallations.getInstance().id)
}

private fun <T> Response<T>.requireBody(errorMessage: String): T {
    if (!isSuccessful) {
        error("$errorMessage (${code()})")
    }
    return body() ?: error("$errorMessage: respuesta vacía")
}
