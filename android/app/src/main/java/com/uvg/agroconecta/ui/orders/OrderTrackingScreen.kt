package com.uvg.agroconecta.ui.orders

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvg.agroconecta.data.models.OrderTrackingChange
import com.uvg.agroconecta.data.models.OrderTrackingResponse
import com.uvg.agroconecta.ui.theme.*

private data class TrackingStep(val estado: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val mainTrackingSteps = listOf(
    TrackingStep("confirmado", "Confirmado", Icons.Default.CheckCircle),
    TrackingStep("preparando", "Preparando", Icons.Default.Inventory),
    TrackingStep("en_ruta", "En ruta", Icons.Default.LocalShipping),
    TrackingStep("entregado", "Entregado", Icons.Default.Home)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    tracking: OrderTrackingResponse?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Seguimiento del pedido",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
        containerColor = GrayLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GreenPrimary
                    )
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = GrayBorder,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = GrayMid,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                tracking != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { TrackingHeader(tracking = tracking) }
                        item { TrackingStepper(currentEstado = tracking.estadoActual) }
                        if (tracking.cambios.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Historial",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GrayDark,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(tracking.cambios) { change ->
                                TimelineItem(change = change)
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(GreenSurface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Pedido #${tracking.idPedido}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrayDark
                    )
                    Text(
                        text = tracking.estadoActual.toDisplayStatus(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCanceled) MaterialTheme.colorScheme.error else GreenPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Entrega estimada",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMid
                    )
                    Text(
                        tracking.tiempoEstimadoEntrega.toFriendlyDateTime(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GrayDark
                    )
                }
            }

            if (!isCanceled) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = GreenPrimary,
                    trackColor = GreenPale
                )
            }
        }
    }
}

@Composable
private fun TrackingStepper(currentEstado: String) {
    val currentIndex = mainTrackingSteps.indexOfFirst { it.estado == currentEstado }
    val isCanceled = currentEstado == "cancelado"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Progreso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GrayDark
            )
            Spacer(Modifier.height(16.dp))

            if (isCanceled) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Pedido cancelado",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                mainTrackingSteps.forEachIndexed { index, step ->
                    val isDone = currentIndex >= index
                    val isCurrent = currentIndex == index

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StepDot(isDone = isDone, isCurrent = isCurrent, icon = step.icon)
                            if (index < mainTrackingSteps.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(36.dp)
                                        .background(
                                            if (currentIndex > index) GreenPrimary else GrayLight
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = step.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) GrayDark else GrayMid
                            )
                            Text(
                                text = when {
                                    isCurrent -> "Estado actual"
                                    isDone -> "Completado"
                                    else -> "Pendiente"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isCurrent -> GreenPrimary
                                    isDone -> GrayMid
                                    else -> GrayBorder
                                }
                            )
                            if (index < mainTrackingSteps.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDot(
    isDone: Boolean,
    isCurrent: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val transition = rememberInfiniteTransition(label = "trackingPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "currentStateScale"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(if (isCurrent) scale else 1f)
            .background(
                color = when {
                    isDone -> GreenPrimary
                    else -> GrayLight
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.Check else icon,
            contentDescription = null,
            tint = if (isDone) Color.White else GrayBorder,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun TimelineItem(change: OrderTrackingChange) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(GreenSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.estado.toDisplayStatus(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GrayDark
                )
                Text(
                    text = change.timestamp.toFriendlyDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayMid
                )
                change.notas?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayDark
                    )
                }
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
    if (isNullOrBlank()) return "Pendiente"
    return take(16).replace("T", " ")
}