package com.uvg.agroconecta.ui.calendar

import com.uvg.agroconecta.data.connectivity.ConnectivityMonitor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvg.agroconecta.data.models.*
import com.uvg.agroconecta.data.repository.PlantingCalendarRepository
import com.uvg.agroconecta.data.repository.PlantingSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Las fechas de calendario del servidor nunca se convierten a instantes UTC.
enum class TaskPeriod(val label: String) { PAST("Pasada"), TODAY("Hoy"), UPCOMING("Próxima") }
data class CalendarTask(val plantingId: Int, val crop: String, val task: PlantingTask, val date: LocalDate, val period: TaskPeriod)
data class SuggestedCatalog(val categoryId: Int?)
data class TaskForm(val plantingId: Int, val name: String = "", val date: String = "", val note: String = "", val error: String? = null)
data class CropCalendarState(
    val ownerId: Int = 0,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val templates: List<PlantingTemplate> = emptyList(),
    val plantings: List<Planting> = emptyList(),
    val tasks: List<CalendarTask> = emptyList(),
    val crop: String = "",
    val date: String = "",
    val saving: Boolean = false,
    val pendingTasks: Set<Int> = emptySet(),
    val taskForm: TaskForm? = null,
    val deleteCandidate: PlantingTask? = null,
    val taskMutationPending: Boolean = false,
    val connected: Boolean = false,
    val offline: Boolean = false,
    val error: String? = null,
    val formError: String? = null,
    val message: String? = null,
    val catalog: SuggestedCatalog? = null
) {
    val canChangeTasks get() = ownerId > 0 && loaded && connected && !offline && !loading &&
        !saving && pendingTasks.isEmpty() && !taskMutationPending
}

