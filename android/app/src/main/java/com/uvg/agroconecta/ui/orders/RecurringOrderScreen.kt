package com.uvg.agroconecta.ui.orders

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.uvg.agroconecta.data.models.RecurringAction
import com.uvg.agroconecta.data.models.RecurringDates
import com.uvg.agroconecta.data.models.RecurringFrequency
import com.uvg.agroconecta.data.models.RecurringOrder
import com.uvg.agroconecta.data.models.RecurringStatus
import com.uvg.agroconecta.notifications.canPostRecurringNotifications
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.RadioOption
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors

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
            title = { Text("¿Dejar de repetir este pedido?") },
            text = {
                Text("El pedido #${order.id} ya no se hará solo. Esto no se puede deshacer; si cambias de opinión tendrás que crearlo de nuevo.")
            },
            confirmButton = {
                AgroTextButton(
                    text = "Cancelar pedido",
                    destructive = true,
                    onClick = {
                        toCancel = null
                        onAction(order, RecurringAction.CANCEL)
                    }
                )
            },
            dismissButton = {
                AgroTextButton(text = "Mantenerlo", onClick = { toCancel = null })
            }
        )
    }

    Scaffold(topBar = { AgroTopAppBar(title = "Pedidos recurrentes", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            item(key = "create") {
                AgroPrimaryButton(
                    text = "Crear desde mi carrito",
                    icon = Icons.Default.Add,
                    onClick = onCreateNew,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted) {
                item(key = "reminders") {
                    Surface(
                        color = MaterialTheme.agroColors.warningContainer,
                        contentColor = MaterialTheme.agroColors.onWarningContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.xs, top = AgroSpacing.sm)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null)
                                Text(
                                    text = "Los recordatorios están desactivados. Puedes gestionar tus pedidos igualmente.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = AgroSpacing.sm)
                                )
                            }
                            AgroTextButton(
                                text = "Activar recordatorios",
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            )
                        }
                    }
                }
            }

            when {
                state.loading && state.orders.isEmpty() -> item(key = "loading") {
                    SkeletonList(count = 2, description = "Cargando pedidos recurrentes")
                }

                state.error != null && state.orders.isEmpty() -> item(key = "error") {
                    ErrorState(message = state.error, onRetry = onRetry)
                }

                state.orders.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Default.Autorenew,
                        title = "Aún no tienes pedidos recurrentes.",
                        message = "Si compras lo mismo cada cierto tiempo, crea uno y se pedirá solo."
                    )
                }

                else -> {
                    if (state.loading) {
                        item(key = "refreshing") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    }
                    state.error?.let { error ->
                        item(key = "inline-error") { InlineError(message = error, onRetry = onRetry) }
                    }
                    items(state.orders, key = { it.id }) { order ->
                        RecurringOrderCard(
                            order = order,
                            actionsEnabled = state.pendingId == null,
                            isPending = state.pendingId == order.id,
                            onAction = { action -> onAction(order, action) },
                            onEdit = { onEdit(order.id) },
                            onCancel = { toCancel = order }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecurringOrderCard(
    order: RecurringOrder,
    actionsEnabled: Boolean,
    isPending: Boolean,
    onAction: (RecurringAction) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.md, top = AgroSpacing.md, bottom = AgroSpacing.xs)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recurrencia #${order.id}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    StatusTag(
                        text = order.status.label,
                        tone = when (order.status) {
                            RecurringStatus.ACTIVE -> TagTone.Success
                            RecurringStatus.PAUSED -> TagTone.Warning
                            RecurringStatus.CANCELED -> TagTone.Danger
                        }
                    )
                }
                Text(
                    text = "Se repite: ${order.frequency.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Próximo pedido: ${RecurringDates.display(order.nextAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isPending) {
                LinearProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = AgroSpacing.xs)
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xxs, Alignment.End)
            ) {
                if (order.status == RecurringStatus.ACTIVE) {
                    AgroTextButton(
                        text = "Pausar",
                        icon = Icons.Default.Pause,
                        enabled = actionsEnabled,
                        onClick = { onAction(RecurringAction.PAUSE) }
                    )
                }
                if (order.status == RecurringStatus.PAUSED) {
                    AgroTextButton(
                        text = "Reanudar",
                        icon = Icons.Default.PlayArrow,
                        enabled = actionsEnabled,
                        onClick = { onAction(RecurringAction.RESUME) }
                    )
                }
                if (order.status != RecurringStatus.CANCELED) {
                    AgroTextButton(text = "Editar", icon = Icons.Default.Edit, enabled = actionsEnabled, onClick = onEdit)
                    AgroTextButton(
                        text = "Cancelar",
                        icon = Icons.Default.Close,
                        destructive = true,
                        enabled = actionsEnabled,
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

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
    val editable = !state.loading && !submitting
    val productCount = state.products.size
    val origin = if (state.source == "cart") "del carrito" else "del pedido"

    Scaffold(topBar = {
        AgroTopAppBar(
            title = if (state.editingId == null) "Nuevo pedido recurrente" else "Editar pedido recurrente",
            onBack = onBack
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.md)
        ) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                text = if (productCount == 1) "1 producto $origin" else "$productCount productos $origin",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "¿Cada cuánto lo quieres?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                listOf(RecurringFrequency.WEEKLY, RecurringFrequency.BIWEEKLY, RecurringFrequency.MONTHLY)
                    .forEach { frequency ->
                        RadioOption(
                            label = frequency.label,
                            selected = state.frequency == frequency,
                            enabled = editable,
                            onClick = { onFrequency(frequency) }
                        )
                    }
            }

            if (state.editingId == null) {
                AgroTextField(
                    value = state.address,
                    onValueChange = onAddress,
                    label = "Dirección de entrega",
                    enabled = editable,
                    singleLine = false,
                    maxLines = 3
                )
            } else {
                Text(
                    text = "Dirección: ${state.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AgroTextField(
                value = state.nextDate,
                onValueChange = onNextDate,
                label = "Primera fecha (aaaa-MM-dd HH:mm)",
                supportingText = "Se usa la hora de este teléfono.",
                enabled = editable
            )
            state.error?.let { InlineError(message = it) }
            AgroPrimaryButton(
                text = "Guardar",
                loadingText = "Guardando…",
                onClick = onSubmit,
                enabled = !state.loading && state.products.isNotEmpty(),
                loading = submitting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
