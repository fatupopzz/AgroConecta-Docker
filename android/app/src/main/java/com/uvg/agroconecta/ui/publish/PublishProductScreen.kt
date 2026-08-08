package com.uvg.agroconecta.ui.publish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishProductScreen(
    onNavigateBack: () -> Unit,
    onHomeClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onPerfilClick: () -> Unit,
    tipoUsuario: String = "distribuidor",
    viewModel: PublishProductViewModel = viewModel()
) {
    val context = LocalContext.current
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

    LaunchedEffect(uiState.publishState) {
        when (val state = uiState.publishState) {
            is PublishState.Success -> {
                snackbarHostState.showSnackbar("¡Producto publicado correctamente!")
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
                        val token = SessionManager.getToken(context).first()
                        if (token == null) { snackbarHostState.showSnackbar("Sesión inválida"); return@launch }
                        if (nombre.isBlank()) { snackbarHostState.showSnackbar("El nombre es obligatorio"); return@launch }
                        if (categoriaSeleccionada == null) { snackbarHostState.showSnackbar("Seleccioná una categoría"); return@launch }
                        val precioNum = precio.toDoubleOrNull()
                        if (precioNum == null || precioNum <= 0) { snackbarHostState.showSnackbar("Ingresá un precio válido"); return@launch }
                        val stockNum = stock.toIntOrNull()
                        if (stockNum == null || stockNum < 0) { snackbarHostState.showSnackbar("Ingresá un stock válido"); return@launch }
                        viewModel.publishProduct(
                            token = token,
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
                enabled = uiState.publishState !is PublishState.Loading,
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
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    unfocusedBorderColor = GrayLight,
    cursorColor = GreenPrimary,
    unfocusedLabelColor = GrayMid
)