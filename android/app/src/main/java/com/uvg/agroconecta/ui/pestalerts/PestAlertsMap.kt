package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.ui.theme.AgroSpacing
import kotlin.math.max

internal data class PestMapBounds(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
)

internal data class NormalizedMapPoint(
    val x: Float,
    val y: Float
)

@Composable
fun PestAlertsMap(
    alerts: List<PestAlert>,
    currentLocation: PestAlertLocation?,
    errorMessage: String?,
    onRetry: () -> Unit,
    onAlertClick: (PestAlert) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = AgroSpacing.screenHorizontal, end = AgroSpacing.screenHorizontal, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mapa de alertas",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${alerts.size} ${if (alerts.size == 1) "marcador" else "marcadores"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MapLegend()
        }

        errorMessage?.let { message ->
            InlineAlertsError(message = message, onRetry = onRetry)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            PestMapCanvas(
                alerts = alerts,
                currentLocation = currentLocation,
                onAlertClick = onAlertClick
            )
        }

        Text(
            text = "Toca un marcador para ver la alerta y los productos sugeridos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PestMapCanvas(
    alerts: List<PestAlert>,
    currentLocation: PestAlertLocation?,
    onAlertClick: (PestAlert) -> Unit
) {
    val bounds = remember(alerts, currentLocation) {
        calculatePestMapBounds(alerts, currentLocation)
    }
    // 48 dp: el mínimo táctil, aunque en un mapa pequeño los marcadores se acerquen.
    val markerSize = 48.dp
    val mapBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val roadColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val markerBackground = MaterialTheme.colorScheme.surfaceContainerLowest
    val locationColor = MaterialTheme.colorScheme.tertiary
    val pestColor = MaterialTheme.colorScheme.error

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pest-alert-map")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(mapBackground)
            repeat(6) { index ->
                val fraction = index / 5f
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(size.width * fraction, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * fraction, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height * fraction),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height * fraction),
                    strokeWidth = 1.dp.toPx()
                )
            }
            drawLine(
                color = roadColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.32f),
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.72f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.32f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val usableWidth = (maxWidth - markerSize).coerceAtLeast(0.dp)
        val usableHeight = (maxHeight - markerSize).coerceAtLeast(0.dp)

        currentLocation?.let { location ->
            val point = normalizePestMapPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                bounds = bounds
            )
            Box(
                modifier = Modifier
                    .offset(
                        x = usableWidth * point.x,
                        y = usableHeight * point.y
                    )
                    .size(markerSize)
                    .testTag("current-location-marker")
                    .semantics { contentDescription = "Tu ubicación" }
                    .clip(CircleShape)
                    .background(markerBackground)
                    .border(3.dp, locationColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = locationColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        alerts.forEach { alert ->
            val point = normalizePestMapPoint(
                latitude = alert.latitud,
                longitude = alert.longitud,
                bounds = bounds
            )
            Surface(
                onClick = { onAlertClick(alert) },
                modifier = Modifier
                    .offset(
                        x = usableWidth * point.x,
                        y = usableHeight * point.y
                    )
                    .size(markerSize)
                    .testTag("pest-map-marker-${alert.id}")
                    .semantics {
                        contentDescription = "Alerta de ${pestTypeDisplayName(alert.pestType)} en ${alert.cultivo}"
                    },
                shape = CircleShape,
                color = markerBackground,
                shadowElevation = 4.dp,
                border = BorderStroke(3.dp, pestColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = pestColor,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapLegend() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AgroSpacing.xs, vertical = AgroSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Plaga activa",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

internal fun calculatePestMapBounds(
    alerts: List<PestAlert>,
    currentLocation: PestAlertLocation?
): PestMapBounds {
    val latitudes = alerts.map(PestAlert::latitud) + listOfNotNull(currentLocation?.latitude)
    val longitudes = alerts.map(PestAlert::longitud) + listOfNotNull(currentLocation?.longitude)

    val minLatitude = latitudes.minOrNull() ?: DEFAULT_MAP_LATITUDE
    val maxLatitude = latitudes.maxOrNull() ?: DEFAULT_MAP_LATITUDE
    val minLongitude = longitudes.minOrNull() ?: DEFAULT_MAP_LONGITUDE
    val maxLongitude = longitudes.maxOrNull() ?: DEFAULT_MAP_LONGITUDE
    val latitudePadding = max((maxLatitude - minLatitude) * MAP_PADDING_RATIO, MIN_MAP_SPAN)
    val longitudePadding = max((maxLongitude - minLongitude) * MAP_PADDING_RATIO, MIN_MAP_SPAN)

    return PestMapBounds(
        minLatitude = minLatitude - latitudePadding,
        maxLatitude = maxLatitude + latitudePadding,
        minLongitude = minLongitude - longitudePadding,
        maxLongitude = maxLongitude + longitudePadding
    )
}

internal fun normalizePestMapPoint(
    latitude: Double,
    longitude: Double,
    bounds: PestMapBounds
): NormalizedMapPoint {
    val longitudeSpan = (bounds.maxLongitude - bounds.minLongitude).coerceAtLeast(MIN_MAP_SPAN)
    val latitudeSpan = (bounds.maxLatitude - bounds.minLatitude).coerceAtLeast(MIN_MAP_SPAN)
    return NormalizedMapPoint(
        x = ((longitude - bounds.minLongitude) / longitudeSpan).toFloat().coerceIn(0f, 1f),
        y = (1.0 - (latitude - bounds.minLatitude) / latitudeSpan)
            .toFloat()
            .coerceIn(0f, 1f)
    )
}

private const val DEFAULT_MAP_LATITUDE = 14.6349
private const val DEFAULT_MAP_LONGITUDE = -90.5069
private const val MAP_PADDING_RATIO = 0.15
private const val MIN_MAP_SPAN = 0.005
