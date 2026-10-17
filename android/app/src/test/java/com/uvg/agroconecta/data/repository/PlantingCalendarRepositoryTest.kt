package com.uvg.agroconecta.data.repository

import com.google.gson.Gson
import com.uvg.agroconecta.data.api.ApiService
import com.uvg.agroconecta.data.local.*
import com.uvg.agroconecta.data.models.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.HttpException
import okhttp3.ResponseBody.Companion.toResponseBody

class PlantingCalendarRepositoryTest {
    private val api = mockk<ApiService>()
    private val dao = mockk<PlantingCacheDao>()
    private val repository = RemoteCropCycleRepository(api, dao)
    private val planting = Planting(1, "maíz", "Maíz", "2026-10-16", listOf(
        PlantingTask(2, "Sembrar", "sembrar", "2026-10-16", null, false)))
    private val stored = PlantingCacheEntity(7, Gson().toJson(listOf(planting)))
    @Test fun `carga remota guarda snapshot por usuario y reapertura lo lee offline`() = runTest {
        coEvery { api.getMyPlantings() } returns Response.success(listOf(planting))
        coEvery { dao.put(stored) } just Runs
        assertEquals(listOf(planting), repository.loadPlantings(7).plantings)
        coEvery { api.getMyPlantings() } throws IOException()
        coEvery { dao.get(7) } returns stored
        val reopened = RemoteCropCycleRepository(api, dao).loadPlantings(7)
        assertTrue(reopened.offline); assertEquals(listOf(planting), reopened.plantings)
        coVerify(exactly = 2) { api.getMyPlantings() }
        coVerify(exactly = 1) { dao.put(stored) }; coVerify(exactly = 1) { dao.get(7) }
        confirmVerified(api, dao)
    }
    @Test fun `marca persiste respuesta y desmarca sin mezclar usuarios`() = runTest {
        coEvery { dao.get(7) } returns stored
        coEvery { api.updatePlantingTask(2, UpdatePlantingTaskRequest(true)) } returns Response.success(PlantingTaskStatus(2, 1, true, "2026-10-16T12:00:00Z"))
        val updated = planting.copy(tasks = listOf(planting.tasks.single().copy(completed = true, completedAt = "2026-10-16T12:00:00Z")))
        val updatedCache = PlantingCacheEntity(7, Gson().toJson(listOf(updated)))
        coEvery { dao.put(updatedCache) } just Runs
        assertEquals(listOf(updated), repository.completeTask(7, 2, true).plantings)
        coEvery { api.updatePlantingTask(2, UpdatePlantingTaskRequest(false)) } returns Response.success(PlantingTaskStatus(2, 1, false, null))
        coEvery { dao.put(stored) } just Runs
        assertEquals(listOf(planting), repository.completeTask(7, 2, false).plantings)
        coVerify(exactly = 1) { dao.get(7) }
        coVerify(exactly = 1) { api.updatePlantingTask(2, UpdatePlantingTaskRequest(true)) }
        coVerify(exactly = 1) { api.updatePlantingTask(2, UpdatePlantingTaskRequest(false)) }
        coVerify(exactly = 1) { dao.put(updatedCache) }; coVerify(exactly = 1) { dao.put(stored) }
        confirmVerified(api, dao)
    }
    @Test fun `401 no muestra cache como si sesion fuera valida`() = runTest {
        coEvery { api.getMyPlantings() } returns Response.error(401, "{}".toResponseBody())
        try { repository.loadPlantings(7); fail("Debe fallar") } catch (_: HttpException) { }
        coVerify(exactly = 1) { api.getMyPlantings() }; confirmVerified(api, dao)
    }
    @Test fun `cache de otro usuario nunca se consulta`() = runTest {
        coEvery { api.getMyPlantings() } throws IOException()
        coEvery { dao.get(8) } returns null
        try { repository.loadPlantings(8); fail("No hay cache de este usuario") } catch (_: IOException) { }
        coVerify(exactly = 1) { api.getMyPlantings() }; coVerify(exactly = 1) { dao.get(8) }; confirmVerified(api, dao)
    }
    @Test fun `registro confirmado no se reporta fallido si cache falla`() = runTest {
        val request = CreatePlantingRequest("maíz", "2026-10-16")
        coEvery { dao.get(7) } returns null
        coEvery { api.createPlanting(request) } returns Response.success(planting)
        coEvery { dao.put(stored) } throws IllegalStateException("Disco lleno")
        val result = repository.registerPlanting(7, request)
        assertEquals(listOf(planting), result.plantings); assertTrue(result.cacheWarning)
        coVerify(exactly = 1) { dao.get(7) }; coVerify(exactly = 1) { api.createPlanting(request) }
        coVerify(exactly = 1) { dao.put(stored) }; confirmVerified(api, dao)
    }
}
