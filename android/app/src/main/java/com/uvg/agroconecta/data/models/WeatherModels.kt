package com.uvg.agroconecta.data.models

import com.google.gson.annotations.SerializedName

data class WeatherCurrent(
    @SerializedName("temperature_c") val temperatureC: Double,
    @SerializedName("precipitation_mm") val precipitationMm: Double
)

data class WeatherDay(
    val date: String,
    @SerializedName("temperature_min_c") val minC: Double,
    @SerializedName("temperature_max_c") val maxC: Double,
    @SerializedName("precipitation_mm") val precipitationMm: Double,
    @SerializedName("precipitation_probability_max") val rainProbability: Double
)

data class WeatherRecommendation(val code: String, val message: String)

data class WeatherReport(
    val current: WeatherCurrent,
    @SerializedName("forecast_3d") val forecast: List<WeatherDay>,
    val recommendations: List<WeatherRecommendation>,
    @SerializedName("data_time") val dataTime: String,
    @SerializedName("fetched_at") val fetchedAt: String,
    val stale: Boolean,
    val source: String
)
