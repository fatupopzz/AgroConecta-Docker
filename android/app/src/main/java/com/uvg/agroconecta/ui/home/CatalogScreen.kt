package com.uvg.agroconecta.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.CategoryChip
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductCard
import com.uvg.agroconecta.ui.components.ProductCardSkeleton
import com.uvg.agroconecta.ui.theme.AgroSpacing

/**
 * Catálogo con búsqueda, categorías y filtros a la vista. Los productos se
 * cargan solos al acercarse al final de la lista: un botón "cargar más"
 * escondido al fondo era un paso extra que pocos encontraban.
 *
 * La cuadrícula es adaptable: en teléfonos normales caben dos columnas y con
 * la letra muy grande baja a una, en vez de apretar las tarjetas.
 */
@Composable
fun CatalogScreen(
    initialQuery: String,
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    initialCategoryId: Int? = null,
    calendarContext: Boolean = false,
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()

    LaunchedEffect(initialQuery, initialCategoryId) {
        viewModel.applyCatalogFilter(initialQuery, initialCategoryId)
    }

    LaunchedEffect(Unit) {
        if (uiState.categorias.isEmpty()) viewModel.loadCategorias()
    }

    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    val visibleProducts = remember(
        uiState.productos,
        uiState.filtroPrecioMin,
        uiState.filtroPrecioMax,
        uiState.filtroMarca
    ) {
        applyLocalFilters(uiState.productos, uiState.filtroPrecioMin, uiState.filtroPrecioMax, uiState.filtroMarca)
    }
    val filterLabels = activeFilterLabels(uiState.filtroPrecioMin, uiState.filtroPrecioMax, uiState.filtroMarca)

    // derivedStateOf evita recomponer en cada píxel de scroll: solo cambia
    // cuando de verdad se cruza el umbral para pedir la siguiente página.
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, uiState.hasMore, uiState.isLoadingProductos) {
        if (nearEnd && uiState.hasMore && !uiState.isLoadingProductos && uiState.productos.isNotEmpty()) {
            viewModel.loadProductos()
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

    val submitSearch: () -> Unit = {
        keyboardController?.hide()
        viewModel.onSearchSubmit()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Catálogo", onBack = onBack) }
    ) { innerPadding ->
        // Recargar repite la búsqueda actual, con sus filtros, desde la página 1.
        AgroPullToRefreshBox(
            isRefreshing = uiState.isLoadingProductos,
            onRefresh = viewModel::onSearchSubmit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AgroSpacing.screenHorizontal,
                    end = AgroSpacing.screenHorizontal,
                    bottom = AgroSpacing.lg
                ),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                fullWidthItem("search") {
                    Row(
                        modifier = Modifier.padding(top = AgroSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
                    ) {
                        AgroTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchChange,
                            label = "Buscar producto",
                            leadingIcon = Icons.Default.Search,
                            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = {
                                        viewModel.onSearchChange("")
                                        viewModel.onSearchSubmit()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                                    }
                                }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                            modifier = Modifier.weight(1f)
                        )
                        FilterButton(
                            activeCount = filterLabels.size,
                            onClick = { viewModel.abrirFiltros() }
                        )
                    }
                }

                if (uiState.categorias.isNotEmpty()) {
                    fullWidthItem("categories") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                            item(key = "all") {
                                CategoryChip(
                                    label = "Todos",
                                    selected = uiState.categoriaSeleccionadaId == null,
                                    onClick = { viewModel.onCategoriaSelect(null) }
                                )
                            }
                            items(uiState.categorias, key = { it.id }) { categoria ->
                                CategoryChip(
                                    label = categoria.nombre,
                                    selected = uiState.categoriaSeleccionadaId == categoria.id,
                                    onClick = { viewModel.onCategoriaSelect(categoria.id) }
                                )
                            }
                        }
                    }
                }

                if (filterLabels.isNotEmpty()) {
                    fullWidthItem("active-filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                            items(filterLabels) { label ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.onFiltrosLimpiados() },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.semantics {
                                        contentDescription = "Filtro $label. Toca para quitar los filtros"
                                    }
                                )
                            }
                        }
                    }
                }

                // Viniendo del calendario de cultivo solo se conoce la categoría
                // sugerida, no un producto concreto; se explica el siguiente paso.
                if (calendarContext) {
                    fullWidthItem("calendar-context") {
                        CalendarContextNotice()
                    }
                }

                // El error queda a la vista con su botón, en vez de un aviso que
                // desaparece antes de que alguien con poca práctica lo lea.
                uiState.errorMessage?.let { message ->
                    fullWidthItem("error") {
                        InlineError(
                            message = message,
                            onRetry = viewModel::onSearchSubmit,
                            retryLabel = "Reintentar"
                        )
                    }
                }

                fullWidthItem("title") {
                    ResultsTitle(
                        query = uiState.searchQuery,
                        count = visibleProducts.size,
                        isLoading = uiState.isLoadingProductos && uiState.productos.isEmpty(),
                        hasMore = uiState.hasMore
                    )
                }

                when {
                    uiState.isLoadingProductos && uiState.productos.isEmpty() -> {
                        items(6, key = { "skeleton-$it" }) { ProductCardSkeleton() }
                    }

                    // Sin conexión solo están los productos guardados en el teléfono
                    // (la primera página); decir "no hay productos" haría creer que la
                    // categoría está vacía cuando en realidad no se pudo consultar.
                    visibleProducts.isEmpty() && uiState.isOffline -> fullWidthItem("empty-offline") {
                        EmptyState(
                            icon = Icons.Default.WifiOff,
                            title = "Sin conexión",
                            message = "Solo podemos mostrarte los productos que guardaste antes. " +
                                "Cuando vuelva la señal verás todo el catálogo.",
                            actionLabel = "Ver todos los guardados",
                            onAction = {
                                viewModel.onFiltrosLimpiados()
                                viewModel.onCategoriaSelect(null)
                            }
                        )
                    }

                    visibleProducts.isEmpty() -> fullWidthItem("empty") {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No encontramos productos",
                            message = if (filterLabels.isNotEmpty()) {
                                "Prueba quitando algún filtro o buscando con otra palabra."
                            } else {
                                "Prueba con otra palabra o revisa cómo está escrita."
                            },
                            actionLabel = if (filterLabels.isNotEmpty()) "Quitar filtros" else null,
                            onAction = if (filterLabels.isNotEmpty()) {
                                { viewModel.onFiltrosLimpiados() }
                            } else {
                                null
                            }
                        )
                    }

                    else -> {
                        items(visibleProducts, key = { it.id }) { product ->
                            ProductCard(
                                producto = product,
                                onClick = { onProductClick(product.id) },
                                showFavoriteAction = showFavoriteAction,
                                isFavorite = product.id in favoriteIds,
                                isUpdatingFavorite = product.id in pendingFavoriteIds,
                                onFavoriteClick = { onFavoriteClick(product.id) }
                            )
                        }
                        if (uiState.isLoadingProductos) {
                            items(2, key = { "more-$it" }) { ProductCardSkeleton() }
                        }
                    }
                }
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 4

@Composable
private fun CalendarContextNotice() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Text(
                text = "Te sugerimos esta categoría según tu calendario. Elige un producto y luego a qué distribuidor comprarle.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun LazyGridScope.fullWidthItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    AgroSecondaryButton(
        text = if (activeCount > 0) "Filtros ($activeCount)" else "Filtros",
        icon = Icons.Default.Tune,
        onClick = onClick,
        modifier = Modifier.padding(top = AgroSpacing.xs)
    )
}

@Composable
private fun ResultsTitle(query: String, count: Int, isLoading: Boolean, hasMore: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (query.isBlank()) "Todos los productos" else "Resultados para “$query”",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        if (!isLoading) {
            Text(
                text = when {
                    count == 1 -> "1 producto"
                    hasMore -> "$count productos por ahora"
                    else -> "$count productos"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}
