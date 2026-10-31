package com.uvg.agroconecta.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CropCalendarScreen(viewModel: CropCalendarViewModel, onBack: () -> Unit, onCatalog: (Int?) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.catalog) {
        state.catalog?.let { onCatalog(it.categoryId); viewModel.catalogOpened() }
    }
    LaunchedEffect(viewModel) {
        while (true) { viewModel.refreshToday(); delay(60_000) }
    }
    CropCalendarContent(state, onBack, viewModel::refresh, viewModel::selectCrop,
        viewModel::changeDate, viewModel::register, viewModel::toggleTask, viewModel::openSuggestedProducts)
}

/**
 * Calendario de cultivo: registrar una siembra y seguir sus tareas.
 *
 * Las tareas van primero porque son lo que se consulta a diario; registrar
 * una siembra pasa pocas veces al año y queda al final.
 *
 * Las tareas de hoy van resaltadas y con la palabra "Hoy" escrita, no solo con
 * color, porque a pleno sol los tonos se confunden. Marcar una tarea se puede
 * con toda la fila y no solo con la casilla: un cuadrito de 20 dp es difícil
 * de atinar con las manos ocupadas en el campo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CropCalendarContent(
    state: CropCalendarState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCrop: (String) -> Unit,
    onDate: (String) -> Unit,
    onRegister: () -> Unit,
    onToggle: (Int) -> Unit,
    onProducts: (Int) -> Unit
) {
    // El ViewModel ignora recargas mientras guarda o marca tareas; el botón lo
    // refleja para que no parezca que no responde.
    val canRefresh = !state.loading && !state.saving && state.pendingTasks.isEmpty()
    val canEditTasks = state.loaded && !state.offline && !state.loading && !state.saving

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = "Calendario de cultivo", onBack = onBack) }
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            enabled = state.ownerId > 0 && canRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.ownerId <= 0 -> RefreshableFullScreen {
                    EmptyState(
                        icon = Icons.Default.CalendarMonth,
                        title = "Calendario para agricultores",
                        message = "Inicia sesión como agricultor para ver tus siembras."
                    )
                }

                state.loading && !state.loaded -> SkeletonList(
                    count = 3,
                    description = "Cargando tu calendario",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    state.error?.let { error ->
                        item(key = "error") { InlineError(message = error, onRetry = onRefresh.takeIf { canRefresh }) }
                    }
                    state.message?.let { message ->
                        item(key = "message") {
                            CalendarNotice(
                                icon = Icons.Default.Info,
                                text = message,
                                container = MaterialTheme.colorScheme.primaryContainer,
                                content = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (state.offline) {
                        item(key = "offline") {
                            CalendarNotice(
                                icon = Icons.Default.WifiOff,
                                text = "Sin conexión: ves la copia guardada. Conéctate para registrar siembras o marcar tareas.",
                                container = MaterialTheme.agroColors.warningContainer,
                                content = MaterialTheme.agroColors.onWarningContainer
                            )
                        }
                    }
                    item(key = "tasks-header") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tus tareas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { heading() }
                            )
                            // TalkBack no puede deslizar para recargar; este botón
                            // deja hacerlo igual.
                            AgroTextButton(
                                text = "Actualizar",
                                icon = Icons.Default.Refresh,
                                onClick = onRefresh,
                                enabled = canRefresh
                            )
                        }
                    }
                    if (state.loaded && state.plantings.isEmpty()) {
                        item(key = "no-plantings") {
                            CalendarHint("Aún no tienes siembras. Registra la primera para ver sus tareas.")
                        }
                    }
                    if (state.loaded && state.plantings.isNotEmpty() && state.tasks.isEmpty()) {
                        item(key = "no-tasks") { CalendarHint("Tus siembras todavía no tienen tareas.") }
                    }
                    items(state.tasks, key = { it.task.id }) { row ->
                        TaskCard(
                            row = row,
                            enabled = canEditTasks && row.task.id !in state.pendingTasks,
                            onToggle = { onToggle(row.task.id) },
                            onProducts = { onProducts(row.task.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                    item(key = "register") {
                        RegisterPlantingCard(
                            state = state,
                            onCrop = onCrop,
                            onDate = onDate,
                            onRegister = onRegister
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterPlantingCard(
    state: CropCalendarState,
    onCrop: (String) -> Unit,
    onDate: (String) -> Unit,
    onRegister: () -> Unit
) {
    val selectedTemplate = state.templates.find { it.cultivo == state.crop }

    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Text(
                text = "Registrar siembra",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            AgroDropdownField(
                label = "Cultivo",
                options = state.templates,
                selected = selectedTemplate,
                onSelectedChange = { onCrop(it.cultivo) },
                optionLabel = { it.nombre },
                placeholder = if (state.templates.isEmpty()) "Cargando cultivos…" else "Elige qué sembraste",
                supportingText = selectedTemplate?.descripcion,
                enabled = !state.saving
            )
            PlantingDateField(
                isoDate = state.date,
                onDate = onDate,
                errorMessage = state.formError,
                enabled = !state.saving
            )
            AgroPrimaryButton(
                text = "Registrar siembra",
                loadingText = "Guardando…",
                loading = state.saving,
                onClick = onRegister,
                enabled = state.loaded && !state.loading && !state.offline && state.pendingTasks.isEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Fecha de siembra con un calendario en vez de escribirla. Antes había que
 * teclear "AAAA-MM-DD", y casi todos la escribían como día/mes/año y el
 * formulario la rechazaba. El ViewModel sigue recibiendo el mismo formato ISO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantingDateField(
    isoDate: String,
    onDate: (String) -> Unit,
    errorMessage: String?,
    enabled: Boolean
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val selected = remember(isoDate) { runCatching { LocalDate.parse(isoDate) }.getOrNull() }

    Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
        Text(
            text = "Fecha de siembra",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AgroSecondaryButton(
            text = selected?.let { friendlyCalendarDate(it) } ?: "Elegir fecha",
            icon = Icons.Default.CalendarMonth,
            onClick = { choosing = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = selected?.let { "Fecha de siembra: ${friendlyCalendarDate(it)}. Toca para cambiarla" }
                        ?: "Elegir fecha de siembra"
                }
        )
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }

    if (choosing) {
        val today = remember { LocalDate.now() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selected ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            // El backend acepta hasta un año antes o después; así no se puede
            // elegir una fecha que después rechace.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return kotlin.math.abs(ChronoUnit.DAYS.between(today, day)) <= PLANTING_DATE_RANGE_DAYS
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { choosing = false },
            confirmButton = {
                AgroTextButton(
                    text = "Listo",
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        }
                        choosing = false
                    }
                )
            },
            dismissButton = { AgroTextButton(text = "Cancelar", onClick = { choosing = false }) }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

private const val PLANTING_DATE_RANGE_DAYS = 365L

private val calendarDateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "GT"))

internal fun friendlyCalendarDate(date: LocalDate): String = date.format(calendarDateFormatter)

@Composable
private fun TaskCard(
    row: CalendarTask,
    enabled: Boolean,
    onToggle: () -> Unit,
    onProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberAgroHaptics()
    val isToday = row.period == TaskPeriod.TODAY

    // Hoy se marca con borde y no con fondo verde: sobre fondo verde la
    // etiqueta "Hoy", que también es verde, dejaba de verse.
    AgroCard(
        modifier = modifier.fillMaxWidth(),
        border = if (isToday) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            StatusTag(
                text = "${row.period.label} · ${friendlyCalendarDate(row.date)}",
                tone = when (row.period) {
                    TaskPeriod.TODAY -> TagTone.Success
                    TaskPeriod.PAST -> TagTone.Warning
                    TaskPeriod.UPCOMING -> TagTone.Info
                }
            )
            Text(
                text = row.task.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${row.crop} · Siembra #${row.plantingId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AgroSizes.minTouchTarget)
                    .toggleable(
                        value = row.task.completed,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = {
                            haptics.tick()
                            onToggle()
                        }
                    )
            ) {
                Checkbox(checked = row.task.completed, onCheckedChange = null, enabled = enabled)
                Text(
                    text = if (row.task.completed) "Realizada" else "Marcar como realizada",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = AgroSpacing.sm)
                )
            }
            Text(
                text = row.task.category?.let { "Insumos sugeridos: ${it.nombre}. Elige producto y distribuidor en el catálogo." }
                    ?: "Sin insumos específicos. Busca un producto y elige distribuidor para agregarlo al carrito.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AgroTextButton(
                text = "Ver insumos en catálogo",
                icon = Icons.Default.ShoppingBasket,
                onClick = onProducts
            )
        }
    }
}

@Composable
private fun CalendarNotice(icon: ImageVector, text: String, container: Color, content: Color) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CalendarHint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
        modifier = Modifier.padding(vertical = AgroSpacing.xs)
    ) {
        Icon(
            imageVector = Icons.Outlined.Agriculture,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
