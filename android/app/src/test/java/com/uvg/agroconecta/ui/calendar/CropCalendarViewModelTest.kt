package com.uvg.agroconecta.ui.calendar

import com.uvg.agroconecta.MainDispatcherRule
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.repository.*
import io.mockk.*
import java.time.*
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CropCalendarViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<PlantingCalendarRepository>()
    // UTC ya es 17; en Guatemala todavía es 16.
    private val clock = Clock.fixed(Instant.parse("2026-10-17T02:00:00Z"), ZoneId.of("America/Guatemala"))
    private val vm = CropCalendarViewModel(repository, clock)
    private val templates = listOf(PlantingTemplate(1, "maíz", "Maíz", "Descripción", 80))
    private val planting = Planting(1, "maíz", "Maíz", "2026-10-15", listOf(
        PlantingTask(3, "Cosechar", "cosechar", "2026-10-17", null, false),
        PlantingTask(1, "Siembra", "sembrar", "2026-10-15", PlantingCategory(4, "Semillas"), false),
        PlantingTask(2, "Fertilizar", "fertilizar", "2026-10-16", PlantingCategory(1, "Fertilizantes"), false)
    ))
    private fun start(plantings: List<Planting> = listOf(planting), offline: Boolean = false) {
        coEvery { repository.templates() } returns templates
        coEvery { repository.loadPlantings(7) } returns PlantingSnapshot(plantings, offline)
        vm.start(7, "agricultor")
    }
    private fun verifyLoad(times: Int = 1) {
        coVerify(exactly = times) { repository.templates() }
        coVerify(exactly = times) { repository.loadPlantings(7) }
    }
    @Test fun `estado inicial no hace llamadas`() {
        assertEquals(CropCalendarState(), vm.state.value)
        confirmVerified(repository)
    }
    @Test fun `carga ordena clasifica y destaca hoy en Guatemala`() {
        start()
        assertEquals(listOf(1,2,3), vm.state.value.tasks.map { it.task.id })
        assertEquals(listOf(TaskPeriod.PAST, TaskPeriod.TODAY, TaskPeriod.UPCOMING), vm.state.value.tasks.map { it.period })
        assertEquals("Hoy", vm.state.value.tasks[1].period.label)
        assertTrue(vm.state.value.loaded)
        assertFalse(vm.state.value.loading)
        verifyLoad(); confirmVerified(repository)
    }
    @Test fun `registro valido persiste con contrato del servidor`() {
        start(emptyList())
        val request = CreatePlantingRequest("maíz", "2026-10-15")
        coEvery { repository.registerPlanting(7, request) } returns PlantingSnapshot(listOf(planting))
        vm.selectCrop("maíz"); vm.changeDate("2026-10-15"); vm.register()
        assertEquals(3, vm.state.value.tasks.size)
        assertEquals("Siembra registrada.", vm.state.value.message)
        assertEquals("", vm.state.value.date)
        verifyLoad(); coVerify(exactly = 1) { repository.registerPlanting(7, request) }; confirmVerified(repository)
    }
    @Test fun `rechaza cultivo obligatorio fecha inexistente formato y rango`() {
        start()
        vm.register(); assertEquals("Selecciona un cultivo.", vm.state.value.formError)
        vm.selectCrop("maíz")
        listOf("", "2026-02-30", "16/10/2026", "2028-10-16").forEach {
            vm.changeDate(it); vm.register(); assertNotNull(vm.state.value.formError)
        }
        verifyLoad(); confirmVerified(repository)
    }
    @Test fun `registro en limite de 365 dias es valido`() {
        start(emptyList())
        val date = LocalDate.now(clock).minusDays(365).toString()
        val request = CreatePlantingRequest("maíz", date)
        coEvery { repository.registerPlanting(7, request) } returns PlantingSnapshot(listOf(planting))
        vm.selectCrop("maíz"); vm.changeDate(date); vm.register()
        assertNull(vm.state.value.formError)
        verifyLoad(); coVerify(exactly = 1) { repository.registerPlanting(7, request) }; confirmVerified(repository)
    }
    @Test fun `no duplica registro mientras espera respuesta`() = runTest {
        start(emptyList())
        val gate = CompletableDeferred<PlantingSnapshot>()
        val request = CreatePlantingRequest("maíz", "2026-10-15")
        coEvery { repository.registerPlanting(7, request) } coAnswers { gate.await() }
        vm.selectCrop("maíz"); vm.changeDate("2026-10-15"); vm.register(); vm.register()
        assertTrue(vm.state.value.saving)
        gate.complete(PlantingSnapshot(listOf(planting)))
        verifyLoad(); coVerify(exactly = 1) { repository.registerPlanting(7, request) }; confirmVerified(repository)
    }
    @Test fun `marcar desmarcar y recargar conserva estado confirmado`() {
        start()
        val completed = planting.copy(tasks = planting.tasks.map { if (it.id == 2) it.copy(completed = true) else it })
        coEvery { repository.completeTask(7, 2, true) } returns PlantingSnapshot(listOf(completed))
        vm.toggleTask(2)
        assertTrue(vm.state.value.tasks[1].task.completed)
        coEvery { repository.loadPlantings(7) } returns PlantingSnapshot(listOf(completed))
        vm.refresh()
        assertTrue(vm.state.value.tasks[1].task.completed)
        coEvery { repository.completeTask(7, 2, false) } returns PlantingSnapshot(listOf(planting))
        vm.toggleTask(2)
        assertFalse(vm.state.value.tasks[1].task.completed)
        verifyLoad(2)
        coVerify(exactly = 1) { repository.completeTask(7, 2, true) }
        coVerify(exactly = 1) { repository.completeTask(7, 2, false) }
        confirmVerified(repository)
    }
    @Test fun `no duplica actualizacion de tarea`() = runTest {
        start()
        val gate = CompletableDeferred<PlantingSnapshot>()
        coEvery { repository.completeTask(7, 2, true) } coAnswers { gate.await() }
        vm.toggleTask(2); vm.toggleTask(2)
        assertEquals(setOf(2), vm.state.value.pendingTasks)
        gate.complete(PlantingSnapshot(listOf(planting)))
        verifyLoad(); coVerify(exactly = 1) { repository.completeTask(7, 2, true) }; confirmVerified(repository)
    }
    @Test fun `abre categoria real o busqueda sin categoria sin inventar inventarios`() {
        start()
        vm.openSuggestedProducts(2); assertEquals(SuggestedCatalog(1), vm.state.value.catalog)
        vm.catalogOpened(); assertNull(vm.state.value.catalog)
        vm.openSuggestedProducts(3); assertEquals(SuggestedCatalog(null), vm.state.value.catalog)
        verifyLoad(); confirmVerified(repository)
    }
    @Test fun `error de carga permite reintento`() {
        coEvery { repository.templates() } returns templates
        coEvery { repository.loadPlantings(7) } throws IOException()
        vm.start(7, "agricultor")
        assertNotNull(vm.state.value.error); assertFalse(vm.state.value.loading)
        coEvery { repository.loadPlantings(7) } returns PlantingSnapshot(emptyList())
        vm.refresh()
        assertNull(vm.state.value.error); assertTrue(vm.state.value.loaded)
        verifyLoad(2); confirmVerified(repository)
    }
    @Test fun `error de registro no crea siembra local ficticia`() {
        start(emptyList())
        val request = CreatePlantingRequest("maíz", "2026-10-15")
        coEvery { repository.registerPlanting(7, request) } throws IOException()
        vm.selectCrop("maíz"); vm.changeDate(request.date); vm.register()
        assertNotNull(vm.state.value.formError); assertTrue(vm.state.value.plantings.isEmpty())
        assertFalse(vm.state.value.saving)
        verifyLoad(); coVerify(exactly = 1) { repository.registerPlanting(7, request) }; confirmVerified(repository)
    }
    @Test fun `error al marcar no altera estado y libera boton`() {
        start()
        coEvery { repository.completeTask(7, 2, true) } throws IOException()
        vm.toggleTask(2)
        assertFalse(vm.state.value.tasks[1].task.completed)
        assertTrue(vm.state.value.pendingTasks.isEmpty()); assertNotNull(vm.state.value.error)
        verifyLoad(); coVerify(exactly = 1) { repository.completeTask(7, 2, true) }; confirmVerified(repository)
    }
    @Test fun `sin conexion no registra ni marca tareas`() {
        start(offline = true)
        vm.selectCrop("maíz"); vm.changeDate("2026-10-15"); vm.register(); vm.toggleTask(2)
        assertTrue(vm.state.value.offline)
        verifyLoad(); confirmVerified(repository)
    }
    @Test fun `cambio de usuario limpia calendario anterior y no permite distribuidores`() {
        start()
        coEvery { repository.loadPlantings(8) } returns PlantingSnapshot(emptyList())
        vm.start(8, "agricultor")
        assertTrue(vm.state.value.plantings.isEmpty())
        vm.start(8, "distribuidor")
        assertEquals(0, vm.state.value.ownerId)
        coVerify(exactly = 2) { repository.templates() }
        coVerify(exactly = 1) { repository.loadPlantings(7) }
        coVerify(exactly = 1) { repository.loadPlantings(8) }
        confirmVerified(repository)
    }
}
