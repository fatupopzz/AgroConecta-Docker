package com.uvg.agroconecta.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var choosingCrop by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Calendario de cultivo") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.ownerId <= 0) {
                item { Text("Inicia sesión como agricultor para ver tus siembras.") }
            } else {
                item {
                    TextButton(onClick = onRefresh, enabled = !state.loading && !state.saving && state.pendingTasks.isEmpty()) {
                        Text("Actualizar calendario")
                    }
                }
                if (state.loading) item { CircularProgressIndicator() }
                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                state.message?.let { item { Text(it) } }
                if (state.offline) item { Text("Sin conexión: copia guardada. Conéctate para registrar o modificar tareas.") }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Registrar siembra", style = MaterialTheme.typography.titleMedium)
                            Box {
                                OutlinedButton(onClick = { choosingCrop = true }, enabled = !state.saving && state.templates.isNotEmpty()) {
                                    Text(state.templates.find { it.cultivo == state.crop }?.nombre ?: "Seleccionar cultivo")
                                }
                                DropdownMenu(expanded = choosingCrop, onDismissRequest = { choosingCrop = false }) {
                                    state.templates.forEach { template -> DropdownMenuItem(text = { Text(template.nombre) }, onClick = {
                                        onCrop(template.cultivo); choosingCrop = false
                                    }) }
                                }
                            }
                            state.templates.find { it.cultivo == state.crop }?.let { Text(it.descripcion) }
                            OutlinedTextField(value = state.date, onValueChange = onDate, label = { Text("Fecha de siembra (AAAA-MM-DD)") },
                                singleLine = true, enabled = !state.saving, modifier = Modifier.fillMaxWidth())
                            state.formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Button(onClick = onRegister, enabled = state.loaded && !state.saving && !state.loading && !state.offline && state.pendingTasks.isEmpty()) {
                                Text(if (state.saving) "Guardando…" else "Registrar siembra")
                            }
                        }
                    }
                }
                if (state.loaded && state.plantings.isEmpty()) item { Text("Aún no tienes siembras. Registra la primera para ver sus tareas.") }
                if (state.loaded && state.plantings.isNotEmpty() && state.tasks.isEmpty()) item { Text("Tus siembras todavía no tienen tareas.") }
                items(state.tasks, key = { it.task.id }) { row ->
                    Card(colors = CardDefaults.cardColors(containerColor = if (row.period == TaskPeriod.TODAY)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (row.period == TaskPeriod.TODAY) "Hoy · ${row.date}" else "${row.period.label} · ${row.date}", fontWeight = FontWeight.Bold)
                            Text(row.task.nombre, style = MaterialTheme.typography.titleMedium)
                            Text("${row.crop} · Siembra #${row.plantingId}")
                            Row {
                                Checkbox(checked = row.task.completed, onCheckedChange = { onToggle(row.task.id) },
                                    enabled = state.loaded && !state.offline && !state.loading && !state.saving && row.task.id !in state.pendingTasks,
                                    modifier = Modifier.semantics { contentDescription = "${if (row.task.completed) "Desmarcar" else "Marcar realizada"}: ${row.task.nombre}" })
                                Text(if (row.task.completed) "Realizada" else "Pendiente", Modifier.padding(top = 12.dp))
                            }
                            Text(row.task.category?.let { "Insumos sugeridos: ${it.nombre}. Elige producto y distribuidor en el catálogo." }
                                ?: "Sin insumos específicos. Busca un producto y elige distribuidor para agregarlo al carrito.")
                            TextButton(onClick = { onProducts(row.task.id) }) { Text("Ver insumos en catálogo") }
                        }
                    }
                }
            }
        }
    }
}
