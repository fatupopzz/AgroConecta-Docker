package com.uvg.agroconecta.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.uvg.agroconecta.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Fuente unica de la configuracion de red: URL base, gson, timeouts y logging.
 *
 * La consume `di.NetworkModule`, que es quien arma el ApiService del grafo de
 * Hilt. Sigue separada del modulo para que la configuracion se pueda leer y
 * probar sin arrastrar Hilt.
 */
internal object ApiClientFactory {

    /**
     * URL base ya normalizada con slash final.
     *
     * El valor viene de `BuildConfig.API_BASE_URL`, que se resuelve en
     * `app/build.gradle.kts`: propiedad de Gradle o variable de entorno
     * `AGROCONECTA_API_BASE_URL`, con fallback a `http://10.0.2.2:8080/api/`
     * en debug (emulador) y forzado a https en release.
     */
    val baseUrl: String = BuildConfig.API_BASE_URL.ensureTrailingSlash()

    fun createGson(): Gson = GsonBuilder().setLenient().create()

    fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(createLoggingInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    private fun createLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    private fun String.ensureTrailingSlash(): String {
        val normalized = trimEnd('/')
        require(normalized.isNotBlank()) {
            "API_BASE_URL is not configured. Set AGROCONECTA_API_BASE_URL in gradle.properties or as an environment variable."
        }
        return "$normalized/"
    }
}
