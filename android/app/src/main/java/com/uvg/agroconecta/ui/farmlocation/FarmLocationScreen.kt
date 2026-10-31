package com.uvg.agroconecta.ui.farmlocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.GuatemalaLocations
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.theme.AgroSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun FarmLocationRoute(
    onNavigateBack: () -> Unit,
    viewModel: FarmLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.useCurrentLocation() else viewModel.onLocationPermissionDenied()
    }

    FarmLocationScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUseCurrentLocation = {
            if (context.hasLocationPermission()) viewModel.useCurrentLocation()
            else permissionLauncher.launch(locationPermissions)
        },
        onClearCoordinates = viewModel::clearCoordinates,
        onDepartamentoChange = viewModel::onDepartamentoChange,
        onMunicipioChange = viewModel::onMunicipioChange,
        onReferenciaChange = viewModel::onReferenciaChange,
        onSave = viewModel::save,
        onRetryLoad = viewModel::load,
        onSavedShown = viewModel::onSavedShown
    )
}

/**
 * Ubicación de la finca. El GPS va primero porque es lo más preciso y lo que
 * menos esfuerzo pide (un toque estando en la finca). Departamento, municipio
 * y referencia van después y sirven solos: quien no da permiso de ubicación
 * igual recibe su pedido y ve el clima de su municipio.
 */
@Composable
fun FarmLocationScreen(
    uiState: FarmLocationUiState,
    onNavigateBack: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    onClearCoordinates: () -> Unit,
    onDepartamentoChange: (String) -> Unit,
    onMunicipioChange: (String) -> Unit,
    onReferenciaChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryLoad: () -> Unit,
    onSavedShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberAgroHaptics()

    // Se confirma y se regresa solo: así queda claro que ya no hay nada más que hacer.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            haptics.confirm()
            onSavedShown()
            launch { snackbarHostState.showSnackbar("Listo, guardamos la ubicación de tu finca") }
            delay(BACK_AFTER_SAVE_MS)
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Ubicación de mi finca", onBack = onNavigateBack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                AgroPrimaryButton(
                    text = "Guardar ubicación",
                    loadingText = "Guardando…",
                    loading = uiState.isSaving,
                    enabled = uiState.canSave,
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.sm)
                )
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            SkeletonList(
                count = 2,
                description = "Cargando la ubicación de tu finca",
                modifier = Modifier
                    .padding(padding)
                    .padding(AgroSpacing.screenHorizontal)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.md)
        ) {
            WhyWeAsk()

            uiState.error?.let { InlineError(message = it, onRetry = onRetryLoad.takeIf { !uiState.isSaving }) }

            GpsCard(
                uiState = uiState,
                onUseCurrentLocation = onUseCurrentLocation,
                onClearCoordinates = onClearCoordinates
            )

            AgroCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(AgroSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    Text(
                        text = "¿En qué municipio está?",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    AgroDropdownField(
                        label = "Departamento",
                        options = GuatemalaLocations.departamentos,
                        selected = uiState.departamento.ifBlank { null },
                        onSelectedChange = onDepartamentoChange,
                        optionLabel = { it },
                        placeholder = "Elige tu departamento"
                    )
                    AgroDropdownField(
                        label = "Municipio",
                        options = uiState.municipios,
                        selected = uiState.municipio.ifBlank { null },
                        onSelectedChange = onMunicipioChange,
                        optionLabel = { it },
                        placeholder = if (uiState.departamento.isBlank()) "Primero elige el departamento" else "Elige tu municipio"
                    )
                    AgroTextField(
                        value = uiState.referencia,
                        onValueChange = onReferenciaChange,
                        label = "Cómo llegar (opcional)",
                        placeholder = "Aldea El Rosario, frente a la escuela",
                        supportingText = "Ayuda al distribuidor a encontrar tu finca.",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WhyWeAsk() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = "La usamos para el clima de tu finca, las alertas de plagas cercanas, " +
                "la distancia a los distribuidores y tu dirección de entrega.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GpsCard(
    uiState: FarmLocationUiState,
    onUseCurrentLocation: () -> Unit,
    onClearCoordinates: () -> Unit
) {
    val located = uiState.coordinates != null

    AgroCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (located) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            if (located) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                    modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column {
                        Text(
                            text = "Punto de tu finca marcado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Si no estabas en la finca, márcalo otra vez cuando estés allá.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                AgroSecondaryButton(
                    text = "Volver a marcar con mi ubicación",
                    icon = Icons.Default.MyLocation,
                    loadingText = "Buscando tu ubicación…",
                    loading = uiState.isLocating,
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                )
                AgroTextButton(text = "Quitar el punto", onClick = onClearCoordinates)
            } else {
                Text(
                    text = "Marca tu finca con el GPS",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Hazlo estando en tu finca: así el clima y las alertas son de ese lugar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AgroPrimaryButton(
                    text = "Usar mi ubicación actual",
                    icon = Icons.Default.MyLocation,
                    loadingText = "Buscando tu ubicación…",
                    loading = uiState.isLocating,
                    onClick = onUseCurrentLocation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            uiState.locationError?.let { InlineError(message = it) }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    locationPermissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// Lo justo para leer el aviso antes de volver al perfil.
private const val BACK_AFTER_SAVE_MS = 1_200L
