package com.uvg.agroconecta.di

import android.content.Context
import com.uvg.agroconecta.data.local.MIGRATION_3_4
import com.uvg.agroconecta.data.repository.PlantingCalendarRepository
import java.time.Clock
import java.time.ZoneId
import androidx.room.Room
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.api.dataStore
import com.uvg.agroconecta.data.connectivity.AndroidConnectivityMonitor
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.images.ContentResolverProductImageReader
import com.uvg.agroconecta.data.images.ProductImageReader
import com.uvg.agroconecta.data.local.AgroConectaDatabase
import com.uvg.agroconecta.data.local.MIGRATION_1_2
import com.uvg.agroconecta.data.local.MIGRATION_2_3
import com.uvg.agroconecta.data.local.DataStoreFavoriteLocalDataSource
import com.uvg.agroconecta.data.local.FavoriteLocalDataSource
import com.uvg.agroconecta.data.local.ProductCacheDao
import com.uvg.agroconecta.data.location.AndroidCurrentLocationProvider
import com.uvg.agroconecta.data.location.CurrentLocationProvider
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.FavoriteRepository
import com.uvg.agroconecta.data.repository.OfflineFirstProductCatalogRepository
import com.uvg.agroconecta.data.repository.ProductCatalogRepository
import com.uvg.agroconecta.data.repository.PestAlertRepository
import com.uvg.agroconecta.data.repository.RemoteCropCycleRepository
import com.uvg.agroconecta.data.repository.RemoteFavoriteRepository
import com.uvg.agroconecta.data.repository.RemotePestAlertRepository
import com.uvg.agroconecta.data.repository.RetrofitProductCatalogApi
import com.uvg.agroconecta.data.repository.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideWeatherRepository(api: ApiService): com.uvg.agroconecta.data.repository.WeatherRepository =
        com.uvg.agroconecta.data.repository.RemoteWeatherRepository(api)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AgroConectaDatabase =
        Room.databaseBuilder(
            context,
            AgroConectaDatabase::class.java,
            "agroconecta.db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideProductCacheDao(database: AgroConectaDatabase): ProductCacheDao =
        database.productCacheDao()

    @Provides
    @Singleton
    fun provideConnectivityMonitor(
        monitor: AndroidConnectivityMonitor
    ): ConnectivityMonitor = monitor

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = TimeProvider(System::currentTimeMillis)

    @Provides
    @Singleton
    fun provideProductCatalogRepository(
        apiService: ApiService,
        dao: ProductCacheDao,
        connectivityMonitor: ConnectivityMonitor,
        timeProvider: TimeProvider
    ): ProductCatalogRepository = OfflineFirstProductCatalogRepository(
        api = RetrofitProductCatalogApi(apiService),
        dao = dao,
        connectivityMonitor = connectivityMonitor,
        timeProvider = timeProvider
    )

    @Provides
    @Singleton
    fun provideRemoteCropCycleRepository(apiService: ApiService, database: AgroConectaDatabase): RemoteCropCycleRepository =
        RemoteCropCycleRepository(apiService, database.plantingCacheDao())

    @Provides
    fun provideCropCycleRepository(repository: RemoteCropCycleRepository): CropCycleRepository = repository

    @Provides
    fun providePlantingCalendarRepository(repository: RemoteCropCycleRepository): PlantingCalendarRepository = repository

    @Provides
    fun provideCalendarClock(): Clock = Clock.system(ZoneId.of("America/Guatemala"))

    @Provides
    @Singleton
    fun providePestAlertRepository(apiService: ApiService): PestAlertRepository =
        RemotePestAlertRepository(apiService)

    @Provides
    @Singleton
    fun provideFavoriteLocalDataSource(
        @ApplicationContext context: Context
    ): FavoriteLocalDataSource = DataStoreFavoriteLocalDataSource(context.dataStore)

    @Provides
    @Singleton
    fun provideFavoriteRepository(
        apiService: ApiService,
        localDataSource: FavoriteLocalDataSource
    ): FavoriteRepository = RemoteFavoriteRepository(apiService, localDataSource)

    @Provides
    @Singleton
    fun provideCurrentLocationProvider(
        provider: AndroidCurrentLocationProvider
    ): CurrentLocationProvider = provider

    @Provides
    @Singleton
    fun provideProductImageReader(
        reader: ContentResolverProductImageReader
    ): ProductImageReader = reader
}
