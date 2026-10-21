package com.uvg.agroconecta.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.WeatherReport
import com.uvg.agroconecta.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class WeatherLocation(val label: String) {
    MAIN_ADDRESS("Dirección de entrega principal"), CURRENT("Tu ubicación actual")
}

data class WeatherUiState(
    val ownerId: Int = 0,
    val loading: Boolean = false,
    val report: WeatherReport? = null,
    val location: WeatherLocation? = null,
    val stale: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val clock: Clock
) : ViewModel() {
    private val _state = MutableStateFlow(WeatherUiState())
    val state = _state.asStateFlow()
    private var fallback: GeoCoordinates? = null
    private var request: Job? = null
    private var generation = 0

    fun start(userId: Int, userType: String, coordinates: GeoCoordinates? = null) {
        val owner = userId.takeIf { it > 0 && userType == "agricultor" } ?: 0
        val validCoordinates = coordinates?.takeIf {
            it.latitude.isFinite() && it.latitude in -90.0..90.0 &&
                it.longitude.isFinite() && it.longitude in -180.0..180.0
        }
        if (_state.value.ownerId != owner) {
            generation++
            request?.cancel()
            fallback = validCoordinates
            _state.value = WeatherUiState(ownerId = owner)
            if (owner > 0) refresh()
        } else if (fallback != validCoordinates) {
            fallback = validCoordinates
            if (owner > 0 && _state.value.location != WeatherLocation.MAIN_ADDRESS) {
                // No conservar el pronóstico de otra ubicación mientras cambia GPS.
                generation++
                request?.cancel()
                _state.value = WeatherUiState(ownerId = owner)
                refresh()
            }
        }
    }

    fun refresh() {
        val current = _state.value
        if (current.ownerId <= 0 || current.loading) return
        val version = ++generation
        _state.update { it.copy(loading = true, error = null) }
        request = viewModelScope.launch {
            try {
                val farm = repository.farmWeather()
                val coordinates = fallback
                val report = farm ?: coordinates?.let { repository.weatherAt(it) }
                if (version != generation) return@launch
                if (report == null) {
                    _state.update { it.copy(error = "Guarda coordenadas en tu dirección principal o permite la ubicación para ver el clima.") }
                } else {
                    _state.update { it.copy(report = report,
                        location = if (farm != null) WeatherLocation.MAIN_ADDRESS else WeatherLocation.CURRENT,
                        stale = isOld(report), error = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version != generation) return@launch
                val unauthorized = error is HttpException && error.code() in listOf(401, 403)
                _state.update { it.copy(
                    report = if (unauthorized) null else it.report,
                    location = if (unauthorized) null else it.location,
                    stale = !unauthorized && it.report != null,
                    error = if (unauthorized) "Inicia sesión como agricultor para consultar el clima."
                        else "No se pudo actualizar el clima. Revisa tu conexión y reintenta."
                ) }
            } finally {
                if (version == generation) _state.update { it.copy(loading = false) }
            }
        }
    }

    fun updateAge() {
        _state.update { state -> state.copy(stale = state.stale || state.report?.let(::isOld) == true) }
    }

    fun onForeground() {
        updateAge()
        if (_state.value.stale || _state.value.report == null) refresh()
    }

    private fun isOld(report: WeatherReport): Boolean = try {
        val now = clock.instant()
        val fetched = Instant.parse(report.fetchedAt)
        val data = Instant.parse(report.dataTime)
        report.stale || Duration.between(fetched, now) >= Duration.ofMinutes(30) ||
            Duration.between(data, now) > Duration.ofHours(1) ||
            fetched.isAfter(now.plusSeconds(300)) || data.isAfter(now.plusSeconds(300)) ||
            LocalDate.parse(report.forecast.first().date) != LocalDate.now(clock.withZone(ZoneId.of("America/Guatemala")))
    } catch (_: Exception) { true }
}
