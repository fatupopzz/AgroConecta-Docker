package com.uvg.agroconecta.ui.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.categoryBackground
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import kotlin.math.roundToInt

/**
 * Diagnóstico de una hoja con el modelo del teléfono. El resultado va arriba
 * y en grande: es lo que la persona vino a saber. Productos y aviso a vecinos
 * quedan debajo, solo cuando la hoja tiene una enfermedad.
 */
@Composable
fun PlantDiagnosisScreen(
    uiState: PlantDiagnosisUiState,
    message: String?,
    onNavigateBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakeAnotherPhoto: () -> Unit,
    onRetryProducts: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Int) -> Unit,
    onAlertNeighbors: () -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = rememberAgroHaptics()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }
    // Analizar tarda unos segundos; la vibración avisa que ya hay resultado
    // aunque la persona haya bajado el teléfono mientras esperaba.
    LaunchedEffect(uiState is PlantDiagnosisUiState.Result) {
        if (uiState is PlantDiagnosisUiState.Result) haptics.confirm()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Diagnosticar una hoja", onBack = onNavigateBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                PlantDiagnosisUiState.Capture -> CaptureContent(onTakePhoto, onPickFromGallery)
                PlantDiagnosisUiState.Analyzing -> AnalyzingContent()
                is PlantDiagnosisUiState.UnclearPhoto -> UnclearPhotoContent(onTakeAnotherPhoto)
                is PlantDiagnosisUiState.Result -> ResultContent(
                    state = uiState,
                    onTakeAnotherPhoto = onTakeAnotherPhoto,
                    onRetryProducts = onRetryProducts,
                    onAddToCart = onAddToCart,
                    onProductClick = onProductClick,
                    onAlertNeighbors = onAlertNeighbors
                )
            }
        }
    }
}

