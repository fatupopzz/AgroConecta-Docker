package com.uvg.agroconecta.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.AppNotification
import com.uvg.agroconecta.data.models.Category
import com.uvg.agroconecta.data.models.CropCycleResponse
import com.uvg.agroconecta.data.models.CropPhase
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductCard
import com.uvg.agroconecta.ui.components.ProductCardSkeleton
import com.uvg.agroconecta.ui.components.ProductGridSkeleton
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.SectionHeader
import com.uvg.agroconecta.ui.components.SkeletonBox
import com.uvg.agroconecta.ui.components.StarRating
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.categoryBackground
import com.uvg.agroconecta.ui.notifications.PriceNotificationState
import com.uvg.agroconecta.ui.notifications.priceNotificationText
import com.uvg.agroconecta.ui.theme.AgroConectaTheme
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.time.LocalTime
import java.util.Locale

private val homeLocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

internal fun shouldShowCropCycleCard(
    userType: String,
    cycle: CropCycleResponse?
): Boolean = userType == "agricultor" && cycle?.faseActual != null

internal fun cropCycleTitle(crop: String): String =
    "Tu cultivo: ${crop.replaceFirstChar { character -> character.titlecase() }}"

/** "Buen día" a las 8 de la noche suena a app hecha sin pensar en quien la usa. */
internal fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

/**
 * Inicio de la app. El orden sigue lo que más se hace: buscar o elegir una
 * categoría arriba, después lo personalizado (avisos, cultivo, recomendados)
 * y al final lo de consulta (alertas de plagas, distribuidores).
 *
 * La búsqueda y las categorías llevan al catálogo en vez de filtrar aquí: en
 * Inicio solo se ven cuatro productos y filtrar esa lista confundía.
 */
