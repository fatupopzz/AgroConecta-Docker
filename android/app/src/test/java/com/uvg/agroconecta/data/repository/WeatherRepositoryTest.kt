package com.uvg.agroconecta.data.repository

import com.google.gson.Gson
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class WeatherRepositoryTest {
    private val api = mockk<ApiService>(relaxed = false)
    private val repository = RemoteWeatherRepository(api)
    private val report = WeatherReport(WeatherCurrent(22.0, 0.5), (21..23).map {
        WeatherDay("2026-10-$it", 15.0, 25.0, 2.0, 60.0)
    }, listOf(WeatherRecommendation("WIND", "No fumigues hoy")), "2026-10-21T15:00:00Z", "2026-10-21T15:01:00Z", false, "Open-Meteo")

    @Test fun `carga direccion principal y conserva consejo y stale`() = runTest {
        coEvery { api.getFarmWeather() } returns Response.success(report.copy(stale = true))
        assertEquals(report.copy(stale = true), repository.farmWeather())
        coVerify(exactly = 1) { api.getFarmWeather() }; confirmVerified(api)
    }
    @Test fun `solo 404 permite resolver GPS`() = runTest {
        coEvery { api.getFarmWeather() } returns Response.error(404, "{}".toResponseBody())
        assertNull(repository.farmWeather())
        coVerify(exactly = 1) { api.getFarmWeather() }; confirmVerified(api)
    }
    @Test fun `401 403 y 503 se propagan sin convertirlos a falta de direccion`() = runTest {
        listOf(401, 403, 503).forEach { code ->
            coEvery { api.getFarmWeather() } returns Response.error(code, "{}".toResponseBody())
            try { repository.farmWeather(); fail("Debe fallar") } catch (error: HttpException) { assertEquals(code, error.code()) }
        }
        coVerify(exactly = 3) { api.getFarmWeather() }; confirmVerified(api)
    }
    @Test fun `consulta coordenadas exactas usando el interceptor habitual`() = runTest {
        coEvery { api.getWeather(14.6, -90.5) } returns Response.success(report)
        assertEquals(report, repository.weatherAt(GeoCoordinates(14.6, -90.5)))
        coVerify(exactly = 1) { api.getWeather(14.6, -90.5) }; confirmVerified(api)
    }
    @Test fun `rechaza coordenadas invalidas sin consultar API`() = runTest {
        try { repository.weatherAt(GeoCoordinates(Double.NaN, 0.0)); fail("Debe fallar") } catch (_: IllegalArgumentException) { }
        confirmVerified(api)
    }
    @Test fun `rechaza contrato antiguo sin tres dias en vez de inventar pronostico`() = runTest {
        coEvery { api.getFarmWeather() } returns Response.success(report.copy(forecast = emptyList()))
        try { repository.farmWeather(); fail("Debe fallar") } catch (_: IOException) { }
        coVerify(exactly = 1) { api.getFarmWeather() }; confirmVerified(api)
    }
    @Test fun `deserializa nombres del backend y rechaza campos faltantes`() = runTest {
        val json = Gson().toJson(report)
        assertTrue(json.contains("forecast_3d")); assertTrue(json.contains("data_time"))
        val missing = Gson().fromJson("{}", WeatherReport::class.java)
        coEvery { api.getFarmWeather() } returns Response.success(missing)
        try { repository.farmWeather(); fail("Debe fallar") } catch (_: IOException) { }
        coVerify(exactly = 1) { api.getFarmWeather() }; confirmVerified(api)
    }
}
