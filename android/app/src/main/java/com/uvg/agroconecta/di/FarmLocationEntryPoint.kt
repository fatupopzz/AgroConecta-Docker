package com.uvg.agroconecta.di

import com.uvg.agroconecta.data.repository.FarmLocationSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Acceso a la ubicación de la finca desde composables que no tienen un
 * ViewModel propio para eso, como las rutas de pedido del NavHost que llenan
 * la dirección de entrega.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FarmLocationEntryPoint {
    fun farmLocationSource(): FarmLocationSource
}
