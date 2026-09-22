package com.uvg.agroconecta.ui.pestalerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PestAlertScreen(
    uiState: PestAlertUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onAlertClick: (PestAlert) -> Unit,
    onReportPest: () -> Unit,
    onDismissReportForm: () -> Unit = {},
    onReportPestTypeSelected: (PestType) -> Unit = {},
    onReportCropSelected: (String) -> Unit = {},
    onReportDescriptionChanged: (String) -> Unit = {},
    onSubmitReport: () -> Unit = {},
    onDismissReportSuccess: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas de plagas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportPest,
                modifier = Modifier.testTag("report-pest"),
                icon = {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null
                    )
                },
                text = { Text("Reportar plaga") },
                containerColor = GreenPrimary,
                contentColor = Color.White
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (uiState.isLoadingAlerts && uiState.alerts.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.reportSuccessMessage?.let { message ->
                ReportSuccessBanner(
                    message = message,
                    onDismiss = onDismissReportSuccess
                )
            }

            when {
                uiState.isLoadingAlerts && uiState.alerts.isEmpty() -> {
                    LoadingAlertsState()
                }

                uiState.alertsErrorMessage != null && uiState.alerts.isEmpty() -> {
                    AlertsErrorState(
                        message = uiState.alertsErrorMessage,
                        onRetry = onRetry
                    )
                }

                uiState.alerts.isEmpty() -> {
                    EmptyAlertsState()
                }

                else -> {
                    AlertsList(
                        alerts = uiState.alerts,
                        errorMessage = uiState.alertsErrorMessage,
                        onRetry = onRetry,
                        onAlertClick = onAlertClick
                    )
                }
            }
        }
    }

    if (uiState.isReportFormVisible) {
        PestAlertReportDialog(
            formState = uiState.reportForm,
            location = uiState.location,
            isSubmitting = uiState.isSubmittingReport,
            errorMessage = uiState.reportErrorMessage,
            onDismiss = onDismissReportForm,
            onPestTypeSelected = onReportPestTypeSelected,
            onCropSelected = onReportCropSelected,
            onDescriptionChanged = onReportDescriptionChanged,
            onSubmit = onSubmitReport
        )
    }
}

@Composable
private fun ReportSuccessBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = GreenSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = GreenPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    }
}

@Composable
private fun AlertsList(
    alerts: List<PestAlert>,
    errorMessage: String?,
    onRetry: () -> Unit,
    onAlertClick: (PestAlert) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 20.dp,
            end = 16.dp,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Alertas activas cerca de ti",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${alerts.size} ${if (alerts.size == 1) "reporte cercano" else "reportes cercanos"}",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMid
            )
        }

        errorMessage?.let { message ->
            item {
                InlineAlertsError(message = message, onRetry = onRetry)
            }
        }

        items(items = alerts, key = PestAlert::id) { alert ->
            PestAlertCard(
                alert = alert,
                onClick = { onAlertClick(alert) }
            )
        }
    }
}

@Composable
private fun PestAlertCard(
    alert: PestAlert,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pest-alert-${alert.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = GreenSurface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = pestTypeDisplayName(alert.pestType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cultivo: ${alert.cultivo}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayMid
                        )
                    }
                }
                SeverityBadge(alert.severidad)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AlertMetadata(
                    icon = Icons.Default.LocationOn,
                    contentDescription = "Distancia",
                    value = formatDistance(alert.distanceKm)
                )
                AlertMetadata(
                    icon = Icons.Default.CalendarToday,
                    contentDescription = "Fecha del reporte",
                    value = formatAlertDate(alert.reportedAt)
                )
            }

            Text(
                text = "Ver detalle y productos preventivos",
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SeverityBadge(severity: String) {
    val normalized = severity.trim().lowercase(Locale.ROOT)
    val (label, background, foreground) = when (normalized) {
        "critica" -> Triple("Crítica", Color(0xFFFFE7E7), ErrorRed)
        "alta" -> Triple("Alta", Color(0xFFFFEDE5), Color(0xFFB54708))
        "baja" -> Triple("Baja", GreenSurface, GreenPrimary)
        else -> Triple("Media", Color(0xFFFFF4D6), Color(0xFF8A6100))
    }
    Surface(color = background, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AlertMetadata(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GrayMid,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = GrayMid
        )
    }
}

@Composable
private fun LoadingAlertsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GreenPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Buscando alertas cercanas…", color = GrayMid)
        }
    }
}

@Composable
private fun AlertsErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = message,
                color = ErrorRed,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Reintentar", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InlineAlertsError(message: String, onRetry: () -> Unit) {
    Surface(
        color = Color(0xFFFFEDEA),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun EmptyAlertsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(52.dp)
            )
            Text(
                text = "No hay alertas activas cerca",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tu zona no tiene reportes recientes de plagas.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMid
            )
        }
    }
}

internal fun pestTypeDisplayName(apiValue: String): String =
    PestType.entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) }
        ?.displayName
        ?: apiValue
            .replace('_', ' ')
            .trim()
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(Locale("es", "GT")) else character.toString()
            }

internal fun formatDistance(distanceKm: Double): String = when {
    distanceKm < 1.0 -> "${(distanceKm.coerceAtLeast(0.0) * 1_000).roundToInt()} m"
    else -> String.format(Locale.US, "%.1f km", distanceKm)
}

internal fun formatAlertDate(value: String): String = runCatching {
    LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrElse { value }
