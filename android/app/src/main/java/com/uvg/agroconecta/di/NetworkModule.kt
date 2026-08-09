package com.uvg.agroconecta.di

import com.google.gson.Gson
import com.uvg.agroconecta.data.api.ApiClientFactory
import com.uvg.agroconecta.data.api.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Expone la capa de red al grafo de Hilt.
 *
 * Toda la configuracion (URL base, gson, timeouts, logging) vive en
 * [ApiClientFactory]; aqui solo se declara el ciclo de vida singleton.
 *
 * El [ApiService] que se provee es el publico, sin interceptor de auth: los
 * endpoints que requieren token lo reciben como parametro `@Header`.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = ApiClientFactory.createGson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = ApiClientFactory.createOkHttpClient()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        ApiClientFactory.createRetrofit(client, gson)

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
