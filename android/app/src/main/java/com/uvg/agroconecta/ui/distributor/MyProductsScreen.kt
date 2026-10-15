package com.uvg.agroconecta.ui.distributor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.InventoryProduct
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.OrangeLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onImagePicked(uri) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = GrayLight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mis productos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
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
            when {
                uiState.isLoading && uiState.products.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenPrimary
                )

                uiState.loadError != null && uiState.products.isEmpty() -> CenteredMessage(
                    message = uiState.loadError.orEmpty(),
                    onRetry = viewModel::loadProducts,
                    modifier = Modifier.align(Alignment.Center)
                )

                uiState.products.isEmpty() -> CenteredMessage(
                    message = "Todavía no tienes productos publicados.",
                    onRetry = null,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!uiState.isVerified) {
                        item { UnverifiedBanner() }
                    }
                    items(uiState.products, key = { it.idInventario }) { product ->
                        MyProductCard(
                            product = product,
                            isUploading = uiState.uploadingProductId == product.idProducto,
                            // Mientras sube una foto se bloquean los demás botones:
                            // el ViewModel atiende una subida a la vez.
                            enabled = uiState.uploadingProductId == null && uiState.isVerified,
                            onChangePhoto = {
                                if (viewModel.onChangePhotoClick(product.idProducto)) {
                                    pickImage.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            onEdit = { viewModel.onEditInventoryClick(product) }
                        )
                    }
                }
            }
        }
    }

    uiState.inventoryEditor?.let { editor ->
        EditInventoryDialog(
            editor = editor,
            onPriceChange = viewModel::onInventoryPriceChange,
            onStockChange = viewModel::onInventoryStockChange,
            onSave = viewModel::saveInventoryChanges,
            onDismiss = viewModel::dismissInventoryEditor
        )
    }
}

@Composable
private fun MyProductCard(
    product: InventoryProduct,
    isUploading: Boolean,
    enabled: Boolean,
    onChangePhoto: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    ProductImage(
                        imagenUrl = product.imagenUrl,
                        contentDescription = null,
                        categoria = product.categoria,
                        iconSize = 32.dp,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenSurface)
                    )
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = GreenPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product.producto,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    listOfNotNull(product.marca, product.categoria)
                        .takeIf { it.isNotEmpty() }
                        ?.let { Text(it.joinToString(" · "), fontSize = 12.sp, color = GrayMid) }
                    Text(
                        text = "Q${"%.2f".format(product.precio)} · ${product.stock} en stock",
                        fontSize = 12.sp,
                        color = GrayMid
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Van debajo y no junto a la foto porque en pantallas de 360 dp los dos
            // botones no caben al lado de la imagen sin partir el texto.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardActionButton(
                    text = if (product.imagenUrl == null) "Agregar foto" else "Cambiar foto",
                    icon = Icons.Default.AddPhotoAlternate,
                    enabled = enabled,
                    onClick = onChangePhoto,
                    modifier = Modifier.weight(1f)
                )
                CardActionButton(
                    text = "Precio y stock",
                    icon = Icons.Default.Edit,
                    enabled = enabled,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CardActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) GreenPrimary else GrayMid,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = if (enabled) GreenPrimary else GrayMid,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EditInventoryDialog(
    editor: InventoryEditorState,
    onPriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GreenPrimary) },
        title = { Text("Precio y stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = editor.productName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                OutlinedTextField(
                    value = editor.price,
                    onValueChange = onPriceChange,
                    label = { Text("Precio (Q)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editor.stock,
                    onValueChange = onStockChange,
                    label = { Text("Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !editor.isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
                editor.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !editor.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (editor.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !editor.isSaving) { Text("Cancelar") }
        }
    )
}

@Composable
private fun UnverifiedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OrangeLight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = OrangeAccent
        )
        Spacer(Modifier.width(10.dp))
        Text(text = UNVERIFIED_ACCOUNT_MESSAGE, fontSize = 13.sp)
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = GrayMid, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Reintentar")
            }
        }
    }
}
