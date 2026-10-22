package com.uvg.agroconecta.di

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseClassifier
import com.uvg.agroconecta.data.diagnosis.TfLitePlantDiseaseClassifier
import com.uvg.agroconecta.data.local.AgroConectaDatabase
import com.uvg.agroconecta.data.local.PendingDiagnosisDao
import com.uvg.agroconecta.data.repository.DiagnosisRepository
import com.uvg.agroconecta.data.repository.DiagnosisSyncScheduler
import com.uvg.agroconecta.data.repository.RemoteDiagnosisRepository
import com.uvg.agroconecta.data.repository.WorkManagerDiagnosisSyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosisModule {

    @Provides
    @Singleton
    fun providePlantDiseaseClassifier(
        classifier: TfLitePlantDiseaseClassifier
    ): PlantDiseaseClassifier = classifier

    @Provides
    fun providePendingDiagnosisDao(database: AgroConectaDatabase): PendingDiagnosisDao =
        database.pendingDiagnosisDao()

    @Provides
    fun provideDiagnosisSyncScheduler(
        scheduler: WorkManagerDiagnosisSyncScheduler
    ): DiagnosisSyncScheduler = scheduler

    // Singleton porque la pantalla y el worker deben compartir el mismo mutex
    // de envío para no duplicar un diagnóstico pendiente.
    @Provides
    @Singleton
    fun provideDiagnosisRepository(
        api: ApiService,
        dao: PendingDiagnosisDao,
        connectivity: ConnectivityMonitor,
        syncScheduler: DiagnosisSyncScheduler
    ): DiagnosisRepository = RemoteDiagnosisRepository(api, dao, connectivity, syncScheduler)
}
