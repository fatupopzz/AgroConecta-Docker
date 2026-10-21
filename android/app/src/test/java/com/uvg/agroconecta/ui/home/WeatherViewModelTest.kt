package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.repository.WeatherRepository
import io.mockk.*
import java.io.IOException
import java.time.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class WeatherViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<WeatherRepository>(relaxed = false)
    private var instant = Instant.parse("2026-10-21T15:10:00Z")
    private val clock = object : Clock() {
        override fun instant() = instant
        override fun getZone(): ZoneId = ZoneId.of("America/Guatemala")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant, zone)
    }
    private val vm = WeatherViewModel(repository, clock)
    private val coordinates = GeoCoordinates(14.6, -90.5)
    private val report = WeatherReport(WeatherCurrent(22.0, 0.5), (21..23).map {
        WeatherDay("2026-10-$it", 15.0, 25.0, 2.0, 60.0)
    }, listOf(WeatherRecommendation("STRONG_WIND", "No fumigues hoy, hay viento fuerte")),
        "2026-10-21T15:00:00Z", "2026-10-21T15:01:00Z", false, "Open-Meteo")
    @After fun verifyAllCalls() { confirmVerified(repository) }
    private fun verifyFarm(times: Int = 1) { coVerify(exactly = times) { repository.farmWeather() } }
    private fun load(data: WeatherReport = report) {
        coEvery { repository.farmWeather() } returns data
        vm.start(7, "agricultor", coordinates)
    }

    @Test fun `estado inicial y otros roles no consultan clima`() {
        assertEquals(WeatherUiState(), vm.state.value)
        vm.start(7, "distribuidor", coordinates); vm.refresh(); vm.onForeground()
        vm.start(0, "agricultor"); vm.refresh()
        assertNull(vm.state.value.report)
    }
    @Test fun `direccion principal tiene prioridad y muestra consejos del backend`() {
        load()
        assertEquals(report, vm.state.value.report)
        assertEquals(WeatherLocation.MAIN_ADDRESS, vm.state.value.location)
        assertFalse(vm.state.value.loading); assertFalse(vm.state.value.stale)
        assertEquals("No fumigues hoy, hay viento fuerte", vm.state.value.report!!.recommendations.single().message)
        verifyFarm()
    }
    @Test fun `usa GPS solo si no hay direccion principal`() {
        coEvery { repository.farmWeather() } returns null
        coEvery { repository.weatherAt(coordinates) } returns report
        vm.start(7, "agricultor", coordinates)
        assertEquals(WeatherLocation.CURRENT, vm.state.value.location)
        assertEquals(report, vm.state.value.report)
        verifyFarm(); coVerify(exactly = 1) { repository.weatherAt(coordinates) }
    }
    @Test fun `sin permiso ni direccion muestra instruccion sin coordenadas ficticias`() {
        coEvery { repository.farmWeather() } returns null
        vm.start(7, "agricultor")
        assertNull(vm.state.value.report); assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.error!!.contains("permite la ubicación"))
        verifyFarm()
    }
    @Test fun `cuando llega permiso y GPS carga automaticamente`() {
        coEvery { repository.farmWeather() } returns null
        vm.start(7, "agricultor")
        coEvery { repository.weatherAt(coordinates) } returns report
        vm.start(7, "agricultor", coordinates)
        assertEquals(report, vm.state.value.report)
        verifyFarm(2); coVerify(exactly = 1) { repository.weatherAt(coordinates) }
    }
    @Test fun `ignora coordenadas invalidas`() {
        coEvery { repository.farmWeather() } returns null
        vm.start(7, "agricultor", GeoCoordinates(100.0, Double.NaN))
        assertNull(vm.state.value.report)
        verifyFarm()
    }
    @Test fun `stale del backend se muestra aun con fecha reciente`() {
        load(report.copy(stale = true))
        assertTrue(vm.state.value.stale); verifyFarm()
    }
    @Test fun `dato envejece a los treinta minutos sin recargar pantalla`() {
        load()
        instant = Instant.parse("2026-10-21T15:30:59Z")
        vm.updateAge(); assertFalse(vm.state.value.stale)
        instant = instant.plusSeconds(1)
        vm.updateAge(); assertTrue(vm.state.value.stale)
        verifyFarm()
    }
    @Test fun `dato del proveedor viejo se indica aunque consulta sea nueva`() {
        load(report.copy(dataTime = "2026-10-21T13:00:00Z"))
        assertTrue(vm.state.value.stale); verifyFarm()
    }
    @Test fun `fecha invalida o futura no presenta consejos vigentes`() {
        load(report.copy(fetchedAt = "incorrecta"))
        assertTrue(vm.state.value.stale)
        coEvery { repository.farmWeather() } returns report.copy(dataTime = "2026-10-22T15:00:00Z")
        vm.refresh(); assertTrue(vm.state.value.stale)
        verifyFarm(2)
    }
    @Test fun `al volver al frente actualiza dato viejo pero conserva fresco`() {
        load(); vm.onForeground(); verifyFarm()
        instant = instant.plusSeconds(3600)
        coEvery { repository.farmWeather() } returns report.copy(dataTime = instant.toString(), fetchedAt = instant.toString())
        vm.onForeground(); assertFalse(vm.state.value.stale)
        verifyFarm(2)
    }
    @Test fun `fallo inicial tiene error y reintento exitoso lo limpia`() {
        coEvery { repository.farmWeather() } throws IOException("offline")
        vm.start(7, "agricultor", coordinates)
        assertNull(vm.state.value.report); assertNotNull(vm.state.value.error)
        coEvery { repository.farmWeather() } returns report
        vm.refresh()
        assertEquals(report, vm.state.value.report); assertNull(vm.state.value.error)
        verifyFarm(2)
    }
    @Test fun `fallo de actualizacion conserva ultimo dato marcado antiguo`() {
        load()
        coEvery { repository.farmWeather() } throws IOException("offline")
        vm.refresh()
        assertEquals(report, vm.state.value.report); assertTrue(vm.state.value.stale)
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.loading)
        verifyFarm(2)
    }
    @Test fun `401 borra ultimo dato sin fallback GPS`() {
        load()
        coEvery { repository.farmWeather() } throws HttpException(Response.error<WeatherReport>(401, "{}".toResponseBody()))
        vm.refresh()
        assertNull(vm.state.value.report); assertNull(vm.state.value.location)
        assertTrue(vm.state.value.error!!.contains("Inicia sesión")); verifyFarm(2)
    }
    @Test fun `no duplica carga por recomposicion o multiples reintentos`() = runTest {
        val gate = CompletableDeferred<WeatherReport?>()
        coEvery { repository.farmWeather() } coAnswers { gate.await() }
        vm.start(7, "agricultor", coordinates)
        vm.start(7, "agricultor", coordinates); vm.refresh(); vm.refresh()
        assertTrue(vm.state.value.loading); assertNull(vm.state.value.report)
        gate.complete(report)
        assertFalse(vm.state.value.loading); assertEquals(report, vm.state.value.report)
        verifyFarm()
    }
    @Test fun `cambiar cuenta cancela carga y descarta respuesta tardia`() = runTest {
        val gate = CompletableDeferred<WeatherReport?>()
        coEvery { repository.farmWeather() } coAnswers { withContext(NonCancellable) { gate.await() } }
        vm.start(7, "agricultor", coordinates)
        vm.start(0, "")
        gate.complete(report)
        assertEquals(WeatherUiState(), vm.state.value)
        verifyFarm()
    }
    @Test fun `otra cuenta no hereda clima ni ubicacion previa`() {
        load()
        coEvery { repository.farmWeather() } returns null
        vm.start(8, "agricultor")
        assertEquals(8, vm.state.value.ownerId); assertNull(vm.state.value.report)
        assertNull(vm.state.value.location); verifyFarm(2)
    }
    @Test fun `direccion principal no recarga por cada cambio del GPS`() {
        load()
        vm.start(7, "agricultor", GeoCoordinates(15.0, -91.0))
        assertEquals(report, vm.state.value.report); verifyFarm()
    }
    @Test fun `nuevo GPS no conserva pronostico de otra ubicacion`() {
        coEvery { repository.farmWeather() } returns null
        coEvery { repository.weatherAt(coordinates) } returns report
        vm.start(7, "agricultor", coordinates)
        val other = GeoCoordinates(15.0, -91.0)
        coEvery { repository.weatherAt(other) } throws IOException("offline")
        vm.start(7, "agricultor", other)
        assertNull(vm.state.value.report); assertNotNull(vm.state.value.error)
        verifyFarm(2)
        coVerify(exactly = 1) { repository.weatherAt(coordinates) }
        coVerify(exactly = 1) { repository.weatherAt(other) }
    }
}
