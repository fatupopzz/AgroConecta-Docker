package com.uvg.agroconecta.ui.pestalerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun PestAlertRoute(
    onNavigateBack: () -> Unit,
    viewModel: PestAlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.refreshLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val requestOrRefreshLocation = {
        if (context.hasLocationPermission()) {
            viewModel.refreshLocation()
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(Unit) {
        requestOrRefreshLocation()
    }

    PestAlertScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = requestOrRefreshLocation,
        onAlertClick = viewModel::selectAlert,
        onViewModeChanged = viewModel::setViewMode,
        onReportPest = {
            viewModel.openReportForm()
            if (uiState.location == null && !uiState.isLocating) {
                requestOrRefreshLocation()
            }
        },
        onDismissReportForm = viewModel::dismissReportForm,
        onReportPestTypeSelected = viewModel::selectReportPestType,
        onReportCropSelected = viewModel::selectReportCrop,
        onReportDescriptionChanged = viewModel::updateReportDescription,
        onSubmitReport = viewModel::submitPestReport,
        onDismissReportSuccess = viewModel::clearReportSuccess
    )
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
