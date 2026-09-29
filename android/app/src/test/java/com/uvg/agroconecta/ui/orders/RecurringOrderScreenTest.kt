package com.uvg.agroconecta.ui.orders

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.uvg.agroconecta.data.models.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecurringOrderScreenTest {
    @get:Rule val compose = createComposeRule()
    private val order = RecurringOrder(5, 17, RecurringFrequency.WEEKLY,
        listOf(RecurringProductDto(8, 2)), 3, "Parcela norte",
        Instant.parse("2030-10-01T12:00:00Z"), RecurringStatus.ACTIVE)

    @Test fun `renders loading empty error and populated list with actions`() {
        val state = mutableStateOf(RecurringListState(loading = true))
        compose.setContent {
            MaterialTheme {
                RecurringOrderScreen(state.value, {}, {}, {}, { _, _ -> }, {})
            }
        }
        compose.runOnIdle { state.value = RecurringListState() }
        compose.onNodeWithText("Aún no tienes pedidos recurrentes.").assertIsDisplayed()
        compose.runOnIdle { state.value = RecurringListState(error = "Sin conexión") }
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
        compose.runOnIdle { state.value = RecurringListState(orders = listOf(order)) }
        compose.onNodeWithText("Recurrencia #5").assertIsDisplayed()
        compose.onNodeWithText("Pausar").assertIsDisplayed()
        compose.onNodeWithText("Reanudar").assertDoesNotExist()
        compose.runOnIdle { state.value = RecurringListState(orders = listOf(order.copy(status = RecurringStatus.PAUSED))) }
        compose.onNodeWithText("Reanudar").assertIsDisplayed()
    }

    @Test fun `cancel requires confirmation and pending state disables actions`() {
        val state = mutableStateOf(RecurringListState(orders = listOf(order)))
        var action: RecurringAction? = null
        compose.setContent {
            MaterialTheme {
                RecurringOrderScreen(state.value, {}, {}, {}, { _, selected -> action = selected }, {})
            }
        }
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(null, action) }
        compose.onNodeWithText("Cancelar pedido").performClick()
        compose.runOnIdle { assertEquals(RecurringAction.CANCEL, action) }
        compose.runOnIdle { state.value = RecurringListState(orders = listOf(order), pendingId = 5) }
        compose.onNodeWithText("Pausar").assertIsNotEnabled()
    }

    @Test @Config(sdk = [33])
    fun `notification denial explains reminders without hiding recurrence actions`() {
        compose.setContent {
            MaterialTheme {
                RecurringOrderScreen(RecurringListState(orders = listOf(order)), {}, {}, {}, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Los recordatorios están desactivados. Puedes gestionar tus pedidos igualmente.")
            .assertIsDisplayed()
        compose.onNodeWithText("Pausar").assertIsDisplayed()
    }
}
