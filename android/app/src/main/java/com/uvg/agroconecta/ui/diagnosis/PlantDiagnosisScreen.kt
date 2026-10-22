package com.uvg.agroconecta.ui.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.OrangeLight
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = GrayLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Diagnosticar una hoja", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
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
        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Toma una foto de una hoja afectada",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Una sola hoja, de cerca y con buena luz. Funciona con maíz, tomate, papa y chile pimiento, aunque no tengas señal.",
            color = GrayMid,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Tomar foto")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPickFromGallery, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = GreenPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Elegir de la galería", color = GreenPrimary)
        }
    }
}

@Composable
private fun AnalyzingContent() {
    CenteredColumn {
        CircularProgressIndicator(color = GreenPrimary)
        Spacer(Modifier.height(16.dp))
        Text("Analizando la hoja…", color = GrayMid)
    }
}

@Composable
private fun UnclearPhotoContent(onTakeAnotherPhoto: () -> Unit) {
    CenteredColumn {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Foto no clara", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(UNCLEAR_PHOTO_MESSAGE, color = GrayMid, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onTakeAnotherPhoto,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) { Text("Tomar otra foto") }
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiagnosisCard(state)
        UploadStatus(state.upload)

        if (!state.label.isHealthy) {
            ProductsSection(state, onRetryProducts, onAddToCart, onProductClick)
            AlertSection(state, onAlertNeighbors)
        }

        OutlinedButton(onClick = onTakeAnotherPhoto, modifier = Modifier.fillMaxWidth()) {
            Text("Diagnosticar otra hoja", color = GreenPrimary)
        }
    }
}

@Composable
private fun DiagnosisCard(state: PlantDiagnosisUiState.Result) {
    val healthy = state.label.isHealthy
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (healthy) GreenSurface else Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (healthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (healthy) GreenPrimary else OrangeAccent
                )
                Spacer(Modifier.width(8.dp))
                Text(state.label.crop, color = GrayMid)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (healthy) "¡Tu hoja está sana!" else state.label.disease,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text("Confianza: ${(state.confidence * 100).roundToInt()} %", color = GrayMid, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (healthy) {
                    "No vemos señales de las enfermedades que conoce el modelo. Sigue revisando tu cultivo con regularidad."
                } else {
                    "Es una orientación a partir de la foto. Confírmala con un técnico agrícola antes de aplicar un tratamiento."
                },
                fontSize = 13.sp
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
    Text("Productos recomendados", fontWeight = FontWeight.SemiBold)
    when (val products = state.products) {
        ProductsState.NotApplicable -> Unit
        ProductsState.Loading -> InfoRow(null, "Buscando productos…", showProgress = true)
        ProductsState.WaitingForSignal -> InfoRow(Icons.Default.CloudOff, PRODUCTS_WAITING_MESSAGE)
        is ProductsState.Error -> Column {
            InfoRow(Icons.Default.WarningAmber, products.message)
            TextButton(onClick = onRetryProducts) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = GreenPrimary)
                Spacer(Modifier.width(6.dp))
                Text("Reintentar", color = GreenPrimary)
            }
        }
        is ProductsState.Loaded -> {
            products.products.forEach { product ->
                ProductRow(
                    product = product,
                    isAdding = product.id in state.addingToCart,
                    onAddToCart = { onAddToCart(product) },
                    onClick = { onProductClick(product.id) }
                )
            }
            products.recommendation?.let { Text(it, fontSize = 13.sp, color = GrayMid) }
        }
    }
}

@Composable
private fun ProductRow(product: Product, isAdding: Boolean, onAddToCart: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(
                imagenUrl = product.imagenUrl,
                imagenThumbUrl = product.imagenThumbUrl,
                contentDescription = null,
                categoria = product.categoria,
                iconSize = 24.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenSurface)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.nombre, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                listOfNotNull(product.marca, product.categoria).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString(" · "), fontSize = 12.sp, color = GrayMid)
                }
            }
            IconButton(onClick = onAddToCart, enabled = !isAdding) {
                if (isAdding) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = GreenPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Agregar ${product.nombre} al carrito", tint = GreenPrimary)
                }
            }
        }
    }
}

@Composable
private fun AlertSection(state: PlantDiagnosisUiState.Result, onAlertNeighbors: () -> Unit) {
    val sent = state.upload is UploadState.Sent
    val enabled = sent && state.hasLocation && state.alert == AlertState.Idle
    Button(
        onClick = onAlertNeighbors,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (state.alert == AlertState.Sending) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Campaign, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (state.alert == AlertState.Sent) "Vecinos avisados" else "Avisar a mis vecinos")
        }
    }
    val hint = when {
        state.alert == AlertState.Sent -> null
        state.upload is UploadState.Pending -> "Podrás avisar a tus vecinos cuando tengas señal."
        !state.hasLocation && sent -> "Activa la ubicación y diagnostica de nuevo para avisar a tus vecinos."
        else -> null
    }
    hint?.let { Text(it, fontSize = 12.sp, color = GrayMid) }
}

@Composable
private fun InfoRow(icon: ImageVector?, text: String, showProgress: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(OrangeLight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            showProgress -> CircularProgressIndicator(Modifier.size(18.dp), color = GreenPrimary, strokeWidth = 2.dp)
            icon != null -> Icon(icon, contentDescription = null, tint = OrangeAccent)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}
