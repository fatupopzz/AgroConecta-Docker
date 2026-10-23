package com.uvg.agroconecta.ui.diagnosis

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import kotlinx.coroutines.flow.first
import java.io.File

private enum class ImageSource { CAMERA, GALLERY }

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun PlantDiagnosisRoute(
    onNavigateBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: PlantDiagnosisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.start(
            userId = SessionManager.getUserId(context).first() ?: 0,
            farmerId = SessionManager.getFarmerId(context).first() ?: 0
        )
    }

    // Se guardan como texto para sobrevivir si el sistema recrea la Activity
    // mientras la cámara está abierta, que en teléfonos con poca memoria pasa.
    var cameraPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var sourceAfterPermission by rememberSaveable { mutableStateOf<String?>(null) }
    var locationAsked by rememberSaveable { mutableStateOf(false) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) viewModel.onImageSelected(cameraPhotoUri?.let(Uri::parse))
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onImageSelected(uri)
    }

    val openSource: (ImageSource) -> Unit = { source ->
        when (source) {
            ImageSource.CAMERA -> {
                val uri = context.newDiagnosisPhotoUri()
                cameraPhotoUri = uri.toString()
                try {
                    takePicture.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            ImageSource.GALLERY -> pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // Diga lo que diga el agricultor, después se abre la cámara o la galería:
    // sin ubicación el diagnóstico funciona igual, solo no se puede avisar a
    // los vecinos.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        sourceAfterPermission?.let { openSource(ImageSource.valueOf(it)) }
        sourceAfterPermission = null
    }

    // La ubicación se pide al tocar el botón y no al abrir la pantalla. Tiene
    // que quedar guardada junto con el diagnóstico, porque el backend la toma
    // de ahí para publicar la alerta.
    val onChooseImage: (ImageSource) -> Unit = { source ->
        if (!locationAsked && !context.hasLocationPermission()) {
            locationAsked = true
            sourceAfterPermission = source.name
            locationPermissionLauncher.launch(locationPermissions)
        } else {
            openSource(source)
        }
    }

    PlantDiagnosisScreen(
        uiState = uiState,
        message = message,
        onNavigateBack = onNavigateBack,
        onTakePhoto = { onChooseImage(ImageSource.CAMERA) },
        onPickFromGallery = { onChooseImage(ImageSource.GALLERY) },
        onTakeAnotherPhoto = viewModel::takeAnotherPhoto,
        onRetryProducts = viewModel::retryProducts,
        onAddToCart = viewModel::addToCart,
        onProductClick = onProductClick,
        onAlertNeighbors = viewModel::alertNeighbors,
        onMessageShown = viewModel::clearMessage
    )
}

private fun Context.newDiagnosisPhotoUri(): Uri {
    val directory = File(cacheDir, "diagnosis").apply { mkdirs() }
    // Siempre el mismo archivo: la foto solo sirve hasta que se lee para el
    // modelo, y así la caché no acumula una por diagnóstico.
    val photo = File(directory, "hoja.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", photo)
}

private fun Context.hasLocationPermission(): Boolean =
    locationPermissions.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
