package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.OrderTrackingChange
import com.uvg.agroconecta.data.models.OrderTrackingResponse
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.formatFriendlyDateTime
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing

private data class TrackingStep(val estado: String, val icon: ImageVector)

private val mainTrackingSteps = listOf(
    TrackingStep("confirmado", Icons.Default.CheckCircle),
    TrackingStep("preparando", Icons.Default.Inventory),
    TrackingStep("en_ruta", Icons.Default.LocalShipping),
    TrackingStep("entregado", Icons.Default.Home)
)

@Composable
fun OrderTrackingScreen(
    tracking: OrderTrackingResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenAdvice: (Int) -> Unit
) {
    // El ViewModel limpia el seguimiento al recargar. Se sigue mostrando el
    // anterior mientras llega el nuevo para que la pantalla no salte a siluetas
    // cada vez que alguien desliza para ver si ya cambió el estado.
    var lastTracking by remember { mutableStateOf<OrderTrackingResponse?>(null) }
    LaunchedEffect(tracking) {
        if (tracking != null) lastTracking = tracking
    }
    val shownTracking = tracking ?: lastTracking.takeIf { isLoading }

    Scaffold(
        topBar = { AgroTopAppBar(title = "Seguimiento del pedido", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && shownTracking == null -> SkeletonList(
                    count = 3,
                    description = "Cargando seguimiento",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                errorMessage != null -> RefreshableFullScreen {
                    ErrorState(
                        title = "No pudimos ver tu pedido",
                        message = errorMessage,
                        onRetry = onRetry
                    )
                }

                shownTracking != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                    ) {
                        item(key = "header") { TrackingHeader(tracking = shownTracking) }
                        item(key = "stepper") { TrackingStepper(currentEstado = shownTracking.estadoActual) }
                        if (shownTracking.cambios.isNotEmpty()) {
                            item(key = "history-title") {
                                Text(
                                    text = "Historial",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(top = AgroSpacing.xs)
                                        .semantics { heading() }
                                )
                            }
                            items(shownTracking.cambios) { change ->
                                TimelineItem(change = change)
                            }
                        }
                        item(key = "advice") {
                            AgroSecondaryButton(
                                text = "Pedir asesoría técnica",
                                icon = Icons.AutoMirrored.Filled.Chat,
                                onClick = { onOpenAdvice(shownTracking.idPedido) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingHeader(tracking: OrderTrackingResponse) {
    val currentStepIndex = mainTrackingSteps.indexOfFirst { it.estado == tracking.estadoActual }
        .coerceAtLeast(0)
    val progress = (currentStepIndex + 1) / mainTrackingSteps.size.toFloat()
    val isCanceled = tracking.estadoActual == "cancelado"

    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Box(
                    modifier = Modifier
                        .size(AgroSizes.minTouchTarget)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(AgroSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pedido #${tracking.idPedido}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusTag(
                        text = orderStatusLabel(tracking.estadoActual),
                        tone = orderStatusTone(tracking.estadoActual)
                    )
                }
            }

            Spacer(Modifier.height(AgroSpacing.md))

            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    text = "Entrega estimada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatFriendlyDateTime(tracking.tiempoEstimadoEntrega) ?: "Por confirmar",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isCanceled) {
                Spacer(Modifier.height(AgroSpacing.sm))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }
    }
}

@Composable
private fun TrackingStepper(currentEstado: String) {
    val currentIndex = mainTrackingSteps.indexOfFirst { it.estado == currentEstado }
    val isCanceled = currentEstado == "cancelado"

    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            Text(
                text = "Paso a paso",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(AgroSpacing.md))

            if (isCanceled) {
                InlineError(message = "Este pedido se canceló.")
            } else {
                mainTrackingSteps.forEachIndexed { index, step ->
                    val isDone = currentIndex >= index
                    val isCurrent = currentIndex == index

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StepDot(isDone = isDone, isCurrent = isCurrent, icon = step.icon)
                            if (index < mainTrackingSteps.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(32.dp)
                                        .background(
                                            if (currentIndex > index) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(AgroSpacing.sm))
                        Column(modifier = Modifier.padding(top = AgroSpacing.xxs)) {
                            Text(
                                text = orderStatusLabel(step.estado),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = when {
                                    isCurrent -> "Aquí va tu pedido"
                                    isDone -> "Listo"
                                    else -> "Falta"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Punto del paso a paso. El paso actual se marca con un anillo en vez del
 * pulso infinito de antes: se entiende igual y no obliga a redibujar la
 * pantalla sin parar.
 */
@Composable
private fun StepDot(
    isDone: Boolean,
    isCurrent: Boolean,
    icon: ImageVector
) {
    val ring = if (isCurrent) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(ring)
            .padding(if (isCurrent) 3.dp else 0.dp)
            .background(
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDone && !isCurrent) Icons.Default.Check else icon,
            contentDescription = null,
            tint = if (isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TimelineItem(change: OrderTrackingChange) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(AgroSpacing.md)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(AgroSizes.iconSmall)
                )
            }
            Spacer(Modifier.width(AgroSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = orderStatusLabel(change.estado),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatFriendlyDateTime(change.timestamp) ?: "Sin fecha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                change.notas?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(AgroSpacing.xxs))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
