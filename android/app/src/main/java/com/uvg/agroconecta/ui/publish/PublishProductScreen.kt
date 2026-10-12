package com.uvg.agroconecta.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uvg.agroconecta.data.images.SelectedProductImage
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.validation.PublishProductFormValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    var dropdownExpanded by remember { mutableStateOf(false) }

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
                snackbarHostState.showSnackbar("¡Producto publicado correctamente!")
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Publicar producto", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
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
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProductImagePicker(
                imagen = uiState.imagen,
                isReading = uiState.isReadingImage,
                enabled = uiState.publishState !is PublishState.Loading,
                onPick = launchImagePicker,
                onRemove = viewModel::removeImage
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del producto *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            OutlinedTextField(
                value = marca,
                onValueChange = { marca = it },
                label = { Text("Marca (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                maxLines = 4,
                colors = fieldColors()
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada?.nombre ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (uiState.isLoadingCategorias) {
                        DropdownMenuItem(text = { Text("Cargando...") }, onClick = {})
                    } else {
                        uiState.categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    categoriaSeleccionada = cat
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = GrayLight)

            Text(
                "Datos de inventario",
                style = MaterialTheme.typography.titleSmall,
                color = GreenPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio (Q) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = unidadMedida,
                    onValueChange = { unidadMedida = it },
                    label = { Text("Unidad (ej: kg, L)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = tiempoEntrega,
                    onValueChange = { tiempoEntrega = it },
                    label = { Text("Entrega (días)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = fieldColors()
                )
            }

            // Botón publicar al fondo del scroll
            Spacer(Modifier.height(8.dp))
            Button(
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
                        val precioNum = checkNotNull(
                            PublishProductFormValidator.priceValue(precio)
                        )
                        val stockNum = checkNotNull(
                            PublishProductFormValidator.stockValue(stock)
                        )
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
                enabled = uiState.publishState !is PublishState.Loading && !uiState.isReadingImage,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.publishState is PublishState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Publicar producto", style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductImagePicker(
    imagen: SelectedProductImage?,
    isReading: Boolean,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GreenSurface),
            contentAlignment = Alignment.Center
        ) {
            when {
                isReading -> CircularProgressIndicator(color = GreenPrimary)
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
                        tint = GreenPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Foto del producto (opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayMid
                    )
                    Text(
                        "Se ajusta el tamaño automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMid
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick, enabled = enabled && !isReading) {
                Text(if (imagen == null) "Elegir foto" else "Cambiar foto", color = GreenPrimary)
            }
            if (imagen != null) {
                TextButton(onClick = onRemove, enabled = enabled && !isReading) {
                    Text("Quitar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = GrayLight,
    cursorColor = GreenPrimary,
    unfocusedLabelColor = GrayMid
)
