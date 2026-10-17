package com.uvg.agroconecta.ui.calendar

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
    val offline: Boolean = false,
    val error: String? = null,
    val formError: String? = null,
    val message: String? = null,
    val catalog: SuggestedCatalog? = null
)

@HiltViewModel
class CropCalendarViewModel @Inject constructor(
    private val repository: PlantingCalendarRepository,
    private val clock: Clock
) : ViewModel() {
    private val _state = MutableStateFlow(CropCalendarState())
    val state = _state.asStateFlow()

    fun start(userId: Int, userType: String) {
        val owner = userId.takeIf { it > 0 && userType == "agricultor" } ?: 0
        if (_state.value.ownerId == owner) return
        viewModelScope.coroutineContext.cancelChildren()
        _state.value = CropCalendarState(ownerId = owner)
        if (owner > 0) refresh()
    }

    fun refresh() {
        val state = _state.value
        if (state.ownerId <= 0 || state.loading || state.saving || state.pendingTasks.isNotEmpty()) return
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

    fun register() {
        val state = _state.value
        if (state.ownerId <= 0 || state.saving || state.loading || state.offline || !state.loaded || state.pendingTasks.isNotEmpty()) return
        val date = runCatching { LocalDate.parse(state.date) }.getOrNull()
        val error = when {
            state.templates.none { it.cultivo == state.crop } -> "Selecciona un cultivo."
            !Regex("\\d{4}-\\d{2}-\\d{2}").matches(state.date) || date == null -> "Ingresa una fecha válida: AAAA-MM-DD."
            kotlin.math.abs(ChronoUnit.DAYS.between(LocalDate.now(clock), date)) > 365 -> "La fecha debe estar dentro de 365 días antes o después de hoy."
            else -> null
        }
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
        if (state.ownerId <= 0 || state.offline || !state.loaded || state.loading || state.saving || taskId in state.pendingTasks) return
        _state.update { it.copy(pendingTasks = it.pendingTasks + taskId, error = null, message = null) }
        viewModelScope.launch {
            try { apply(repository.completeTask(state.ownerId, taskId, !task.completed)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = "No se pudo guardar la tarea. Reintenta con conexión.") } }
            finally { _state.update { if (it.ownerId == state.ownerId) it.copy(pendingTasks = it.pendingTasks - taskId) else it } }
        }
    }

    fun openSuggestedProducts(taskId: Int) {
        val task = _state.value.tasks.find { it.task.id == taskId } ?: return
        _state.update { it.copy(catalog = SuggestedCatalog(task.task.category?.id?.takeIf { id -> id > 0 })) }
    }
    fun catalogOpened() { _state.update { it.copy(catalog = null) } }
}
