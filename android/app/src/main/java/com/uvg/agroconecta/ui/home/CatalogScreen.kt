package com.uvg.agroconecta.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.uvg.agroconecta.ui.home.voice.AndroidVoiceSearchAvailability
import com.uvg.agroconecta.ui.home.voice.AndroidVoiceSearchRecognizerSession
import com.uvg.agroconecta.ui.home.voice.VoiceSearchRecognitionListener
import com.uvg.agroconecta.ui.home.voice.VoiceSearchRecognizerManager
import com.uvg.agroconecta.ui.home.voice.VoiceSearchPermissionAction
import com.uvg.agroconecta.ui.home.voice.voiceSearchPermissionAction

private val CatalogGreen = Color(0xFF2D6A1F)
private val CatalogBackground = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    initialQuery: String,
    initialCategoryId: Int? = null,
    calendarContext: Boolean = false,
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    onVoiceSearchClick: () -> Unit = {},
    voiceSearchAvailable: Boolean = rememberVoiceSearchAvailable(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val voiceRecognitionListener = remember { VoiceSearchRecognitionListener() }
    val voiceRecognizerManager = remember(context, voiceRecognitionListener) {
        VoiceSearchRecognizerManager {
            AndroidVoiceSearchRecognizerSession(
                recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext),
                recognitionListener = voiceRecognitionListener
            )
        }
    }
    val startVoiceRecognition = {
        voiceRecognizerManager.startListening()
        onVoiceSearchClick()
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceRecognition()
    }

    DisposableEffect(voiceRecognizerManager) {
        onDispose(voiceRecognizerManager::destroy)
    }

    LaunchedEffect(initialQuery, initialCategoryId) {
        viewModel.applyCatalogFilter(initialQuery, initialCategoryId)
    }

    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    Scaffold(
        containerColor = CatalogBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de productos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isOffline) item { Text("Sin conexión: solo se muestran coincidencias que se puedan verificar en la copia guardada.") }
            if (calendarContext) item { Text("Elige un producto y luego un distribuidor. La sugerencia es una categoría; todavía no hay un inventario seleccionado.") }
            uiState.errorMessage?.let { message -> item {
                Text(message)
                TextButton(onClick = viewModel::onSearchSubmit) { Text("Reintentar búsqueda") }
            } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (uiState.searchQuery.isBlank()) {
                            if (initialCategoryId != null) "Productos de la categoría sugerida" else "Todos los productos"
                        } else {
                            "Resultados para “${uiState.searchQuery}”"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CatalogGreen
                    )
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar en el catálogo") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            CatalogSearchActions(
                                voiceSearchAvailable = voiceSearchAvailable,
                                onVoiceSearchClick = {
                                    keyboardController?.hide()
                                    val permissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    when (voiceSearchPermissionAction(permissionGranted)) {
                                        VoiceSearchPermissionAction.START_RECOGNITION ->
                                            startVoiceRecognition()
                                        VoiceSearchPermissionAction.REQUEST_PERMISSION ->
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onSearchClick = {
                                    keyboardController?.hide()
                                    viewModel.onSearchSubmit()
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.onSearchSubmit()
                        })
                    )
                }
            }

            if (uiState.isLoadingProductos && uiState.productos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CatalogGreen)
                    }
                }
            } else if (uiState.productos.isEmpty()) {
                item {
                    Text(
                        text = "No encontramos productos con este filtro.",
                        color = Color(0xFF757575),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(uiState.productos.chunked(2)) { products ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        products.forEach { product ->
                            ProductCard(
                                producto = product,
                                onClick = { onProductClick(product.id) },
                                showFavoriteAction = showFavoriteAction,
                                isFavorite = product.id in favoriteIds,
                                isUpdatingFavorite = product.id in pendingFavoriteIds,
                                onFavoriteClick = { onFavoriteClick(product.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (products.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (uiState.hasMore && uiState.productos.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { viewModel.loadProductos() },
                        enabled = !uiState.isLoadingProductos,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isLoadingProductos) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                                color = CatalogGreen
                            )
                        } else {
                            Text("Cargar más productos", color = CatalogGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberVoiceSearchAvailable(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        AndroidVoiceSearchAvailability(context).isAvailable()
    }
}

@Composable
internal fun CatalogSearchActions(
    voiceSearchAvailable: Boolean,
    onVoiceSearchClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Row {
        if (voiceSearchAvailable) {
            IconButton(onClick = onVoiceSearchClick) {
                Icon(Icons.Default.Mic, contentDescription = "Buscar por voz")
            }
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Buscar")
        }
    }
}
