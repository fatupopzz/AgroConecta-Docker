package com.uvg.agroconecta.ui.pestalerts

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.ui.pestalerts.map.LocalPestMapHostFactory
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCameraState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCoordinate
import com.uvg.agroconecta.ui.pestalerts.map.PestMapLoadState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapSearchRadius
import com.uvg.agroconecta.ui.pestalerts.map.PestMapTrackingMode
import com.uvg.agroconecta.ui.pestalerts.map.toPestMapMarker

@Composable
fun PestAlertsMap(
    currentLocation: PestAlertLocation?,
    alerts: List<PestAlert>,
    searchArea: PestMapSearchArea?,
    errorMessage: String?,
    onRetry: () -> Unit,
    onAlertClick: (PestAlert) -> Unit,
    selectedAlertId: Int?,
    onSelectedAlertIdChanged: (Int?) -> Unit,
    showSearchAreaAction: Boolean,
    showEmptySearchResult: Boolean,
    isSearchingArea: Boolean,
    onCameraChanged: (Double, Double) -> Unit,
    trackingMode: PestMapTrackingMode,
    onTrackingModeChanged: (PestMapTrackingMode) -> Unit,
    onCenterOnUser: () -> Unit,
    onSearchVisibleArea: () -> Unit,
    selectedRadiusKm: Double,
    onSearchRadiusSelected: (Double) -> Unit,
    isSelectingReportLocation: Boolean = false,
    reportLocationDraft: PestAlertLocation? = null,
    onConfirmReportLocation: () -> Unit = {},
    onCancelReportLocation: () -> Unit = {},
    onUseCurrentLocationForReport: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(
                text = "Mapa de alertas",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Desplázate y acerca el mapa para explorar otras zonas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        errorMessage?.let { message ->
            InlineAlertsError(message = message, onRetry = onRetry)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = Color(0xFFF4F1E8),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            PestMapView(
                currentLocation = currentLocation,
                alerts = alerts,
                searchArea = searchArea,
                onAlertClick = onAlertClick,
                selectedAlertId = selectedAlertId,
                onSelectedAlertIdChanged = onSelectedAlertIdChanged,
                showSearchAreaAction = showSearchAreaAction,
                showEmptySearchResult = showEmptySearchResult,
                isSearchingArea = isSearchingArea,
                onCameraChanged = onCameraChanged,
                trackingMode = trackingMode,
                onTrackingModeChanged = onTrackingModeChanged,
                onCenterOnUser = onCenterOnUser,
                onSearchVisibleArea = onSearchVisibleArea,
                selectedRadiusKm = selectedRadiusKm,
                onSearchRadiusSelected = onSearchRadiusSelected,
                isSelectingReportLocation = isSelectingReportLocation,
                reportLocationDraft = reportLocationDraft,
                onConfirmReportLocation = onConfirmReportLocation,
                onCancelReportLocation = onCancelReportLocation,
                onUseCurrentLocationForReport = onUseCurrentLocationForReport
            )
        }

        Text(
            text = "Usa gestos de zoom y desplazamiento para explorar el mapa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PestMapView(
    currentLocation: PestAlertLocation?,
    alerts: List<PestAlert>,
    searchArea: PestMapSearchArea?,
    onAlertClick: (PestAlert) -> Unit,
    selectedAlertId: Int?,
    onSelectedAlertIdChanged: (Int?) -> Unit,
    showSearchAreaAction: Boolean,
    showEmptySearchResult: Boolean,
    isSearchingArea: Boolean,
    onCameraChanged: (Double, Double) -> Unit,
    trackingMode: PestMapTrackingMode,
    onTrackingModeChanged: (PestMapTrackingMode) -> Unit,
    onCenterOnUser: () -> Unit,
    onSearchVisibleArea: () -> Unit,
    selectedRadiusKm: Double,
    onSearchRadiusSelected: (Double) -> Unit,
    isSelectingReportLocation: Boolean,
    reportLocationDraft: PestAlertLocation?,
    onConfirmReportLocation: () -> Unit,
    onCancelReportLocation: () -> Unit,
    onUseCurrentLocationForReport: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val hostFactory = LocalPestMapHostFactory.current
    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)
    val currentOnTrackingModeChanged by rememberUpdatedState(onTrackingModeChanged)
    val currentOnSelectedAlertIdChanged by rememberUpdatedState(onSelectedAlertIdChanged)
    val savedMapViewState = rememberSaveable { Bundle() }
    var cameraState by rememberSaveable(stateSaver = PestMapCameraState.Saver) {
        mutableStateOf(PestMapCameraState.initial())
    }
    var loadState by remember { mutableStateOf<PestMapLoadState>(PestMapLoadState.Loading) }
    val alertsById = remember(alerts) { alerts.associateBy(PestAlert::id) }
    val pestMarkers = remember(alerts) {
        alerts.mapNotNull(PestAlert::toPestMapMarker)
    }

    val mapHost = remember(context, hostFactory) {
        hostFactory.create(
            context = context,
            savedState = savedMapViewState,
            cameraState = cameraState,
            onLoadStateChanged = { loadState = it },
            onCameraChanged = { updatedCamera ->
                cameraState = updatedCamera
                currentOnCameraChanged(updatedCamera.latitude, updatedCamera.longitude)
            },
            onTrackingModeChanged = { mode -> currentOnTrackingModeChanged(mode) }
        )
    }

    LaunchedEffect(mapHost, currentLocation) {
        mapHost.updateUserLocation(
            location = currentLocation?.let {
                PestMapCoordinate(latitude = it.latitude, longitude = it.longitude)
            },
            centerCamera = trackingMode == PestMapTrackingMode.FOLLOWING_USER
        )
    }

    LaunchedEffect(mapHost, searchArea) {
        mapHost.updateSearchRadius(
            searchArea?.let { area ->
                PestMapSearchRadius(
                    center = PestMapCoordinate(
                        latitude = area.centerLatitude,
                        longitude = area.centerLongitude
                    ),
                    radiusKm = area.radiusKm
                )
            }
        )
    }

    LaunchedEffect(mapHost, pestMarkers, selectedAlertId) {
        mapHost.updatePestMarkers(
            markers = pestMarkers,
            selectedAlertId = selectedAlertId,
            onMarkerSelected = { alertId -> currentOnSelectedAlertIdChanged(alertId) },
            onSelectionCleared = { currentOnSelectedAlertIdChanged(null) }
        )
    }

    LaunchedEffect(mapHost, isSelectingReportLocation) {
        if (isSelectingReportLocation) {
            reportLocationDraft?.let { location ->
                mapHost.centerOnCoordinate(
                    PestMapCoordinate(location.latitude, location.longitude)
                )
            }
        }
    }

    DisposableEffect(mapHost, lifecycle) {
        mapHost.attach(lifecycle)
        onDispose {
            mapHost.saveState(savedMapViewState)
            mapHost.dispose()
        }
    }

    val selectedAlert = selectedAlertId?.let(alertsById::get)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pest-alert-map")
    ) {
        AndroidView(
            factory = { mapHost.view },
            modifier = Modifier.fillMaxSize()
        )

        if (loadState == PestMapLoadState.Ready && !isSelectingReportLocation) {
            SearchRadiusSelector(
                selectedRadiusKm = selectedRadiusKm,
                enabled = !isSearchingArea,
                onRadiusSelected = onSearchRadiusSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (
                trackingMode == PestMapTrackingMode.FREE_EXPLORATION &&
                showSearchAreaAction
            ) {
                Button(
                    onClick = onSearchVisibleArea,
                    enabled = !isSearchingArea,
                    modifier = Modifier
                        .zIndex(1f)
                        .align(Alignment.TopCenter)
                        .padding(top = 64.dp)
                        .testTag("pest-map-search-area")
                ) {
                    Text(
                        if (isSearchingArea) "Buscando…"
                        else "Buscar plagas en esta zona"
                    )
                }
            }

            FilledTonalIconButton(
                onClick = {
                    onCenterOnUser()
                    mapHost.centerOnUser()
                },
                enabled = currentLocation != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = if (selectedAlert == null) 16.dp else 116.dp
                    )
                    .size(48.dp)
                    .testTag("pest-map-my-location")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mi ubicación"
                )
            }
        }

        selectedAlert?.takeUnless { isSelectingReportLocation }?.let { alert ->
            PestMarkerSummary(
                alert = alert,
                onViewDetails = { onAlertClick(alert) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        if (
            loadState == PestMapLoadState.Ready &&
            showEmptySearchResult &&
            !isSelectingReportLocation
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 72.dp, bottom = 12.dp)
                    .testTag("pest-map-empty-result"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 3.dp,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "No se encontraron plagas en esta zona",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        if (loadState == PestMapLoadState.Ready && isSelectingReportLocation) {
            ReportLocationPickerOverlay(
                location = reportLocationDraft,
                canUseCurrentLocation = currentLocation != null,
                onUseCurrentLocation = {
                    onUseCurrentLocationForReport()
                    onCenterOnUser()
                    mapHost.centerOnUser()
                },
                onConfirm = onConfirmReportLocation,
                onCancel = onCancelReportLocation,
                modifier = Modifier.fillMaxSize()
            )
        }

        when (val state = loadState) {
            PestMapLoadState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            PestMapLoadState.Ready -> Unit

            is PestMapLoadState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = mapHost::retryStyle,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Reintentar mapa")
                    }
                    if (isSelectingReportLocation) {
                        TextButton(
                            onClick = onCancelReportLocation,
                            modifier = Modifier.testTag("cancel-report-location-on-map-error")
                        ) {
                            Text("Cancelar selección")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportLocationPickerOverlay(
    location: PestAlertLocation?,
    canUseCurrentLocation: Boolean,
    onUseCurrentLocation: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.testTag("report-location-picker")) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 4.dp,
            shadowElevation = 3.dp
        ) {
            Text(
                text = "Mueve el mapa hasta colocar el pin donde observaste la plaga.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Ubicación seleccionada",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .testTag("report-location-pin")
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 4.dp,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = location?.let(::formatReportCoordinates)
                        ?: "Esperando la posición del mapa…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar")
                    }
                    TextButton(
                        onClick = onUseCurrentLocation,
                        enabled = canUseCurrentLocation,
                        modifier = Modifier.testTag("report-location-use-gps")
                    ) {
                        Text("Mi GPS")
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = location != null,
                        modifier = Modifier.testTag("report-location-confirm")
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRadiusSelector(
    selectedRadiusKm: Double,
    enabled: Boolean,
    onRadiusSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pestMapSearchRadiusOptionsKm.forEach { radiusKm ->
                FilterChip(
                    selected = selectedRadiusKm == radiusKm,
                    onClick = { onRadiusSelected(radiusKm) },
                    enabled = enabled,
                    label = { Text("${radiusKm.toInt()} km") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pest-map-radius-${radiusKm.toInt()}")
                )
            }
        }
    }
}

@Composable
private fun PestMarkerSummary(
    alert: PestAlert,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("pest-map-selected-alert"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = pestTypeDisplayName(alert.pestType),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.testTag("pest-map-view-details")
                ) {
                    Text("Ver detalles")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Cultivo: ${alert.cultivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Distancia: ${formatDistance(alert.distanceKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fecha: ${formatAlertDate(alert.reportedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = summarizeAlertDescription(alert.descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, end = 8.dp)
            )
        }
    }
}

internal fun summarizeAlertDescription(
    description: String?,
    maxLength: Int = 120
): String {
    val normalized = description
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        .orEmpty()
    if (normalized.isEmpty()) return "Sin descripción adicional."
    if (normalized.length <= maxLength) return normalized

    return normalized
        .take((maxLength - 1).coerceAtLeast(0))
        .trimEnd()
        .plus("…")
}
