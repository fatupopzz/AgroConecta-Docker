package com.uvg.agroconecta.ui.calendar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.agroconecta.data.models.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CropCalendarScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `el resultado de guardar o borrar se muestra aunque la lista este desplazada`() {
        val date = LocalDate.of(2026, 10, 16)
        val task = PlantingTask(1, "Riego", "otro", date.toString(), null, false, nota = "Sector norte", custom = true)
        val tasks = (1..8).map { task.copy(id = it, nombre = "Riego $it") }
        val planting = Planting(1, "maíz", "Maíz", date.toString(), tasks)
        val state = mutableStateOf(CropCalendarState(ownerId = 7, loaded = true, connected = true,
            plantings = listOf(planting), tasks = tasks.map { CalendarTask(1, "Maíz", it, date, TaskPeriod.TODAY) }))
        compose.setContent { MaterialTheme {
            CropCalendarContent(state.value, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Riego 8"))
        compose.runOnIdle { state.value = state.value.copy(message = "Tarea agregada.") }
        compose.onNodeWithText("Tarea agregada.").assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Riego 8"))
        compose.runOnIdle { state.value = state.value.copy(message = null, error = "No se pudo borrar la tarea.") }
        compose.onNodeWithText("No se pudo borrar la tarea.").assertIsDisplayed()
    }
    @Test fun `hoy tiene etiqueta textual y permite abrir insumos`() {
        val task = PlantingTask(2, "Fertilizar", "fertilizar", "2026-10-16", PlantingCategory(1, "Fertilizantes"), false)
        val planting = Planting(1, "maíz", "Maíz", "2026-10-06", listOf(task))
        var opened = 0
        compose.setContent { MaterialTheme {
            CropCalendarContent(CropCalendarState(ownerId = 7, loaded = true, plantings = listOf(planting),
                tasks = listOf(CalendarTask(1, "Maíz", task, LocalDate.of(2026,10,16), TaskPeriod.TODAY))),
                {}, {}, {}, {}, {}, {}, { opened = it }, {}, {})
        } }
        compose.onNodeWithText("Hoy · viernes 16 de octubre").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver insumos en catálogo").performScrollTo().performClick()
        compose.onNodeWithText("Borrar tarea").assertDoesNotExist()
        assertEquals(2, opened)
    }
    @Test fun `estado vacio muestra instruccion de registro`() {
        compose.setContent { MaterialTheme {
            CropCalendarContent(CropCalendarState(ownerId = 7, loaded = true), {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Aún no tienes siembras. Registra la primera para ver sus tareas.").performScrollTo().assertIsDisplayed()
    }
}
