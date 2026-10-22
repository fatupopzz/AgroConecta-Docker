package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.models.*

data class PlantingSnapshot(val plantings: List<Planting>, val offline: Boolean = false, val cacheWarning: Boolean = false)
interface PlantingCalendarRepository {
    suspend fun templates(): List<PlantingTemplate>
    suspend fun loadPlantings(userId: Int): PlantingSnapshot
    suspend fun registerPlanting(userId: Int, request: CreatePlantingRequest): PlantingSnapshot
    suspend fun completeTask(userId: Int, taskId: Int, completed: Boolean): PlantingSnapshot
}
