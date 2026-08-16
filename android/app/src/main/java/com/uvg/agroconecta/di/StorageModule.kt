package com.uvg.agroconecta.di

import android.content.Context
import androidx.room.Room
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.connectivity.AndroidConnectivityMonitor
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.local.AgroConectaDatabase
import com.uvg.agroconecta.data.local.ProductCacheDao
import com.uvg.agroconecta.data.repository.CropCycleRepository
import com.uvg.agroconecta.data.repository.OfflineFirstProductCatalogRepository
import com.uvg.agroconecta.data.repository.ProductCatalogRepository
import com.uvg.agroconecta.data.repository.RemoteCropCycleRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AgroConectaDatabase =
        Room.databaseBuilder(
            context,
            AgroConectaDatabase::class.java,
            "agroconecta.db"
        ).build()

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
    fun provideCropCycleRepository(): CropCycleRepository = RemoteCropCycleRepository()
}
