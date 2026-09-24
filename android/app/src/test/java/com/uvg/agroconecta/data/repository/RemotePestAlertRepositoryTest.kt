package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.models.NearbyPestAlertsResponse
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestAlertPushRegistrationRequest
import com.uvg.agroconecta.data.models.PestAlertReportRequest
import com.uvg.agroconecta.data.models.PestAlertReportResponse
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.data.models.SuggestedPestProductsResponse
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RemotePestAlertRepositoryTest {

    @Test
    fun `loads nearby alerts with requested location and radius`() = runTest {
        val alert = alert()
        val api = FakePestAlertApi(
            nearbyResponse = Response.success(
                NearbyPestAlertsResponse(total = 1, radiusKm = 15.0, alertas = listOf(alert))
            )
        )
        val repository = RemotePestAlertRepository(api)

        val result = repository.getNearbyAlerts(14.63, -90.50, 15.0)

        assertEquals(listOf(alert), result)
        assertEquals(Triple(14.63, -90.50, 15.0), api.requestedArea)
    }

    @Test
    fun `reports pest and returns created alert`() = runTest {
        val createdAlert = alert(id = 30)
        val api = FakePestAlertApi(
            reportResponse = Response.success(
                PestAlertReportResponse("Alerta de plaga creada correctamente", createdAlert)
            )
        )
        val repository = RemotePestAlertRepository(api)
        val request = PestAlertReportRequest(
            pestType = "trips",
            cultivo = "Tomate",
            latitud = 14.63,
            longitud = -90.50
        )

        val result = repository.reportPest(request)

        assertEquals(createdAlert, result)
        assertEquals(request, api.reportedRequest)
    }

    @Test
    fun `loads alert detail by notification id`() = runTest {
        val alert = alert()
        val api = FakePestAlertApi(alertResponse = Response.success(alert))
        val repository = RemotePestAlertRepository(api)

        val result = repository.getAlert(29)

        assertEquals(alert, result)
        assertEquals(29, api.requestedDetailAlertId)
    }

    @Test
    fun `loads suggested products for selected alert`() = runTest {
        val product = PestSuggestedProduct(id = 8, nombre = "Control biológico")
        val api = FakePestAlertApi(
            productsResponse = Response.success(
                SuggestedPestProductsResponse(
                    alertId = 29,
                    pestType = "pulgon",
                    cultivo = "Frijol",
                    products = listOf(product)
                )
            )
        )
        val repository = RemotePestAlertRepository(api)

        val result = repository.getSuggestedProducts(29)

        assertEquals(listOf(product), result)
        assertEquals(29, api.requestedAlertId)
    }

    @Test
    fun `registers the FCM token with current location`() = runTest {
        val api = FakePestAlertApi()
        val repository = RemotePestAlertRepository(api) { "fcm-token-29" }

        repository.syncPushRegistration(14.6349, -90.5069)

        assertEquals(
            PestAlertPushRegistrationRequest("fcm-token-29", 14.6349, -90.5069),
            api.registeredToken
        )
    }

    @Test
    fun `exposes backend status when nearby request fails`() = runTest {
        val api = FakePestAlertApi(
            nearbyResponse = Response.error(500, "".toResponseBody())
        )
        val repository = RemotePestAlertRepository(api)

        val error = try {
            repository.getNearbyAlerts(14.63, -90.50)
            null
        } catch (exception: IllegalStateException) {
            exception
        }

        assertEquals("No se pudieron cargar las alertas cercanas (500)", error?.message)
    }

    private fun alert(id: Int = 29) = PestAlert(
        id = id,
        pestType = "pulgon",
        cultivo = "Frijol",
        distanceKm = 3.4,
        latitud = 14.63,
        longitud = -90.50,
        reportedAt = "2026-09-21T15:30:00Z"
    )
}

private class FakePestAlertApi(
    private val nearbyResponse: Response<NearbyPestAlertsResponse> = Response.success(
        NearbyPestAlertsResponse()
    ),
    private val reportResponse: Response<PestAlertReportResponse>? = null,
    private val alertResponse: Response<PestAlert>? = null,
    private val productsResponse: Response<SuggestedPestProductsResponse>? = null
) : PestAlertApi {
    var requestedArea: Triple<Double, Double, Double>? = null
    var reportedRequest: PestAlertReportRequest? = null
    var requestedDetailAlertId: Int? = null
    var requestedAlertId: Int? = null
    var registeredToken: PestAlertPushRegistrationRequest? = null

    override suspend fun getNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Response<NearbyPestAlertsResponse> {
        requestedArea = Triple(latitude, longitude, radiusKm)
        return nearbyResponse
    }

    override suspend fun reportPest(
        request: PestAlertReportRequest
    ): Response<PestAlertReportResponse> {
        reportedRequest = request
        return checkNotNull(reportResponse)
    }

    override suspend fun getAlert(alertId: Int): Response<PestAlert> {
        requestedDetailAlertId = alertId
        return checkNotNull(alertResponse)
    }

    override suspend fun getSuggestedProducts(
        alertId: Int
    ): Response<SuggestedPestProductsResponse> {
        requestedAlertId = alertId
        return checkNotNull(productsResponse)
    }

    override suspend fun registerToken(
        request: PestAlertPushRegistrationRequest
    ): Response<Unit> {
        registeredToken = request
        return Response.success(Unit)
    }
}
