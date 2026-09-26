package com.uvg.agroconecta.ui.pestalerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    initialAlertId: Int? = null,
    onInitialAlertHandled: () -> Unit = {},
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    viewModel: PestAlertViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isLocationPermissionResolved by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isLocationPermissionResolved = true
        if (permissions.values.any { it }) {
            viewModel.refreshLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val requestOrRefreshLocation = {
        if (context.hasLocationPermission()) {
            isLocationPermissionResolved = true
            viewModel.refreshLocation()
        } else {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(Unit) {
        requestOrRefreshLocation()
    }

    LaunchedEffect(initialAlertId) {
        initialAlertId?.let { alertId ->
            viewModel.openAlert(alertId)
            onInitialAlertHandled()
        }
    }

    LaunchedEffect(isLocationPermissionResolved) {
        if (
            isLocationPermissionResolved &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasNotificationPermission()
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    PestAlertScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = requestOrRefreshLocation,
        onAlertClick = viewModel::selectAlert,
        onDismissAlertDetail = viewModel::dismissAlertDetail,
        onRetryAlertDetail = viewModel::retrySuggestedProducts,
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
        onDismissReportSuccess = viewModel::clearReportSuccess,
        showFavoriteAction = showFavoriteAction,
        favoriteIds = favoriteIds,
        pendingFavoriteIds = pendingFavoriteIds,
        favoriteErrorMessage = favoriteErrorMessage,
        onFavoriteClick = onFavoriteClick,
        onFavoriteErrorShown = onFavoriteErrorShown
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

private fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
