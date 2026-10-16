package com.uvg.agroconecta.ui.product

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.ListItemSkeleton
import com.uvg.agroconecta.ui.components.SkeletonBox
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.theme.AgroSpacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Detalle de producto. Lo que decide la compra (foto, precio, disponibilidad
 * y a quién comprarle) va arriba; la ficha técnica, la comparación y las
 * reseñas quedan debajo para quien quiera profundizar.
 *
 * La barra de compra solo aparece para agricultores: un distribuidor no
 * puede comprar y antes veía un botón que siempre fallaba.
 */
@Composable
fun ProductDetailScreen(
    productId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    onAddedToCart: () -> Unit = {},
    isFavorite: Boolean = false,
    isUpdatingFavorite: Boolean = false,
    favoriteErrorMessage: String? = null,
    onFavoriteClick: () -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    viewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val product by viewModel.productDetail.observeAsState()
    val comparison by viewModel.comparison.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true)
    val selectedOffer by viewModel.selectedOffer.observeAsState()
    val cartSuccess by viewModel.cartSuccess.observeAsState()
    val isAddingToCart by viewModel.isAddingToCart.observeAsState(false)
    val isFollowingPrice by viewModel.isFollowingPrice.observeAsState(false)
    val isUpdatingFollow by viewModel.isUpdatingFollow.observeAsState(false)
    val followPriceMessage by viewModel.followPriceMessage.observeAsState()
    val distributorRating by viewModel.distributorRating.observeAsState()
    val isLoadingDistributorRating by viewModel.isLoadingDistributorRating.observeAsState(false)
    val error by viewModel.error.observeAsState()

    val reviews by viewModel.reviews.observeAsState(emptyList())
    val reviewsPromedio by viewModel.reviewsPromedio.observeAsState(null)
    val reviewsLoading by viewModel.reviewsLoading.observeAsState(false)
    val reviewSubmitState by viewModel.reviewSubmitState.observeAsState(ReviewSubmitState.Idle)

    var comparadorExpanded by remember { mutableStateOf(false) }
    var isFarmer by remember { mutableStateOf(false) }
    // Se reinicia al cambiar de distribuidor porque cada uno tiene su propio stock.
    var quantity by rememberSaveable(selectedOffer?.idInventario) { mutableIntStateOf(1) }
    var lastAddedQuantity by remember { mutableIntStateOf(1) }

    LaunchedEffect(productId) {
        isFarmer = SessionManager.getTipoUsuario(context).first() == "agricultor"
        viewModel.loadProduct(productId)
        viewModel.loadComparison(productId)
        if (isFarmer) {
            viewModel.loadFollowStatus(productId)
        }
        viewModel.loadReviews(productId)
    }

    LaunchedEffect(selectedOffer?.idDistribuidor) {
        val distributorId = selectedOffer?.idDistribuidor ?: return@LaunchedEffect
        viewModel.loadDistributorRating(distributorId)
    }

    // El aviso trae el atajo al carrito: así comprar varias cosas no obliga a
    // buscar el ícono del carrito arriba cada vez.
    LaunchedEffect(cartSuccess) {
        if (cartSuccess != null) {
            viewModel.clearMessages()
            onAddedToCart()
            val message = if (lastAddedQuantity == 1) {
                "Listo, se agregó al carrito"
            } else {
                "Listo, se agregaron $lastAddedQuantity al carrito"
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Ver carrito",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onNavigateToCart()
        }
    }
    LaunchedEffect(followPriceMessage) {
        followPriceMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProductDetailTopBar(
                title = product?.nombre ?: "Producto",
                onNavigateBack = onNavigateBack,
                onShare = {
                    product?.let { p ->
                        val offer = selectedOffer
                        val text = buildString {
                            append("Te recomiendo este producto en AgroConecta:\n\n")
                            append("📦 ${p.nombre}\n")
                            p.categoria?.let { append("📂 Categoría: $it\n") }
                            p.marca?.let { append("🏷️ Marca: $it\n") }
                            if (offer != null) {
                                append("💰 Precio: ${formatQuetzales(offer.precio)}\n")
                                append("🏪 Distribuidor: ${offer.distribuidor}\n")
                            }
                            append("\n¡Descárgala y cuida mejor tus cultivos!")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Recomendar producto"))
                    }
                },
                onNavigateToCart = onNavigateToCart,
                productId = productId,
                showFavoriteAction = isFarmer,
                isFavorite = isFavorite,
                isUpdatingFavorite = isUpdatingFavorite,
                onFavoriteClick = onFavoriteClick
            )
        },
        bottomBar = {
            if (isFarmer && product != null) {
                AddToCartBar(
                    selectedOffer = selectedOffer,
                    quantity = quantity,
                    onQuantityChange = { quantity = it },
                    isAddingToCart = isAddingToCart,
                    onAddToCart = {
                        scope.launch {
                            val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                            if (farmerId == -1) {
                                snackbarHostState.showSnackbar("Tu sesión se cerró. Vuelve a iniciar sesión para comprar.")
                                return@launch
                            }
                            lastAddedQuantity = quantity
                            viewModel.addToCart(farmerId, quantity)
                        }
                    }
                )
            }
        }
    ) { padding ->

        if (isLoading) {
            ProductDetailSkeleton(Modifier.padding(padding))
            return@Scaffold
        }

        val p = product ?: run {
            ErrorState(
                title = "No pudimos abrir este producto",
                message = "Revisa tu señal e inténtalo otra vez.",
                icon = Icons.Default.Inventory2,
                onRetry = {
                    viewModel.loadProduct(productId)
                    viewModel.loadComparison(productId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AgroSpacing.md)
        ) {
            item(key = "overview") {
                ProductOverviewSection(
                    product = p,
                    selectedOffer = selectedOffer,
                    isFarmer = isFarmer,
                    isFollowingPrice = isFollowingPrice,
                    isUpdatingFollow = isUpdatingFollow,
                    onToggleFollow = { viewModel.toggleFollowPrice(productId) }
                )
            }

            item(key = "offers") {
                DistributorOffersSection(
                    offers = p.ofertas,
                    selectedOffer = selectedOffer,
                    distributorRating = distributorRating,
                    isLoadingRating = isLoadingDistributorRating,
                    onOfferSelected = viewModel::selectOffer
                )
            }

            item(key = "technical-sheet") {
                ProductTechnicalSheetSection(product = p)
            }

            item(key = "comparison") {
                PriceComparisonSection(
                    comparison = comparison,
                    isExpanded = comparadorExpanded,
                    onExpandedChange = { comparadorExpanded = it }
                )
            }

            item(key = "reviews") {
                ReviewsSection(
                    reviews = reviews,
                    promedio = reviewsPromedio,
                    isLoading = reviewsLoading,
                    submitState = reviewSubmitState,
                    onSubmit = { calificacion, comentario ->
                        viewModel.submitReview(productId, calificacion, comentario)
                    },
                    onDismissMsg = { viewModel.clearReviewMessages() }
                )
            }
        }
    }
}

/** Silueta del detalle mientras carga: foto, nombre, precio y una sección. */
@Composable
private fun ProductDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            shape = MaterialTheme.shapes.extraSmall
        )
        Column(
            modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            SkeletonBox(Modifier.fillMaxWidth(0.7f).height(28.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.4f).height(20.dp))
            SkeletonBox(Modifier.width(140.dp).height(40.dp))
            ListItemSkeleton()
        }
    }
}
