package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab

private val VerdeAgroConecta = Color(0xFF2D6A1F)
private val VerdeClaro = Color(0xFF4CAF50)
private val NaranjaOferta = Color(0xFFE65100)
private val NaranjaOfertaClaro = Color(0xFFFFF3E0)
private val GrisFondo = Color(0xFFF5F5F5)
private val TextoGris = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductoClick: (productoId: Int) -> Unit,
    onVerMasProductos: () -> Unit,
    onVerTodasCategorias: () -> Unit,
    onCarritoClick: () -> Unit,
    onPerfilClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onDistribuidorClick: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.filtrosAbiertos) {
        FilterBottomSheet(
            precioMinInicial = uiState.filtroPrecioMin,
            precioMaxInicial = uiState.filtroPrecioMax,
            marcaInicial = uiState.filtroMarca,
            onAplicar = { min, max, marca -> viewModel.onFiltrosAplicados(min, max, marca) },
            onLimpiar = { viewModel.onFiltrosLimpiados() },
            onDismiss = { viewModel.cerrarFiltros() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GrisFondo,
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.HOME,
                onHomeClick = { },
                onAgregarClick = onAgregarClick,
                onPedidosClick = { },
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                HomeHeader(
                    nombre = uiState.nombreAgricultor,
                    searchQuery = uiState.searchQuery,
                    onSearchChange = { viewModel.onSearchChange(it) },
                    onSearchSubmit = { viewModel.onSearchSubmit() },
                    onFiltrosClick = { viewModel.abrirFiltros() },
                    onCarritoClick = onCarritoClick,
                    onPerfilClick = onPerfilClick
                )
            }
            item {
                SeccionCategorias(
                    categorias = uiState.categorias,
                    categoriaSeleccionadaId = uiState.categoriaSeleccionadaId,
                    onCategoriaSelect = { viewModel.onCategoriaSelect(it) },
                    onVerTodas = onVerTodasCategorias
                )
            }
            item {
                SeccionProductosDestacados(
                    productos = uiState.productos.take(4),
                    isLoading = uiState.isLoadingProductos,
                    onProductoClick = onProductoClick,
                    onVerMas = onVerMasProductos
                )
            }
            uiState.ofertaDelDia?.let { oferta ->
                item {
                    OfertaBanner(
                        producto = oferta,
                        onClick = { onProductoClick(oferta.id) }
                    )
                }
            }
            item {
                SeccionDistribuidores(
                    distribuidores = uiState.distribuidores,
                    isLoading = uiState.isLoadingDistribuidores,
                    onDistribuidorClick = onDistribuidorClick
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    nombre: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onFiltrosClick: () -> Unit,
    onCarritoClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VerdeAgroConecta)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Buen día,", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                Text(
                    text = nombre.ifBlank { "Agricultor" },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCarritoClick) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = "Carrito", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(VerdeClaro)
                        .clickable { onPerfilClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nombre.firstOrNull()?.uppercase() ?: "A",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            placeholder = { Text("Buscar productos...", color = Color.Gray) },
            leadingIcon = {
                IconButton(onClick = onFiltrosClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Filtros", tint = Color.Gray)
                }
            },
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    onSearchSubmit()
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = VerdeAgroConecta)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                onSearchSubmit()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SeccionCategorias(
    categorias: List<Category>,
    categoriaSeleccionadaId: Int?,
    onCategoriaSelect: (Int?) -> Unit,
    onVerTodas: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categorías", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Ver todas...",
                color = VerdeClaro,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onVerTodas() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoriaSeleccionadaId == null,
                onClick = { onCategoriaSelect(null) },
                label = { Text("Todos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VerdeAgroConecta,
                    selectedLabelColor = Color.White
                )
            )
            categorias.forEach { cat ->
                FilterChip(
                    selected = categoriaSeleccionadaId == cat.id,
                    onClick = { onCategoriaSelect(cat.id) },
                    label = { Text(cat.nombre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeAgroConecta,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SeccionProductosDestacados(
    productos: List<Product>,
    isLoading: Boolean,
    onProductoClick: (Int) -> Unit,
    onVerMas: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Productos destacados", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                "Ver más",
                color = VerdeClaro,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onVerMas() }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading && productos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VerdeAgroConecta)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                productos.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { producto ->
                            ProductCard(
                                producto = producto,
                                onClick = { onProductoClick(producto.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    producto: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tieneStock = (producto.precioDesde ?: 0.0) > 0.0
    val precioTexto = producto.precioDesde?.let { "Q %.2f".format(it) } ?: "Sin stock"

    Card(
        modifier = modifier.clickable(enabled = tieneStock, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        when (producto.categoria?.lowercase()) {
                            "fertilizantes" -> Color(0xFFE8F5E9)
                            "pesticidas", "herbicidas" -> Color(0xFFFFF8E1)
                            "semillas" -> Color(0xFFE3F2FD)
                            else -> Color(0xFFF5F5F5)
                        }
                    )
            ) {
                if (!tieneStock) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color(0xFFB71C1C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Sin stock", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                producto.categoria?.let { cat ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VerdeClaro.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = cat,
                            color = VerdeAgroConecta,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = precioTexto,
                        fontWeight = FontWeight.Bold,
                        color = if (tieneStock) VerdeAgroConecta else TextoGris,
                        fontSize = 15.sp
                    )
                    if (tieneStock) {
                        SmallFloatingActionButton(
                            onClick = onClick,
                            containerColor = VerdeAgroConecta,
                            contentColor = Color.White,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Ver",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfertaBanner(producto: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NaranjaOfertaClaro),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NaranjaOferta)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = NaranjaOferta) {
                Text(
                    "Oferta del día",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "15% de descuento · Solo hoy",
                color = NaranjaOferta.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                producto.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = NaranjaOferta
            )
        }
    }
}

@Composable
private fun SeccionDistribuidores(
    distribuidores: List<Distributor>,
    isLoading: Boolean,
    onDistribuidorClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            "Distribuidores verificados",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        if (isLoading && distribuidores.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterHorizontally),
                color = VerdeAgroConecta
            )
        } else if (distribuidores.isEmpty()) {
            Text(
                "No hay distribuidores verificados aún",
                color = TextoGris,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                distribuidores.forEach { dist ->
                    DistribuidorCard(
                        distribuidor = dist,
                        onClick = { onDistribuidorClick(dist.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DistribuidorCard(
    distribuidor: Distributor,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = distribuidor.nombreNegocio,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (distribuidor.estadoVerificacion == "verificado") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VerdeClaro.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Verificado",
                            color = VerdeAgroConecta,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            distribuidor.departamento?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = TextoGris, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i <= distribuidor.calificacion) Color(0xFFFFC107)
                        else Color(0xFFDDDDDD),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}