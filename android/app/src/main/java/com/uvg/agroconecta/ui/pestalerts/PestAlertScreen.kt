package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.diagnosis.PlantDiseaseLabels
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestType
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PestAlertScreen(
    uiState: PestAlertUiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    onAlertClick: (PestAlert) -> Unit,
    onReportPest: () -> Unit,
    onDismissAlertDetail: () -> Unit = {},
    onRetryAlertDetail: () -> Unit = {},
    onViewModeChanged: (PestAlertViewMode) -> Unit = {},
    onDismissReportForm: () -> Unit = {},
    onReportPestTypeSelected: (PestType) -> Unit = {},
    onReportCropSelected: (String) -> Unit = {},
    onReportDescriptionChanged: (String) -> Unit = {},
    onSubmitReport: () -> Unit = {},
    onDismissReportSuccess: () -> Unit = {},
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    onDiagnoseLeafClick: (() -> Unit)? = null
) {
    val isLoadingLocationOrAlerts = uiState.isLocating || uiState.isLoadingAlerts
    val primaryErrorMessage = uiState.locationErrorMessage ?: uiState.alertsErrorMessage
    val snackbarHostState = remember { SnackbarHostState() }

    val haptics = rememberAgroHaptics()
    LaunchedEffect(uiState.reportSuccessMessage) {
        if (uiState.reportSuccessMessage != null) haptics.confirm()
    }

    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AgroTopAppBar(
                title = "Alertas de plagas",
                onBack = onNavigateBack,
                actions = {
                    // Junto al reporte manual: quien ve una hoja enferma pero no
                    // sabe qué plaga es puede diagnosticarla con una foto.
                    onDiagnoseLeafClick?.let { onClick ->
                        TextButton(
                            onClick = onClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.agroColors.onTopBar),
                            modifier = Modifier.testTag("diagnose-leaf")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(AgroSpacing.xs))
                            Text("Diagnosticar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportPest,
                modifier = Modifier.testTag("report-pest"),
                icon = { Icon(imageVector = Icons.Default.BugReport, contentDescription = null) },
                text = { Text("Reportar plaga", style = MaterialTheme.typography.labelLarge) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (isLoadingLocationOrAlerts && uiState.alerts.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.reportSuccessMessage?.let { message ->
                ReportSuccessBanner(message = message, onDismiss = onDismissReportSuccess)
            }

            PestAlertViewSelector(
                selectedMode = uiState.viewMode,
                onModeSelected = onViewModeChanged
            )

            // En el mapa el gesto se apaga: arrastrar hacia abajo debe mover el
            // mapa, no recargar.
            AgroPullToRefreshBox(
                isRefreshing = isLoadingLocationOrAlerts,
                onRefresh = onRetry,
                enabled = uiState.viewMode != PestAlertViewMode.MAP,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoadingLocationOrAlerts && uiState.alerts.isEmpty() -> {
                        SkeletonList(
                            count = 3,
                            description = "Buscando alertas cercanas",
                            modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                        )
                    }

                    primaryErrorMessage != null && uiState.alerts.isEmpty() -> {
                        RefreshableFullScreen {
                            ErrorState(
                                title = "No pudimos ver las alertas",
                                message = primaryErrorMessage,
                                onRetry = onRetry
                            )
                        }
                    }

                    uiState.alerts.isEmpty() -> {
                        RefreshableFullScreen {
                            EmptyState(
                                icon = Icons.Default.CheckCircle,
                                title = "No hay alertas activas cerca",
                                message = "Tu zona no tiene reportes recientes de plagas."
                            )
                        }
                    }

                    uiState.viewMode == PestAlertViewMode.MAP -> {
                        PestAlertsMap(
                            alerts = uiState.alerts,
                            currentLocation = uiState.location,
                            errorMessage = primaryErrorMessage,
                            onRetry = onRetry,
                            onAlertClick = onAlertClick
                        )
                    }

                    else -> {
                        AlertsList(
                            alerts = uiState.alerts,
                            errorMessage = primaryErrorMessage,
                            onRetry = onRetry,
                            onAlertClick = onAlertClick
                        )
                    }
                }
            }
        }
    }

    if (uiState.isReportFormVisible) {
        PestAlertReportDialog(
            formState = uiState.reportForm,
            location = uiState.location,
            isSubmitting = uiState.isSubmittingReport,
            errorMessage = uiState.reportErrorMessage ?: uiState.locationErrorMessage,
            onDismiss = onDismissReportForm,
            onPestTypeSelected = onReportPestTypeSelected,
            onCropSelected = onReportCropSelected,
            onDescriptionChanged = onReportDescriptionChanged,
            onSubmit = onSubmitReport
        )
    }

    uiState.selectedAlert?.let { alert ->
        PestAlertDetailSheet(
            alert = alert,
            products = uiState.suggestedProducts,
            isLoadingProducts = uiState.isLoadingSuggestions,
            errorMessage = uiState.detailErrorMessage,
            onRetry = onRetryAlertDetail,
            onDismiss = onDismissAlertDetail,
            showFavoriteAction = showFavoriteAction,
            favoriteIds = favoriteIds,
            pendingFavoriteIds = pendingFavoriteIds,
            onFavoriteClick = onFavoriteClick
        )
    }
}

/**
 * Selector de lista o mapa. Botones segmentados en lugar de dos chips sueltos:
 * se entiende que es una sola elección entre dos vistas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PestAlertViewSelector(
    selectedMode: PestAlertViewMode,
    onModeSelected: (PestAlertViewMode) -> Unit
) {
    val options = listOf(
        Triple(PestAlertViewMode.LIST, "Lista", Icons.AutoMirrored.Filled.List),
        Triple(PestAlertViewMode.MAP, "Mapa", Icons.Default.Map)
    )
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.xs)
    ) {
        options.forEachIndexed { index, (mode, label, icon) ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selectedMode == mode) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                label = { Text(label, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
}

@Composable
private fun ReportSuccessBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AgroSpacing.sm, vertical = AgroSpacing.xs)
            )
            AgroTextButton(text = "Cerrar", onClick = onDismiss)
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
        // Espacio inferior extra para que el botón flotante no tape la última alerta.
        contentPadding = PaddingValues(
            start = AgroSpacing.screenHorizontal,
            top = AgroSpacing.md,
            end = AgroSpacing.screenHorizontal,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        item(key = "title") {
            Column {
                Text(
                    text = "Alertas activas cerca de ti",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "${alerts.size} ${if (alerts.size == 1) "reporte cercano" else "reportes cercanos"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        errorMessage?.let { message ->
            item(key = "error") { InlineAlertsError(message = message, onRetry = onRetry) }
        }

        items(items = alerts, key = PestAlert::id) { alert ->
            PestAlertCard(alert = alert, onClick = { onAlertClick(alert) })
        }
    }
}

@Composable
private fun PestAlertCard(
    alert: PestAlert,
    onClick: () -> Unit
) {
    AgroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pest-alert-${alert.id}")
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.padding(AgroSpacing.xs)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pestTypeDisplayName(alert.pestType),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cultivo: ${alert.cultivo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SeverityBadge(alert.severidad)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.lg)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ver qué hacer y productos para prevenir",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Severidad con palabra y tono. La palabra va siempre: "Crítica" se tiene que
 * entender aunque no se distinga el rojo bajo el sol.
 */
@Composable
private fun SeverityBadge(severity: String) {
    val (label, tone) = when (severity.trim().lowercase(Locale.ROOT)) {
        "critica" -> "Crítica" to TagTone.Danger
        "alta" -> "Alta" to TagTone.Warning
        "baja" -> "Baja" to TagTone.Success
        else -> "Media" to TagTone.Neutral
    }
    StatusTag(text = label, tone = tone)
}

@Composable
private fun AlertMetadata(
    icon: ImageVector,
    contentDescription: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(AgroSizes.iconSmall)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun InlineAlertsError(message: String, onRetry: () -> Unit) {
    InlineError(message = message, onRetry = onRetry)
}

internal fun pestTypeDisplayName(apiValue: String): String =
    // Las alertas publicadas desde un diagnóstico (KAN-110) traen la etiqueta
    // del modelo, por ejemplo "Tomato___Late_blight", en lugar de un PestType.
    if (apiValue.contains("___")) PlantDiseaseLabels.describe(apiValue).disease
    else PestType.entries.firstOrNull { it.apiValue.equals(apiValue, ignoreCase = true) }
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