@HiltViewModel
class CropCalendarViewModel @Inject constructor(
    private val repository: PlantingCalendarRepository,
    private val clock: Clock,
    private val connectivity: ConnectivityMonitor
) : ViewModel() {
    private val _state = MutableStateFlow(CropCalendarState())
    val state = _state.asStateFlow()

    fun start(userId: Int, userType: String) {
        val owner = userId.takeIf {
            it > 0 && UserRole.fromBackend(userType) == UserRole.FARMER
        } ?: 0
        if (_state.value.ownerId == owner) return
        viewModelScope.coroutineContext.cancelChildren()
        _state.value = CropCalendarState(ownerId = owner)
        if (owner > 0) {
            viewModelScope.launch { connectivity.status.collect { online -> _state.update { it.copy(connected = online) } } }
            refresh()
        }
    }

    fun refresh() {
        val state = _state.value
        if (state.taskMutationPending || state.ownerId <= 0 || state.loading || state.saving || state.pendingTasks.isNotEmpty()) return
        _state.update { it.copy(loading = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                try {
                    val templates = repository.templates()
                    _state.update { it.copy(templates = templates) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(error = "No se pudieron cargar los cultivos. Reintenta para registrar una siembra.") }
                }
                apply(repository.loadPlantings(state.ownerId))
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(loaded = false, error = "No se pudo cargar el calendario. Revisa tu conexión y reintenta.") }
            } finally { _state.update { if (it.ownerId == state.ownerId) it.copy(loading = false) else it } }
        }
    }

    private fun timeline(plantings: List<Planting>): List<CalendarTask> {
        val today = LocalDate.now(clock)
        return plantings.flatMap { planting -> planting.tasks.map { task ->
            val date = LocalDate.parse(task.date)
            CalendarTask(planting.id, planting.name, task, date, when {
                date < today -> TaskPeriod.PAST
                date == today -> TaskPeriod.TODAY
                else -> TaskPeriod.UPCOMING
            })
        } }.sortedWith(compareBy<CalendarTask> { it.date }.thenBy { it.task.id })
    }

    private fun apply(snapshot: PlantingSnapshot) {
        val tasks = timeline(snapshot.plantings)
        _state.update { it.copy(plantings = snapshot.plantings, tasks = tasks, loaded = true,
            offline = snapshot.offline, message = if (snapshot.cacheWarning)
                "Guardado en el servidor. No se pudo actualizar la copia local; vuelve a cargar con conexión." else it.message) }
    }

    fun refreshToday() { _state.update { it.copy(tasks = timeline(it.plantings)) } }
    fun selectCrop(crop: String) { if (!_state.value.saving) _state.update { it.copy(crop = crop, formError = null) } }
    fun changeDate(date: String) { if (!_state.value.saving) _state.update { it.copy(date = date, formError = null) } }

    private fun dateError(value: String): String? {
        val date = runCatching { LocalDate.parse(value) }.getOrNull()
        return when {
            date == null || !Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value) -> "Ingresa una fecha válida: AAAA-MM-DD."
            kotlin.math.abs(ChronoUnit.DAYS.between(LocalDate.now(clock), date)) > 365 -> "La fecha debe estar dentro de 365 días antes o después de hoy."
            else -> null
        }
    }

    fun register() {
        val state = _state.value
        if (state.taskMutationPending || state.ownerId <= 0 || state.saving || state.loading || state.offline || !state.loaded || state.pendingTasks.isNotEmpty()) return
        val error = if (state.templates.none { it.cultivo == state.crop }) "Selecciona un cultivo." else dateError(state.date)
        if (error != null) { _state.update { it.copy(formError = error) }; return }
        _state.update { it.copy(saving = true, formError = null, message = null) }
        viewModelScope.launch {
            try {
                apply(repository.registerPlanting(state.ownerId, CreatePlantingRequest(state.crop, state.date)))
                _state.update { it.copy(crop = "", date = "", message = it.message ?: "Siembra registrada.") }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(formError = "No se pudo confirmar el registro. Actualiza el calendario antes de reintentar para comprobar si se guardó.") }
            } finally { _state.update { if (it.ownerId == state.ownerId) it.copy(saving = false) else it } }
        }
    }

    fun toggleTask(taskId: Int) {
        val state = _state.value
        val task = state.tasks.find { it.task.id == taskId }?.task ?: return
        if (state.taskMutationPending || state.ownerId <= 0 || state.offline || !state.loaded || state.loading || state.saving || taskId in state.pendingTasks) return
        _state.update { it.copy(pendingTasks = it.pendingTasks + taskId, error = null, message = null) }
        viewModelScope.launch {
            try { apply(repository.completeTask(state.ownerId, taskId, !task.completed)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = "No se pudo guardar la tarea. Reintenta con conexión.") } }
            finally { _state.update { if (it.ownerId == state.ownerId) it.copy(pendingTasks = it.pendingTasks - taskId) else it } }
        }
    }

    fun openTaskForm() {
        val current = state.value
        if (!current.canChangeTasks || current.deleteCandidate != null) return
        val planting = current.plantings.firstOrNull() ?: return
        _state.update { it.copy(taskForm = TaskForm(planting.id), message = null, error = null) }
    }

    fun editTaskForm(form: TaskForm) {
        if (state.value.taskForm != null && !state.value.taskMutationPending) {
            _state.update { it.copy(taskForm = form.copy(error = null)) }
        }
    }

    fun closeTaskDialog() {
        if (!state.value.taskMutationPending) _state.update { it.copy(taskForm = null, deleteCandidate = null) }
    }

    fun createTask() {
        val current = state.value
        val form = current.taskForm ?: return
        if (!current.canChangeTasks) return
        val name = form.name.trim()
        val note = form.note.trim()
        val error = when {
            current.plantings.none { it.id == form.plantingId } -> "Selecciona una siembra."
            name.isBlank() || '\u0000' in name || name.codePointCount(0, name.length) > 120 -> "Ingresa un nombre de hasta 120 caracteres."
            '\u0000' in note || note.codePointCount(0, note.length) > 1000 -> "La nota admite hasta 1000 caracteres."
            else -> dateError(form.date)
        }
        if (error != null) {
            _state.update { it.copy(taskForm = form.copy(error = error)) }
            return
        }
        mutateTask(current.ownerId, "Tarea agregada.", "No se pudo confirmar la creación. Actualiza el calendario antes de reintentar.") {
            repository.createTask(current.ownerId, form.plantingId, CreatePlantingTaskRequest(name, form.date, note.ifEmpty { null }))
        }
    }

    fun requestDelete(taskId: Int) {
        val current = state.value
        if (!current.canChangeTasks || current.taskForm != null) return
        val task = current.tasks.find { it.task.id == taskId && it.task.custom } ?: return
        _state.update { it.copy(deleteCandidate = task.task, message = null, error = null) }
    }

    fun confirmDelete() {
        val current = state.value
        val task = current.deleteCandidate ?: return
        if (!current.canChangeTasks || current.tasks.none { it.task.id == task.id && it.task.custom }) return
        mutateTask(current.ownerId, "Tarea borrada.", "No se pudo confirmar el borrado. Actualiza el calendario antes de reintentar.") {
            repository.deleteTask(current.ownerId, task.id)
        }
    }

    private fun mutateTask(ownerId: Int, success: String, failure: String, operation: suspend () -> PlantingSnapshot) {
        _state.update { it.copy(taskMutationPending = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                apply(operation())
                _state.update { it.copy(taskForm = null, deleteCandidate = null, message = it.message ?: success) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { state ->
                    if (state.taskForm != null) state.copy(taskForm = state.taskForm.copy(error = failure))
                    else state.copy(deleteCandidate = null, error = failure)
                }
            } finally {
                _state.update { if (it.ownerId == ownerId) it.copy(taskMutationPending = false) else it }
            }
        }
    }

    fun openSuggestedProducts(taskId: Int) {
        val task = _state.value.tasks.find { it.task.id == taskId } ?: return
        _state.update { it.copy(catalog = SuggestedCatalog(task.task.category?.id?.takeIf { id -> id > 0 })) }
    }
    fun catalogOpened() { _state.update { it.copy(catalog = null) } }
}