@Composable
private fun CaptureContent(onTakePhoto: () -> Unit, onPickFromGallery: () -> Unit) {
    CenteredColumn {
        StateIcon(
            icon = Icons.Default.PhotoCamera,
            container = MaterialTheme.colorScheme.primaryContainer,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(AgroSpacing.md))
        Text(
            text = "Toma una foto de una hoja afectada",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(AgroSpacing.xs))
        Text(
            text = "Una sola hoja, de cerca y con buena luz. Funciona con maíz, tomate, papa y chile pimiento, aunque no tengas señal.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AgroSpacing.lg))
        AgroPrimaryButton(
            text = "Tomar foto",
            icon = Icons.Default.PhotoCamera,
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(AgroSpacing.xs))
        AgroSecondaryButton(
            text = "Elegir de la galería",
            icon = Icons.Default.PhotoLibrary,
            onClick = onPickFromGallery,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnalyzingContent() {
    CenteredColumn {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(AgroSpacing.md))
        Text(
            text = "Analizando la hoja…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
private fun UnclearPhotoContent(onTakeAnotherPhoto: () -> Unit) {
    CenteredColumn {
        StateIcon(
            icon = Icons.Default.WarningAmber,
            container = MaterialTheme.agroColors.warningContainer,
            tint = MaterialTheme.agroColors.onWarningContainer
        )
        Spacer(Modifier.height(AgroSpacing.md))
        Text(
            text = "Foto no clara",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(AgroSpacing.xs))
        Text(
            text = UNCLEAR_PHOTO_MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AgroSpacing.lg))
        AgroPrimaryButton(
            text = "Tomar otra foto",
            icon = Icons.Default.PhotoCamera,
            onClick = onTakeAnotherPhoto,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultContent(
    state: PlantDiagnosisUiState.Result,
    onTakeAnotherPhoto: () -> Unit,
    onRetryProducts: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Int) -> Unit,
    onAlertNeighbors: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AgroSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        DiagnosisCard(state)
        UploadStatus(state.upload)

        if (!state.label.isHealthy) {
            ProductsSection(state, onRetryProducts, onAddToCart, onProductClick)
            AlertSection(state, onAlertNeighbors)
        }

        AgroSecondaryButton(
            text = "Diagnosticar otra hoja",
            icon = Icons.Default.PhotoCamera,
            onClick = onTakeAnotherPhoto,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DiagnosisCard(state: PlantDiagnosisUiState.Result) {
    val healthy = state.label.isHealthy
    val confidence = (state.confidence * 100).roundToInt()

    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        containerColor = if (healthy) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.agroColors.warningContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (healthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (healthy) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.agroColors.onWarningContainer
                    }
                )
                Spacer(Modifier.width(AgroSpacing.xs))
                Text(
                    text = state.label.crop,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                StatusTag(text = "Confianza: $confidence %", tone = TagTone.Neutral)
            }
            Text(
                text = if (healthy) "¡Tu hoja está sana!" else state.label.disease,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (healthy) {
                    "No vemos señales de las enfermedades que conoce el modelo. Sigue revisando tu cultivo con regularidad."
                } else {
                    "Es una orientación a partir de la foto. Confírmala con un técnico agrícola antes de aplicar un tratamiento."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun UploadStatus(upload: UploadState) {
    when (upload) {
        UploadState.Sending -> InfoRow(null, "Guardando el diagnóstico…", showProgress = true)
        is UploadState.Sent -> Unit
        is UploadState.Pending -> InfoRow(
            Icons.Default.CloudOff,
            "Sin señal: el diagnóstico quedó guardado en tu teléfono y se enviará solo cuando vuelva la conexión."
        )
        is UploadState.Failed -> InfoRow(Icons.Default.WarningAmber, upload.message)
    }
}

@Composable
private fun ProductsSection(
    state: PlantDiagnosisUiState.Result,
    onRetryProducts: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Int) -> Unit
) {
    Text(
        text = "Productos recomendados",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = AgroSpacing.xs)
            .semantics { heading() }
    )
    when (val products = state.products) {
        ProductsState.NotApplicable -> Unit
        ProductsState.Loading -> InfoRow(null, "Buscando productos…", showProgress = true)
        ProductsState.WaitingForSignal -> InfoRow(Icons.Default.CloudOff, PRODUCTS_WAITING_MESSAGE)
        is ProductsState.Error -> InlineError(message = products.message, onRetry = onRetryProducts)
        is ProductsState.Loaded -> {
            products.products.forEach { product ->
                ProductRow(
                    product = product,
                    isAdding = product.id in state.addingToCart,
                    onAddToCart = { onAddToCart(product) },
                    onClick = { onProductClick(product.id) }
                )
            }
            products.recommendation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, isAdding: Boolean, onAddToCart: () -> Unit, onClick: () -> Unit) {
    AgroCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imagenUrl = product.imagenUrl,
                imagenThumbUrl = product.imagenThumbUrl,
                contentDescription = null,
                categoria = product.categoria,
                iconSize = 24.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(categoryBackground(product.categoria))
            )
            Spacer(Modifier.width(AgroSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                listOfNotNull(product.marca, product.categoria).takeIf { it.isNotEmpty() }?.let {
                    Text(
                        text = it.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onAddToCart, enabled = !isAdding) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AgroSizes.iconSmall),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Agregar ${product.nombre} al carrito",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertSection(state: PlantDiagnosisUiState.Result, onAlertNeighbors: () -> Unit) {
    val sent = state.upload is UploadState.Sent
    val enabled = sent && state.hasLocation && state.alert == AlertState.Idle
    AgroPrimaryButton(
        text = if (state.alert == AlertState.Sent) "Vecinos avisados" else "Avisar a mis vecinos",
        loadingText = "Avisando…",
        icon = if (state.alert == AlertState.Sent) Icons.Default.CheckCircle else Icons.Default.Campaign,
        onClick = onAlertNeighbors,
        enabled = enabled,
        loading = state.alert == AlertState.Sending,
        modifier = Modifier.fillMaxWidth()
    )
    val hint = when {
        state.alert == AlertState.Sent -> null
        state.upload is UploadState.Pending -> "Podrás avisar a tus vecinos cuando tengas señal."
        !state.hasLocation && sent -> "Activa la ubicación y diagnostica de nuevo para avisar a tus vecinos."
        else -> null
    }
    hint?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector?, text: String, showProgress: Boolean = false) {
    Surface(
        color = MaterialTheme.agroColors.warningContainer,
        contentColor = MaterialTheme.agroColors.onWarningContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            when {
                showProgress -> CircularProgressIndicator(
                    modifier = Modifier.size(AgroSizes.iconSmall),
                    color = MaterialTheme.agroColors.onWarningContainer,
                    strokeWidth = 2.dp
                )
                icon != null -> Icon(icon, contentDescription = null)
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StateIcon(icon: ImageVector, container: Color, tint: Color) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = tint,
        modifier = Modifier.size(AgroSizes.illustration)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AgroSizes.iconLarge))
        }
    }
}

// Centrado en la pantalla, pero desplazable: con la letra al 200 % los
// botones quedarían fuera de alcance si la columna no pudiera moverse.
@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(AgroSpacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}
