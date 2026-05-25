package com.uvg.agroconecta.ui.orders

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.OrderTrackingChange
import com.uvg.agroconecta.data.models.OrderTrackingResponse

private data class TrackingStep(
    val estado: String,
    val label: String
)

private val mainTrackingSteps = listOf(
    TrackingStep("confirmado", "Confirmado"),
    TrackingStep("preparando", "Preparando"),
    TrackingStep("en_ruta", "En ruta"),
    TrackingStep("entregado", "Entregado")
)

@Composable
fun OrderTrackingScreen(
    tracking: OrderTrackingResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Seguimiento del pedido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("Reintentar")
                        }
                    }
                }

                tracking != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            TrackingHeader(tracking = tracking)
                        }

                        item {
                            TrackingStepper(currentEstado = tracking.estadoActual)
                        }

                        item {
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(tracking.cambios) { change ->
                            TimelineItem(change = change)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Regresar")
        }
    }
}

@Composable
private fun TrackingHeader(tracking: OrderTrackingResponse) {
    val currentStepIndex = mainTrackingSteps.indexOfFirst { it.estado == tracking.estadoActual }
        .coerceAtLeast(0)
    val progress = (currentStepIndex + 1) / mainTrackingSteps.size.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pedido #${tracking.idPedido}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Estado actual: ${tracking.estadoActual.toDisplayStatus()}")
            Text("Entrega estimada: ${tracking.tiempoEstimadoEntrega.toFriendlyDateTime()}")

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TrackingStepper(currentEstado: String) {
    val currentIndex = mainTrackingSteps.indexOfFirst { it.estado == currentEstado }
    val isCanceled = currentEstado == "cancelado"

    Column {
        Text(
            text = "Progreso",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        mainTrackingSteps.forEachIndexed { index, step ->
            val isDone = !isCanceled && currentIndex >= index
            val isCurrent = !isCanceled && currentIndex == index

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StepDot(isDone = isDone, isCurrent = isCurrent)
                    if (index < mainTrackingSteps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(34.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = if (isCurrent) "Estado actual" else if (isDone) "Completado" else "Pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isCanceled) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Pedido cancelado",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StepDot(isDone: Boolean, isCurrent: Boolean) {
    val transition = rememberInfiniteTransition(label = "trackingPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "currentStateScale"
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(if (isCurrent) scale else 1f)
            .background(
                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TimelineItem(change: OrderTrackingChange) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = change.estado.toDisplayStatus(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = change.timestamp.toFriendlyDateTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            change.notas?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(notes)
            }
        }
    }
}

private fun String.toDisplayStatus(): String =
    when (this) {
        "confirmado" -> "Confirmado"
        "preparando" -> "Preparando"
        "en_ruta" -> "En ruta"
        "entregado" -> "Entregado"
        "cancelado" -> "Cancelado"
        else -> replace("_", " ").replaceFirstChar { it.uppercase() }
    }

private fun String?.toFriendlyDateTime(): String {
    if (isNullOrBlank()) {
        return "Pendiente"
    }

    return take(16).replace("T", " ")
}
