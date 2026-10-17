package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.MeResponse
import retrofit2.Response
import retrofit2.HttpException
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.local.PlantingCacheDao
import com.uvg.agroconecta.data.local.PlantingCacheEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

interface CropCycleRepository {
    suspend fun getRelevantCycle(): CropCycleResponse?
}

internal interface CropCycleApi {
    suspend fun getProfile(): Response<MeResponse>
    suspend fun getCycles(crop: String): Response<CropCycleResponse>
}

private class RetrofitCropCycleApi(
    private val service: ApiService
) : CropCycleApi {
    override suspend fun getProfile(): Response<MeResponse> =
        service.getMe()

    override suspend fun getCycles(crop: String): Response<CropCycleResponse> =
        service.getCropCycles(crop)
}

class RemoteCropCycleRepository internal constructor(
    private val api: CropCycleApi,
    private val service: ApiService? = null,
    private val cache: PlantingCacheDao? = null
) : CropCycleRepository, PlantingCalendarRepository {

    // El constructor real recibe el ApiService de Hilt; el interno con
    // CropCycleApi existe para poder falsear la red en los tests.
    constructor(service: ApiService, cache: PlantingCacheDao? = null) : this(RetrofitCropCycleApi(service), service, cache)

    private val calendarMutex = Mutex()
    private val gson = Gson()
    private val listType = object : TypeToken<List<Planting>>() {}.type
    private val snapshots = mutableMapOf<Int, List<Planting>>()

    private fun <T> Response<T>.required(): T {
        if (!isSuccessful) throw HttpException(this)
        return body() ?: error("Respuesta vacía del calendario")
    }

    override suspend fun templates(): List<PlantingTemplate> = requireNotNull(service).getPlantingTemplates().required()

    private suspend fun cached(userId: Int): List<Planting>? =
        snapshots[userId] ?: cache?.get(userId)?.let { gson.fromJson<List<Planting>>(it.payload, listType) }

    private suspend fun save(userId: Int, plantings: List<Planting>): PlantingSnapshot {
        snapshots[userId] = plantings
        // Una escritura local fallida no debe convertir un POST confirmado en un reintento duplicado.
        val warning = try {
            requireNotNull(cache).put(PlantingCacheEntity(userId, gson.toJson(plantings)))
            false
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { true }
        return PlantingSnapshot(plantings, cacheWarning = warning)
    }

    override suspend fun loadPlantings(userId: Int): PlantingSnapshot = calendarMutex.withLock {
        require(userId > 0)
        val remote = try { requireNotNull(service).getMyPlantings().required() }
        catch (offline: IOException) {
            return@withLock PlantingSnapshot(cached(userId) ?: throw offline, offline = true)
        }
        save(userId, remote)
    }

    override suspend fun registerPlanting(userId: Int, request: CreatePlantingRequest): PlantingSnapshot = calendarMutex.withLock {
        require(userId > 0)
        val previous = cached(userId).orEmpty()
        val created = requireNotNull(service).createPlanting(request).required()
        save(userId, previous.filterNot { it.id == created.id } + created)
    }

    override suspend fun completeTask(userId: Int, taskId: Int, completed: Boolean): PlantingSnapshot = calendarMutex.withLock {
        require(userId > 0)
        val previous = requireNotNull(cached(userId))
        require(previous.any { planting -> planting.tasks.any { it.id == taskId } })
        val updated = requireNotNull(service).updatePlantingTask(taskId, UpdatePlantingTaskRequest(completed)).required()
        save(userId, previous.map { planting -> planting.copy(tasks = planting.tasks.map { task ->
            if (task.id == updated.id && planting.id == updated.plantingId)
                task.copy(completed = updated.completed, completedAt = updated.completedAt) else task
        }) })
    }


    override suspend fun getRelevantCycle(): CropCycleResponse? {
        val profileResponse = api.getProfile()
        if (!profileResponse.isSuccessful) {
            error("No se pudo consultar el perfil del agricultor")
        }

        val crops = profileResponse.body()?.perfil?.cultivos.orEmpty()
        for (crop in crops) {
            val cycleResponse = api.getCycles(crop)
            val cycle = cycleResponse.body()
            if (cycleResponse.isSuccessful && cycle?.faseActual != null) {
                return cycle
            }
        }

        return null
    }
}
