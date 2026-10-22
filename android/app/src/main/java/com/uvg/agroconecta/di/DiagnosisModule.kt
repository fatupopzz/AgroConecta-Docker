package com.uvg.agroconecta.di

import com.uvg.agroconecta.data.diagnosis.PlantDiseaseClassifier
import com.uvg.agroconecta.data.diagnosis.TfLitePlantDiseaseClassifier
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
}