@Composable
fun HomeScreen(
    onProductoClick: (productoId: Int) -> Unit,
    onVerMasProductos: () -> Unit,
    onVerTodasCategorias: () -> Unit,
    onCarritoClick: () -> Unit,
    onPerfilClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPedidosClick: () -> Unit,
    onDistribuidorClick: (Int) -> Unit,
    tipoUsuario: String = "agricultor",
    cartItemCount: Int = 0,
    onSearch: (String) -> Unit = {},
    onCategoryClick: (Int) -> Unit = {},
    priceNotifications: PriceNotificationState = PriceNotificationState(),
    onPriceNotificationClick: (AppNotification) -> Unit = {},
    onPriceNotificationRead: (AppNotification) -> Unit = {},
    onRetryPriceNotifications: () -> Unit = {},
    urgentNotification: AppNotification? = null,
    onUrgentNotificationClick: (AppNotification) -> Unit = {},
    onRecommendedProductClick: (String) -> Unit = {},
    onPestAlertsClick: () -> Unit = {},
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.refreshLocation()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    LaunchedEffect(tipoUsuario) {
        if (tipoUsuario == "agricultor") {
            if (context.hasHomeLocationPermission()) {
                viewModel.refreshLocation()
            } else {
                locationPermissionLauncher.launch(homeLocationPermissions)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    val esAgricultor = tipoUsuario == "agricultor"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.HOME,
                tipoUsuario = tipoUsuario,
                onHomeClick = { },
                onAgregarClick = onAgregarClick,
                onPedidosClick = onPedidosClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = AgroSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            item(key = "header") {
                HomeHeader(
                    nombre = uiState.nombreAgricultor,
                    tipoUsuario = tipoUsuario,
                    cartItemCount = cartItemCount,
                    onSearch = onSearch,
                    onCarritoClick = onCarritoClick
                )
            }

            if (esAgricultor) {
                if (priceNotifications.isLoading) item(key = "price-loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AgroSpacing.screenHorizontal)
                    )
                }
                priceNotifications.error?.let { error ->
                    item(key = "price-error") {
                        InlineError(
                            message = error,
                            onRetry = onRetryPriceNotifications,
                            modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal)
                        )
                    }
                }
                priceNotifications.notifications.forEach { notification ->
                    item(key = "price-${notification.id}") {
                        PriceDropNotificationAlert(
                            notification,
                            reading = notification.id in priceNotifications.readingIds,
                            onClick = { onPriceNotificationClick(notification) },
                            onRead = { onPriceNotificationRead(notification) }
                        )
                    }
                }
            }

            if (tipoUsuario == "distribuidor") {
                urgentNotification?.let { notification ->
                    item(key = "urgent-notification-${notification.id}") {
                        UrgentOrderNotificationAlert(
                            notification = notification,
                            onClick = { onUrgentNotificationClick(notification) }
                        )
                    }
                }
            }

            item(key = "categories") {
                CategoryTilesSection(
                    categorias = uiState.categorias,
                    isLoading = uiState.isLoadingCategorias,
                    onCategoryClick = onCategoryClick,
                    onVerTodas = onVerTodasCategorias
                )
            }

            if (shouldShowCropCycleCard(tipoUsuario, uiState.cicloRelevante)) {
                uiState.cicloRelevante?.let { cycle ->
                    item(key = "crop-cycle-${cycle.faseActual?.idCiclo}") {
                        CropCycleInfoCard(
                            cycle = cycle,
                            onRecommendedProductClick = onRecommendedProductClick
                        )
                    }
                }
            }

            if (esAgricultor) {
                item(key = "recommended") {
                    RecommendedProductsSection(
                        productos = uiState.productosRecomendados,
                        isLoading = uiState.isLoadingRecomendados,
                        onProductoClick = onProductoClick,
                        favoriteIds = favoriteIds,
                        pendingFavoriteIds = pendingFavoriteIds,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }

            item(key = "featured") {
                FeaturedProductsSection(
                    productos = uiState.productos.take(FEATURED_COUNT),
                    isLoading = uiState.isLoadingProductos,
                    onProductoClick = onProductoClick,
                    onVerMas = onVerMasProductos,
                    showFavoriteAction = esAgricultor,
                    favoriteIds = favoriteIds,
                    pendingFavoriteIds = pendingFavoriteIds,
                    onFavoriteClick = onFavoriteClick
                )
            }

            item(key = "pest-alerts-shortcut") {
                PestAlertsShortcutCard(
                    onClick = onPestAlertsClick,
                    modifier = Modifier.padding(top = AgroSpacing.xs)
                )
            }

            item(key = "distributors") {
                DistributorsSection(
                    distribuidores = uiState.distribuidores,
                    isLoading = uiState.isLoadingDistribuidores,
                    onDistribuidorClick = onDistribuidorClick
                )
            }
        }
    }
}

private const val FEATURED_COUNT = 4

private fun Context.hasHomeLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun HomeHeader(
    nombre: String,
    tipoUsuario: String,
    cartItemCount: Int,
    onSearch: (String) -> Unit,
    onCarritoClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val onHeader = MaterialTheme.agroColors.onTopBar
    var query by rememberSaveable { mutableStateOf("") }
    val greeting = remember { greetingFor(LocalTime.now().hour) }

    val submit: () -> Unit = {
        keyboardController?.hide()
        onSearch(query.trim())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.agroColors.topBarContainer,
                // Esquinas redondeadas solo abajo: separa el encabezado del
                // contenido sin que parezca una tarjeta flotando.
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .padding(
                start = AgroSpacing.screenHorizontal,
                end = AgroSpacing.xs,
                top = AgroSpacing.md,
                bottom = AgroSpacing.lg
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$greeting,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onHeader
                )
                Text(
                    text = nombre.ifBlank { if (tipoUsuario == "distribuidor") "Distribuidor" else "Agricultor" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = onHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Solo los agricultores compran, así que solo ellos ven el carrito.
            if (tipoUsuario != "distribuidor") {
                IconButton(onClick = onCarritoClick) {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) { Text("$cartItemCount") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = if (cartItemCount > 0) {
                                "Carrito, $cartItemCount productos"
                            } else {
                                "Carrito"
                            },
                            tint = onHeader
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AgroSpacing.sm))

        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = AgroSpacing.xs),
            shape = MaterialTheme.shapes.extraLarge,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text("¿Qué producto buscas?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = submit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Buscar")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

/**
 * Categorías como tarjetas con foto. La imagen se reconoce antes de leer la
 * palabra, lo que ayuda a quien lee con dificultad; son las mismas fotos de
 * respaldo que usan los productos sin foto propia.
 */
@Composable
private fun CategoryTilesSection(
    categorias: List<Category>,
    isLoading: Boolean,
    onCategoryClick: (Int) -> Unit,
    onVerTodas: () -> Unit
) {
    if (!isLoading && categorias.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Categorías", actionLabel = "Ver catálogo", onAction = onVerTodas)
        LazyRow(
            contentPadding = PaddingValues(horizontal = AgroSpacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            if (isLoading && categorias.isEmpty()) {
                items(4) {
                    SkeletonBox(
                        modifier = Modifier.size(width = CATEGORY_TILE_WIDTH, height = 112.dp),
                        shape = MaterialTheme.shapes.large
                    )
                }
            } else {
                items(categorias, key = { it.id }) { categoria ->
                    CategoryTile(categoria = categoria, onClick = { onCategoryClick(categoria.id) })
                }
            }
        }
    }
}

private val CATEGORY_TILE_WIDTH = 104.dp

@Composable
private fun CategoryTile(categoria: Category, onClick: () -> Unit) {
    AgroCard(
        onClick = onClick,
        modifier = Modifier
            .width(CATEGORY_TILE_WIDTH)
            .semantics { contentDescription = "Ver ${categoria.nombre}" }
    ) {
        ProductImage(
            imagenUrl = null,
            contentDescription = null,
            categoria = categoria.nombre,
            iconSize = 32.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(categoryBackground(categoria.nombre))
        )
        Text(
            text = categoria.nombre,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AgroSpacing.xs, vertical = AgroSpacing.xs)
        )
    }
}

@Composable
internal fun PestAlertsShortcutCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AgroCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
            .testTag("pest-alerts-shortcut"),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            IconBadge(icon = Icons.Default.BugReport)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alertas de plagas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Consulta reportes cercanos o informa una plaga",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir alertas de plagas",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(AgroSpacing.xs)
                .size(AgroSizes.iconMedium)
        )
    }
}

@Composable
internal fun CropCycleInfoCard(
    cycle: CropCycleResponse,
    modifier: Modifier = Modifier,
    onRecommendedProductClick: (String) -> Unit = {}
) {
    val currentPhase = cycle.faseActual ?: return

    AgroCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.xs)
            .testTag("crop-cycle-card"),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                IconBadge(icon = Icons.Default.Eco)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cropCycleTitle(cycle.cultivo),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Fase actual: ${currentPhase.fase}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = currentPhase.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (currentPhase.productosRecomendados.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Lo que te puede servir en esta fase",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                currentPhase.productosRecomendados.take(3).forEach { product ->
                    AgroTextButton(
                        text = product,
                        icon = Icons.Default.Search,
                        onClick = { onRecommendedProductClick(product) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recommended-product-link")
                            .semantics { contentDescription = "Buscar $product en el catálogo" }
                    )
                }
            }

            cycle.proximaFase?.let { nextPhase ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "Próxima fase: ${nextPhase.fase} · mes ${nextPhase.mesInicio}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AgroSpacing.sm)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CropCycleInfoCardPreview() {
    AgroConectaTheme(darkTheme = false) {
        CropCycleInfoCard(
            cycle = CropCycleResponse(
                cultivo = "maíz",
                mesActual = 8,
                faseActual = CropPhase(
                    idCiclo = 3,
                    fase = "Desarrollo vegetativo",
                    mesInicio = 7,
                    mesFin = 9,
                    descripcion = "Crecimiento activo y monitoreo de nutrición, plagas y enfermedades.",
                    productosRecomendados = listOf(
                        "Fertilizante nitrogenado",
                        "Insecticida para gusano cogollero",
                        "Fungicida agrícola"
                    )
                ),
                fasesActivas = emptyList(),
                proximaFase = CropPhase(
                    idCiclo = 4,
                    fase = "Cosecha y postcosecha",
                    mesInicio = 10,
                    mesFin = 12,
                    descripcion = "Cosecha y almacenamiento.",
                    productosRecomendados = emptyList()
                )
            )
        )
    }
}

@Composable
private fun UrgentOrderNotificationAlert(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val content = notification.contenido

    AgroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(AgroSizes.iconLarge)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pedido urgente",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = content.mensaje ?: "Un agricultor necesita insumos por una plaga",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                content.tipoPlaga?.takeIf { it.isNotBlank() }?.let { pestType ->
                    Text(
                        text = "Plaga detectada: $pestType",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                content.agricultor?.takeIf { it.isNotBlank() }?.let { farmer ->
                    Text(
                        text = "Agricultor: $farmer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Text(
                    text = "Toca para ver los pedidos recibidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(top = AgroSpacing.xxs)
                )
            }
        }
    }
}

