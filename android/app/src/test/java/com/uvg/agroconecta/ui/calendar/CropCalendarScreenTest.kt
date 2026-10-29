package com.uvg.agroconecta.ui.calendar

import androidx.compose.material3.MaterialTheme
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
    @Test fun `hoy tiene etiqueta textual y permite abrir insumos`() {
        val task = PlantingTask(2, "Fertilizar", "fertilizar", "2026-10-16", PlantingCategory(1, "Fertilizantes"), false)
        val planting = Planting(1, "maíz", "Maíz", "2026-10-06", listOf(task))
        var opened = 0
        compose.setContent { MaterialTheme {
            CropCalendarContent(CropCalendarState(ownerId = 7, loaded = true, plantings = listOf(planting),
                tasks = listOf(CalendarTask(1, "Maíz", task, LocalDate.of(2026,10,16), TaskPeriod.TODAY))),
                {}, {}, {}, {}, {}, {}, { opened = it })
        } }
        compose.onNodeWithText("Hoy · viernes 16 de octubre").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver insumos en catálogo").performScrollTo().performClick()
        assertEquals(2, opened)
    }
    @Test fun `estado vacio muestra instruccion de registro`() {
        compose.setContent { MaterialTheme {
            CropCalendarContent(CropCalendarState(ownerId = 7, loaded = true), {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Aún no tienes siembras. Registra la primera para ver sus tareas.").performScrollTo().assertIsDisplayed()
    }
}
