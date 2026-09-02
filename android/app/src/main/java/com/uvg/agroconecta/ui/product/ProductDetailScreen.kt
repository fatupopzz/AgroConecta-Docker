package com.uvg.agroconecta.ui.product

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    onAddedToCart: () -> Unit = {},
    viewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val product       by viewModel.productDetail.observeAsState()
    val comparison    by viewModel.comparison.observeAsState()
    val isLoading     by viewModel.isLoading.observeAsState(true)
    val selectedOffer by viewModel.selectedOffer.observeAsState()
    val cartSuccess   by viewModel.cartSuccess.observeAsState()
    val isAddingToCart by viewModel.isAddingToCart.observeAsState(false)
    val isFollowingPrice by viewModel.isFollowingPrice.observeAsState(false)
    val isUpdatingFollow by viewModel.isUpdatingFollow.observeAsState(false)
    val followPriceMessage by viewModel.followPriceMessage.observeAsState()
    val distributorRating by viewModel.distributorRating.observeAsState()
    val isLoadingDistributorRating by viewModel.isLoadingDistributorRating.observeAsState(false)
    val error         by viewModel.error.observeAsState()

    val reviews           by viewModel.reviews.observeAsState(emptyList())
    val reviewsPromedio   by viewModel.reviewsPromedio.observeAsState(null)
    val reviewsLoading    by viewModel.reviewsLoading.observeAsState(false)
    val reviewSubmitState by viewModel.reviewSubmitState.observeAsState(ReviewSubmitState.Idle)

    var comparadorExpanded by remember { mutableStateOf(false) }
    var isFarmer by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        val token = SessionManager.getToken(context).first()
        isFarmer = SessionManager.getTipoUsuario(context).first() == "agricultor"
        viewModel.loadProduct(productId, token)
        viewModel.loadComparison(productId, token)
        if (isFarmer) {
            viewModel.loadFollowStatus(productId, token)
        }
        viewModel.loadReviews(productId, token)
    }

    LaunchedEffect(selectedOffer?.idDistribuidor) {
        val distributorId = selectedOffer?.idDistribuidor ?: return@LaunchedEffect
        val token = SessionManager.getToken(context).first()
        viewModel.loadDistributorRating(distributorId, token)
    }

    LaunchedEffect(cartSuccess) {
        cartSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onAddedToCart()
        }
    }
    LaunchedEffect(followPriceMessage) {
        followPriceMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProductDetailTopBar(
                title = product?.nombre ?: "Detalle del producto",
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
                                append("💰 Precio: Q${"%.2f".format(offer.precio)}\n")
                                append("🏪 Distribuidor: ${offer.distribuidor}\n")
                            }
                            append("\n¡Descárgala y cuida mejor tus cultivos!")
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Recomendar producto")
                        )
                    }
                },
                onNavigateToCart = onNavigateToCart
            )
        },
        bottomBar = {
            AddToCartBar(
                selectedOffer = selectedOffer,
                isAddingToCart = isAddingToCart,
                onAddToCart = {
                    scope.launch {
                        val token = SessionManager.getToken(context).first() ?: return@launch
                        val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                        if (farmerId == -1) {
                            snackbarHostState.showSnackbar("Error: sesión inválida")
                            return@launch
                        }
                        viewModel.addToCart(farmerId, token)
                    }
                }
            )
        }
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
            return@Scaffold
        }

        val p = product ?: run {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar el producto", color = GrayMid)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {

            item {
                ProductOverviewSection(
                    product = p,
                    selectedOffer = selectedOffer,
                    isFarmer = isFarmer,
                    isFollowingPrice = isFollowingPrice,
                    isUpdatingFollow = isUpdatingFollow,
                    onToggleFollow = {
                        scope.launch {
                            val token = SessionManager.getToken(context).first()
                            viewModel.toggleFollowPrice(productId, token)
                        }
                    }
                )
            }

            item {
                ProductTechnicalSheetSection(product = p)
            }

            item {
                DistributorOffersSection(
                    offers = p.ofertas,
                    selectedOffer = selectedOffer,
                    distributorRating = distributorRating,
                    isLoadingRating = isLoadingDistributorRating,
                    onOfferSelected = viewModel::selectOffer
                )
            }

            item {
                PriceComparisonSection(
                    comparison = comparison,
                    isExpanded = comparadorExpanded,
                    onExpandedChange = { comparadorExpanded = it }
                )
            }

            item {
                ReviewsSection(
                    reviews           = reviews,
                    promedio          = reviewsPromedio,
                    isLoading         = reviewsLoading,
                    submitState       = reviewSubmitState,
                    onSubmit          = { calificacion, comentario ->
                        scope.launch {
                            val token = SessionManager.getToken(context).first()
                            viewModel.submitReview(productId, calificacion, comentario, token)
                        }
                    },
                    onDismissMsg      = { viewModel.clearReviewMessages() }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
