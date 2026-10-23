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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.InventoryProduct
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.categoryBackground
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers

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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Mis productos", onBack = onNavigateBack) }
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = viewModel::loadProducts,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.products.isEmpty() -> SkeletonList(
                    description = "Cargando tus productos",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                uiState.loadError != null && uiState.products.isEmpty() -> RefreshableFullScreen {
                    ErrorState(
                        title = "No pudimos cargar tus productos",
                        message = uiState.loadError.orEmpty(),
                        onRetry = viewModel::loadProducts
                    )
                }

                uiState.products.isEmpty() -> RefreshableFullScreen {
                    EmptyState(
                        icon = Icons.Default.Inventory2,
                        title = "Todavía no tienes productos publicados.",
                        message = "Usa la pestaña Publicar para subir tu primer producto."
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    if (!uiState.isVerified) {
                        item(key = "unverified") { UnverifiedBanner() }
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
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AgroSpacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(contentAlignment = Alignment.Center) {
                    ProductImage(
                        imagenUrl = product.imagenUrl,
                        contentDescription = null,
                        categoria = product.categoria,
                        iconSize = 32.dp,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(categoryBackground(product.categoria))
                    )
                    if (isUploading) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.width(AgroSpacing.sm))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {}
                ) {
                    Text(
                        text = product.producto,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    listOfNotNull(product.marca, product.categoria)
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            Text(
                                text = it.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    Text(
                        text = formatQuetzales(product.precio),
                        style = MaterialTheme.typography.titleMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${product.stock} disponibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Van debajo y no junto a la foto porque en pantallas de 360 dp los dos
            // botones no caben al lado de la imagen sin partir el texto.
            Row(
                modifier = Modifier.padding(top = AgroSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                AgroSecondaryButton(
                    text = if (product.imagenUrl == null) "Agregar foto" else "Cambiar foto",
                    loadingText = "Subiendo…",
                    icon = Icons.Default.AddPhotoAlternate,
                    onClick = onChangePhoto,
                    enabled = enabled,
                    loading = isUploading,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Cambiar foto de ${product.producto}" }
                )
                AgroSecondaryButton(
                    text = "Precio y stock",
                    icon = Icons.Default.Edit,
                    onClick = onEdit,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Editar precio y stock de ${product.producto}" }
                )
            }
        }
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
        onDismissRequest = { if (!editor.isSaving) onDismiss() },
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Precio y stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                Text(
                    text = editor.productName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AgroTextField(
                    value = editor.price,
                    onValueChange = onPriceChange,
                    label = "Precio en quetzales",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !editor.isSaving
                )
                AgroTextField(
                    value = editor.stock,
                    onValueChange = onStockChange,
                    label = "Cantidad disponible",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !editor.isSaving
                )
                editor.error?.let { InlineError(message = it) }
            }
        },
        confirmButton = {
            AgroPrimaryButton(
                text = "Guardar",
                loadingText = "Guardando…",
                onClick = onSave,
                loading = editor.isSaving
            )
        },
        dismissButton = {
            AgroTextButton(text = "Cancelar", onClick = onDismiss, enabled = !editor.isSaving)
        }
    )
}

@Composable
private fun UnverifiedBanner() {
    Surface(
        color = MaterialTheme.agroColors.warningContainer,
        contentColor = MaterialTheme.agroColors.onWarningContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null)
            Spacer(Modifier.width(AgroSpacing.sm))
            Text(text = UNVERIFIED_ACCOUNT_MESSAGE, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
