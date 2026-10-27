package com.uvg.agroconecta.ui.pestalerts

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.uvg.agroconecta.ui.pestalerts.map.LocalPestMapHostFactory
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCameraState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapCoordinate
import com.uvg.agroconecta.ui.pestalerts.map.PestMapLoadState
import com.uvg.agroconecta.ui.pestalerts.map.PestMapTrackingMode
import com.uvg.agroconecta.ui.theme.GrayMid

@Composable
fun PestAlertsMap(
    currentLocation: PestAlertLocation?,
    errorMessage: String?,
    onRetry: () -> Unit
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
                color = GrayMid
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
            PestMapView(currentLocation = currentLocation)
        }

        Text(
            text = "Usa gestos de zoom y desplazamiento para explorar el mapa.",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMid
        )
    }
}

@Composable
private fun PestMapView(currentLocation: PestAlertLocation?) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val hostFactory = LocalPestMapHostFactory.current
    val savedMapViewState = rememberSaveable { Bundle() }
    var cameraState by rememberSaveable(stateSaver = PestMapCameraState.Saver) {
        mutableStateOf(PestMapCameraState.initial())
    }
    var loadState by remember { mutableStateOf<PestMapLoadState>(PestMapLoadState.Loading) }
    var trackingMode by rememberSaveable {
        mutableStateOf(PestMapTrackingMode.FOLLOWING_USER)
    }

    val mapHost = remember(context, hostFactory) {
        hostFactory.create(
            context = context,
            savedState = savedMapViewState,
            cameraState = cameraState,
            onLoadStateChanged = { loadState = it },
            onCameraChanged = { cameraState = it },
            onTrackingModeChanged = { trackingMode = it }
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

    DisposableEffect(mapHost, lifecycle) {
        mapHost.attach(lifecycle)
        onDispose {
            mapHost.saveState(savedMapViewState)
            mapHost.dispose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pest-alert-map")
    ) {
        AndroidView(
            factory = { mapHost.view },
            modifier = Modifier.fillMaxSize()
        )

        if (loadState == PestMapLoadState.Ready) {
            FilledTonalIconButton(
                onClick = {
                    trackingMode = PestMapTrackingMode.FOLLOWING_USER
                    mapHost.centerOnUser()
                },
                enabled = currentLocation != null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .testTag("pest-map-my-location")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mi ubicación"
                )
            }
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
                }
            }
        }
    }
}
