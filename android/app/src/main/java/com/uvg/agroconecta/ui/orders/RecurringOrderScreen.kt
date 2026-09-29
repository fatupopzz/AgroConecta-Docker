package com.uvg.agroconecta.ui.orders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.RecurringDates
import com.uvg.agroconecta.data.models.RecurringAction
import com.uvg.agroconecta.data.models.RecurringFrequency
import com.uvg.agroconecta.data.models.RecurringOrder
import com.uvg.agroconecta.data.models.RecurringStatus
import com.uvg.agroconecta.notifications.canPostRecurringNotifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringOrderScreen(
    state: RecurringListState,
    onRetry: () -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Int) -> Unit,
    onAction: (RecurringOrder, RecurringAction) -> Unit,
    onBack: () -> Unit,
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(context.canPostRecurringNotifications()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (it) onPermissionGranted()
    }
    var toCancel by remember { mutableStateOf<RecurringOrder?>(null) }

    toCancel?.let { order ->
        AlertDialog(
            onDismissRequest = { toCancel = null },
            title = { Text("Cancelar pedido recurrente") },
            text = { Text("El pedido #${order.id} no volverá a procesarse. ¿Deseas cancelarlo?") },
            confirmButton = {
                TextButton(onClick = { toCancel = null; onAction(order, RecurringAction.CANCEL) }) { Text("Cancelar pedido") }
            },
            dismissButton = { TextButton(onClick = { toCancel = null }) { Text("Volver") } }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Pedidos recurrentes") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
                Text("Crear desde mi carrito")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                Text("Los recordatorios están desactivados. Puedes gestionar tus pedidos igualmente.")
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text("Activar recordatorios")
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Reintentar") }
            }
            if (!state.loading && state.orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no tienes pedidos recurrentes.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.orders, key = { it.id }) { order ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Recurrencia #${order.id}", style = MaterialTheme.typography.titleMedium)
                                Text("Estado: ${order.status.label}")
                                Text("Frecuencia: ${order.frequency.label}")
                                Text("Próximo procesamiento: ${RecurringDates.display(order.nextAt)}")
                                val enabled = state.pendingId == null
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (order.status == RecurringStatus.ACTIVE) {
                                        TextButton(enabled = enabled, onClick = { onAction(order, RecurringAction.PAUSE) }) {
                                            Text("Pausar")
                                        }
                                    }
                                    if (order.status == RecurringStatus.PAUSED) {
                                        TextButton(enabled = enabled, onClick = { onAction(order, RecurringAction.RESUME) }) {
                                            Text("Reanudar")
                                        }
                                    }
                                    if (order.status != RecurringStatus.CANCELED) {
                                        TextButton(enabled = enabled, onClick = { onEdit(order.id) }) { Text("Editar") }
                                        TextButton(enabled = enabled, onClick = { toCancel = order }) { Text("Cancelar") }
                                    }
                                }
                                if (state.pendingId == order.id) LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringOrderFormScreen(
    state: RecurringFormState,
    submitting: Boolean,
    onFrequency: (RecurringFrequency) -> Unit,
    onAddress: (String) -> Unit,
    onNextDate: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    var frequencyMenu by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (state.editingId == null) "Crear pedido recurrente" else "Editar pedido recurrente") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("${state.products.size} producto(s) del ${if (state.source == "cart") "carrito" else "pedido"}")
            Box {
                OutlinedButton(onClick = { frequencyMenu = true }, enabled = !state.loading && !submitting) {
                    Text("Frecuencia: ${state.frequency.label}")
                }
                DropdownMenu(expanded = frequencyMenu, onDismissRequest = { frequencyMenu = false }) {
                    listOf(RecurringFrequency.WEEKLY, RecurringFrequency.BIWEEKLY, RecurringFrequency.MONTHLY)
                        .forEach { frequency ->
                            DropdownMenuItem(text = { Text(frequency.label) }, onClick = {
                                onFrequency(frequency)
                                frequencyMenu = false
                            })
                        }
                }
            }
            if (state.editingId == null) {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = onAddress,
                    label = { Text("Dirección de entrega") },
                    enabled = !state.loading && !submitting,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Dirección: ${state.address}")
            }
            OutlinedTextField(
                value = state.nextDate,
                onValueChange = onNextDate,
                label = { Text("Próxima fecha (aaaa-MM-dd HH:mm)") },
                enabled = !state.loading && !submitting,
                modifier = Modifier.fillMaxWidth()
            )
            Text("La fecha se interpreta en la zona horaria de este dispositivo.", style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = onSubmit,
                enabled = !state.loading && !submitting && state.products.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (submitting) "Guardando…" else "Guardar recurrencia") }
        }
    }
}
