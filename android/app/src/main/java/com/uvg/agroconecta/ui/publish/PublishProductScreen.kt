package com.uvg.agroconecta.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.validation.PublishProductFormValidator
import kotlinx.coroutines.launch

@Composable
fun PublishProductScreen(
    onNavigateBack: () -> Unit,
    onHomeClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit,
    tipoUsuario: String = "distribuidor",
    viewModel: PublishProductViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Category?>(null) }
    var precio by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var unidadMedida by remember { mutableStateOf("") }
    var tiempoEntrega by remember { mutableStateOf("") }

    // PhotoPicker del sistema: no pide permiso de almacenamiento y solo
    // entrega la foto que el usuario elige.
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onImageSelected(uri) }
    val launchImagePicker = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(uiState.publishState) {
        when (val state = uiState.publishState) {
            is PublishState.Success -> {
                snackbarHostState.showSnackbar("¡Listo! Tu producto ya está publicado.")
                viewModel.resetState()
                onNavigateBack()
            }
            // El producto ya existe: se sale igual que en un éxito para que el
            // distribuidor no lo vuelva a publicar y quede duplicado.
            is PublishState.PublishedWithoutImage -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
                onNavigateBack()
            }
            is PublishState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    LaunchedEffect(uiState.imageError) {
        uiState.imageError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImageError()
        }
    }

    val isPublishing = uiState.publishState is PublishState.Loading

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Publicar producto") },
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.AGREGAR,
                tipoUsuario = tipoUsuario,
                onHomeClick = onHomeClick,
                onAgregarClick = { },
                onPedidosClick = onPedidosClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            ProductImagePicker(
                imagen = uiState.imagen,
                isReading = uiState.isReadingImage,
                enabled = !isPublishing,
                onPick = launchImagePicker,
                onRemove = viewModel::removeImage
            )

            SectionTitle("Producto")
            AgroTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = "Nombre del producto",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AgroTextField(
                value = marca,
                onValueChange = { marca = it },
                label = "Marca (opcional)",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            AgroTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = "Descripción (opcional)",
                supportingText = "Para qué sirve y cómo se usa",
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )
            AgroDropdownField(
                label = "Categoría",
                options = uiState.categorias,
                selected = categoriaSeleccionada,
                onSelectedChange = { categoriaSeleccionada = it },
                optionLabel = { it.nombre },
                supportingText = if (uiState.isLoadingCategorias) "Cargando categorías…" else null
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = AgroSpacing.xs)
            )

            SectionTitle("Precio y existencias")

            AgroTextField(
                value = precio,
                onValueChange = { precio = it },
                label = "Precio en quetzales",
                placeholder = "Ej: 125.50",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )
            AgroTextField(
                value = stock,
                onValueChange = { stock = it },
                label = "Cantidad disponible",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                AgroTextField(
                    value = unidadMedida,
                    onValueChange = { unidadMedida = it },
                    label = "Unidad",
                    placeholder = "kg, L, saco",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
                AgroTextField(
                    value = tiempoEntrega,
                    onValueChange = { tiempoEntrega = it },
                    label = "Días de entrega",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(AgroSpacing.xs))
            AgroPrimaryButton(
                text = "Publicar producto",
                loadingText = "Publicando…",
                icon = Icons.Default.CloudUpload,
                onClick = {
                    scope.launch {
                        val validationError = PublishProductFormValidator.errorFor(
                            name = nombre,
                            hasCategory = categoriaSeleccionada != null,
                            price = precio,
                            stock = stock
                        )
                        if (validationError != null) {
                            snackbarHostState.showSnackbar(validationError)
                            return@launch
                        }
                        val precioNum = checkNotNull(PublishProductFormValidator.priceValue(precio))
                        val stockNum = checkNotNull(PublishProductFormValidator.stockValue(stock))
                        viewModel.publishProduct(
                            nombre = nombre,
                            marca = marca,
                            descripcion = descripcion,
                            idCategoria = categoriaSeleccionada!!.id,
                            precio = precioNum,
                            stock = stockNum,
                            unidadMedida = unidadMedida,
                            tiempoEntrega = tiempoEntrega.toIntOrNull()
                        )
                    }
                },
                // Mientras se lee la foto todavía no hay bytes que subir; publicar
                // en ese momento dejaría el producto sin imagen sin avisar.
                enabled = !uiState.isReadingImage,
                loading = isPublishing,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun ProductImagePicker(
    imagen: SelectedProductImage?,
    isReading: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            when {
                isReading -> CircularProgressIndicator()
                imagen != null -> AsyncImage(
                    model = imagen.previewUri,
                    contentDescription = "Vista previa de la foto del producto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(AgroSpacing.xs))
                    Text(
                        text = "Foto del producto (opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Con foto se vende mejor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgroSecondaryButton(
                text = if (imagen == null) "Elegir foto" else "Cambiar foto",
                icon = Icons.Default.PhotoCamera,
                onClick = onPick,
                enabled = enabled && !isReading
            )
            if (imagen != null) {
                AgroTextButton(
                    text = "Quitar",
                    icon = Icons.Default.Delete,
                    destructive = true,
                    onClick = onRemove,
                    enabled = enabled && !isReading
                )
            }
        }
    }
}