@Composable
private fun RecommendedProductsSection(
    productos: List<Product>,
    isLoading: Boolean,
    onProductoClick: (Int) -> Unit,
    favoriteIds: Set<Int>,
    pendingFavoriteIds: Set<Int>,
    onFavoriteClick: (Int) -> Unit
) {
    if (!isLoading && productos.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Recomendados para ti")

        if (isLoading && productos.isEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = AgroSpacing.screenHorizontal)
                    .semantics { contentDescription = "Cargando recomendaciones" },
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                ProductCardSkeleton(Modifier.width(RECOMMENDED_CARD_WIDTH))
                ProductCardSkeleton(Modifier.width(RECOMMENDED_CARD_WIDTH))
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = AgroSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                items(productos, key = { it.id }) { producto ->
                    ProductCard(
                        producto = producto,
                        onClick = { onProductoClick(producto.id) },
                        showFavoriteAction = true,
                        isFavorite = producto.id in favoriteIds,
                        isUpdatingFavorite = producto.id in pendingFavoriteIds,
                        onFavoriteClick = { onFavoriteClick(producto.id) },
                        modifier = Modifier.width(RECOMMENDED_CARD_WIDTH)
                    )
                }
            }
        }
    }
}

private val RECOMMENDED_CARD_WIDTH = 180.dp

