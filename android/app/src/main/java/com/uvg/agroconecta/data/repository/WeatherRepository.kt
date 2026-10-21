package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.location.GeoCoordinates
import com.uvg.agroconecta.data.models.WeatherReport
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import retrofit2.HttpException
import retrofit2.Response

interface WeatherRepository {
    /** Null únicamente si no hay coordenadas en la dirección principal (404). */
    suspend fun farmWeather(): WeatherReport?
    suspend fun weatherAt(coordinates: GeoCoordinates): WeatherReport
}

class RemoteWeatherRepository(private val api: ApiService) : WeatherRepository {
    override suspend fun farmWeather(): WeatherReport? {
        val response = api.getFarmWeather()
        if (response.code() == 404) return null
        return response.weatherBody()
    }

    override suspend fun weatherAt(coordinates: GeoCoordinates): WeatherReport {
        require(coordinates.latitude.isFinite() && coordinates.latitude in -90.0..90.0 &&
            coordinates.longitude.isFinite() && coordinates.longitude in -180.0..180.0)
        return api.getWeather(coordinates.latitude, coordinates.longitude).weatherBody()
    }

    private fun Response<WeatherReport>.weatherBody(): WeatherReport {
        if (!isSuccessful) throw HttpException(this)
        val report = body() ?: throw IOException("Respuesta de clima vacía")
        try {
            // Gson puede omitir campos requeridos del contrato sin fallar al parsear.
            val current = requireNotNull(report.current)
            require(current.temperatureC.isFinite() && current.precipitationMm.isFinite() && current.precipitationMm >= 0)
            Instant.parse(report.dataTime)
            Instant.parse(report.fetchedAt)
            requireNotNull(report.recommendations).forEach { require(it.message.isNotBlank()) }
            val forecast = requireNotNull(report.forecast)
            require(forecast.size == 3)
            val dates = forecast.map {
                require(it.minC.isFinite() && it.maxC.isFinite() && it.minC <= it.maxC &&
                    it.precipitationMm.isFinite() && it.precipitationMm >= 0 &&
                    it.rainProbability.isFinite() && it.rainProbability in 0.0..100.0)
                LocalDate.parse(it.date)
            }
            require(dates[1] == dates[0].plusDays(1) && dates[2] == dates[1].plusDays(1))
        } catch (error: Exception) {
            throw IOException("Respuesta de clima incompleta", error)
        }
        return report
    }
}