@Composable
private fun FeaturedProductsSection(
    productos: List<Product>,
    isLoading: Boolean,
    onProductoClick: (Int) -> Unit,
    onVerMas: () -> Unit,
    showFavoriteAction: Boolean,
    favoriteIds: Set<Int>,
    pendingFavoriteIds: Set<Int>,
    onFavoriteClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Productos destacados", actionLabel = "Ver todos", onAction = onVerMas)
        when {
            isLoading && productos.isEmpty() -> ProductGridSkeleton(
                modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal)
            )

            productos.isEmpty() -> Text(
                text = "Todavía no hay productos publicados. Vuelve pronto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal)
            )

            else -> Column(
                modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                productos.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                    ) {
                        rowItems.forEach { producto ->
                            ProductCard(
                                producto = producto,
                                onClick = { onProductoClick(producto.id) },
                                showFavoriteAction = showFavoriteAction,
                                isFavorite = producto.id in favoriteIds,
                                isUpdatingFavorite = producto.id in pendingFavoriteIds,
                                onFavoriteClick = { onFavoriteClick(producto.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                AgroSecondaryButton(
                    text = "Ver todo el catálogo",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onVerMas,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DistributorsSection(
    distribuidores: List<Distributor>,
    isLoading: Boolean,
    onDistribuidorClick: (Int) -> Unit
) {
    if (!isLoading && distribuidores.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Distribuidores verificados")
        if (isLoading && distribuidores.isEmpty()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = AgroSpacing.screenHorizontal)
                    .semantics { contentDescription = "Cargando distribuidores" },
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                repeat(2) {
                    SkeletonBox(
                        modifier = Modifier
                            .width(DISTRIBUTOR_CARD_WIDTH)
                            .height(120.dp),
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = AgroSpacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                items(distribuidores, key = { it.id }) { dist ->
                    DistribuidorCard(
                        distribuidor = dist,
                        onClick = { onDistribuidorClick(dist.id) }
                    )
                }
            }
        }
    }
}

private val DISTRIBUTOR_CARD_WIDTH = 196.dp

@Composable
internal fun DistribuidorCard(
    distribuidor: Distributor,
    onClick: () -> Unit
) {
    val hasReviews = distribuidor.cantidadResenas?.let { it > 0 }
        ?: ((distribuidor.calificacion ?: 0.0) > 0.0)
    val displayedRating = distribuidor.calificacion.takeIf { hasReviews } ?: 0.0

    AgroCard(
        onClick = onClick,
        modifier = Modifier.width(DISTRIBUTOR_CARD_WIDTH)
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(AgroSizes.iconSmall)
                    )
                }
                Spacer(Modifier.width(AgroSpacing.xs))
                Text(
                    text = distribuidor.nombreNegocio,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (distribuidor.estadoVerificacion == "verificado") {
                StatusTag(text = "Verificado", tone = TagTone.Info, icon = Icons.Default.Verified)
            }
            distribuidor.departamento?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            formatDistributorDistanceKm(distribuidor.distanciaKm)?.let { distance ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Distancia al distribuidor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AgroSpacing.xxs))
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.labelMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(rating = displayedRating)
                if (!hasReviews) {
                    Spacer(modifier = Modifier.width(AgroSpacing.xs))
                    Text(
                        text = "Sin reseñas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

internal fun formatDistributorDistanceKm(distanceKm: Double?): String? =
    distanceKm
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.let { String.format(Locale.US, "%.1f km", it) }

@Composable
internal fun PriceDropNotificationAlert(
    notification: AppNotification,
    reading: Boolean,
    onClick: () -> Unit,
    onRead: () -> Unit
) {
    AgroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.md, top = AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            StatusTag(text = "¡Bajó de precio!", tone = TagTone.Success)
            Text(
                text = notification.contenido.producto.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = priceNotificationText(notification),
                style = MaterialTheme.typography.bodyMedium.tabularNumbers,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Toca para ver el producto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        AgroTextButton(
            text = if (reading) "Marcando…" else "Marcar como leída",
            onClick = onRead,
            enabled = !reading,
            modifier = Modifier.padding(start = AgroSpacing.xs, bottom = AgroSpacing.xxs)
        )
    }
}
