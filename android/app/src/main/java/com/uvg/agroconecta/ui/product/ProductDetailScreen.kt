package com.uvg.agroconecta.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.DistributorCompare
import com.uvg.agroconecta.data.models.Review
import com.uvg.agroconecta.ui.components.DistributorRatingCard
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
    viewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Observables existentes ────────────────────────────────────────────────
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

    // ── Observables de reseñas (NUEVO) ───────────────────────────────────────
    val reviews           by viewModel.reviews.observeAsState(emptyList())
    val reviewsPromedio   by viewModel.reviewsPromedio.observeAsState(null)
    val reviewsLoading    by viewModel.reviewsLoading.observeAsState(false)
    val reviewSubmitState by viewModel.reviewSubmitState.observeAsState(ReviewSubmitState.Idle)

    var comparadorExpanded by remember { mutableStateOf(false) }
    var isFarmer by remember { mutableStateOf(false) }

    // ── Cargar datos al entrar ────────────────────────────────────────────────
    LaunchedEffect(productId) {
        val token = SessionManager.getToken(context).first()
        isFarmer = SessionManager.getTipoUsuario(context).first() == "agricultor"
        viewModel.loadProduct(productId, token)
        viewModel.loadComparison(productId, token)
        if (isFarmer) {
            viewModel.loadFollowStatus(productId, token)
        }
        viewModel.loadReviews(productId, token)   // NUEVO
    }

    LaunchedEffect(selectedOffer?.idDistribuidor) {
        val distributorId = selectedOffer?.idDistribuidor ?: return@LaunchedEffect
        val token = SessionManager.getToken(context).first()
        viewModel.loadDistributorRating(distributorId, token)
    }

    // ── Snackbars ─────────────────────────────────────────────────────────────
    LaunchedEffect(cartSuccess) {
        cartSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
            onAddedToCart()   // <-- NUEVO
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
            TopAppBar(
                title = {
                    Text(
                        text = product?.nombre ?: "Detalle del producto",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        scope.launch {
                            val token = SessionManager.getToken(context).first() ?: return@launch
                            val farmerId = SessionManager.getFarmerId(context).first() ?: -1
                            if (farmerId == -1) { snackbarHostState.showSnackbar("Error: sesión inválida"); return@launch }
                            viewModel.addToCart(farmerId, token)
                        }
                    },
                    enabled = !isAddingToCart && selectedOffer != null,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAddingToCart) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedOffer?.let { "Agregar al carrito — Q${"%.2f".format(it.precio)}" } ?: "Seleccioná un distribuidor",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
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

            // ── Hero ──────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(GreenSurface).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(GreenPale),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Eco, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    p.categoria?.let { cat ->
                        Surface(color = GreenPale, shape = RoundedCornerShape(20.dp)) {
                            Text(text = cat, style = MaterialTheme.typography.labelMedium, color = GreenPrimaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(text = p.nombre, style = MaterialTheme.typography.headlineSmall, color = GrayDark, textAlign = TextAlign.Center)
                    p.marca?.let { marca ->
                        Spacer(Modifier.height(4.dp))
                        Text(text = marca, style = MaterialTheme.typography.bodyMedium, color = GrayMid)
                    }
                    selectedOffer?.let { offer ->
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "Q${"%.2f".format(offer.precio)}", style = MaterialTheme.typography.titleLarge,
                                color = GreenPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Text(text = "/ ${offer.unidadMedida ?: "unidad"}", style = MaterialTheme.typography.bodySmall,
                                color = GrayMid, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }
                    if (isFarmer) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val token = SessionManager.getToken(context).first()
                                    viewModel.toggleFollowPrice(productId, token)
                                }
                            },
                            enabled = !isUpdatingFollow,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isFollowingPrice) GreenPrimaryDark else GreenPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isUpdatingFollow) {
                                CircularProgressIndicator(
                                    color = GreenPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isFollowingPrice) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isFollowingPrice) "Dejar de seguir precio" else "Seguir precio",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // ── Ficha técnica ─────────────────────────────────────────────
            item {
                SectionCard(title = "Ficha técnica") {
                    val hayFicha = !p.descripcion.isNullOrBlank() || !p.composicion.isNullOrBlank()
                            || !p.dosis.isNullOrBlank() || !p.instrucciones.isNullOrBlank()
                    if (!hayFicha) {
                        Text("Este producto aún no tiene ficha técnica completa.", style = MaterialTheme.typography.bodyMedium, color = GrayMid)
                    } else {
                        p.descripcion?.let  { FichaRow("Descripción", it) }
                        p.composicion?.let  { FichaRow("Composición", it) }
                        p.dosis?.let        { FichaRow("Dosis recomendada", it) }
                        p.instrucciones?.let{ FichaRow("Instrucciones de uso", it) }
                    }
                }
            }

            // ── Distribuidores disponibles ────────────────────────────────
            item {
                SectionCard(title = "Distribuidores disponibles") {
                    if (p.ofertas.isEmpty()) {
                        Text("No hay distribuidores disponibles.", style = MaterialTheme.typography.bodyMedium, color = GrayMid)
                    } else {
                        p.ofertas.forEach { offer ->
                            OfertaItem(
                                offer = offer,
                                isSelected = selectedOffer?.idInventario == offer.idInventario,
                                distributorRating = if (selectedOffer?.idInventario == offer.idInventario) distributorRating else null,
                                isLoadingRating = selectedOffer?.idInventario == offer.idInventario && isLoadingDistributorRating,
                                onClick = { viewModel.selectOffer(offer) }
                            )
                            if (offer != p.ofertas.last()) HorizontalDivider(color = GrayLight, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            // ── Comparador de precios ─────────────────────────────────────
            item {
                SectionCard(
                    title = "Comparar precios",
                    trailing = {
                        IconButton(onClick = { comparadorExpanded = !comparadorExpanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (comparadorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (comparadorExpanded) "Colapsar" else "Expandir",
                                tint = GreenPrimary
                            )
                        }
                    }
                ) {
                    AnimatedVisibility(visible = comparadorExpanded) {
                        Column {
                            comparison?.let { cmp ->
                                cmp.precioMasBajo?.let { mejor ->
                                    Surface(color = GreenSurface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Mejor precio: Q${"%.2f".format(mejor)}", style = MaterialTheme.typography.labelLarge, color = GreenPrimary)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                                cmp.distribuidores.forEach { dist ->
                                    DistribuidorCompareItem(dist = dist)
                                    if (dist != cmp.distribuidores.last()) HorizontalDivider(color = GrayLight, modifier = Modifier.padding(vertical = 8.dp))
                                }
                            } ?: Text("Cargando comparación...", style = MaterialTheme.typography.bodyMedium, color = GrayMid)
                        }
                    }
                    if (!comparadorExpanded) {
                        Text(
                            text = comparison?.let { "${it.distribuidores.size} distribuidor(es) disponibles" } ?: "Toca para ver precios de otros distribuidores",
                            style = MaterialTheme.typography.bodySmall, color = GrayMid
                        )
                    }
                }
            }

            // ── Reseñas (NUEVO — KAN-48) ──────────────────────────────────
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

// ── Componentes privados (sin cambios) ────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = GrayDark)
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun FichaRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = GrayMid)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = GrayDark)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun OfertaItem(
    offer: DistributorOffer,
    isSelected: Boolean,
    distributorRating: com.uvg.agroconecta.data.models.DistributorRatingResponse?,
    isLoadingRating: Boolean,
    onClick: () -> Unit
) {
    val esVerificado = offer.estadoVerificacion == "verificado"
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .then(if (isSelected) Modifier.border(2.dp, GreenPrimary, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(onClick = onClick).background(if (isSelected) GreenSurface else Color.Transparent).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = offer.distribuidor, style = MaterialTheme.typography.titleSmall, color = GrayDark)
                if (esVerificado) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Verificado",
                        tint = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("Stock: ${offer.stock} ${offer.unidadMedida ?: "unidades"}", style = MaterialTheme.typography.bodySmall, color = GrayMid)
            if (isSelected) {
                Spacer(Modifier.height(8.dp))
                when {
                    isLoadingRating -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = GreenPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cargando rating del distribuidor...",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMid
                            )
                        }
                    }
                    distributorRating != null && distributorRating.totalResenas > 0 -> {
                        DistributorRatingCard(
                            rating = distributorRating.calificacionPromedio,
                            totalReviews = distributorRating.totalResenas
                        )
                    }
                    else -> {
                        Text(
                            text = "Aún no hay reseñas de este distribuidor",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMid
                        )
                    }
                }
            }
        }
        Text("Q${"%.2f".format(offer.precio)}", style = MaterialTheme.typography.titleMedium, color = GreenPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DistribuidorCompareItem(dist: DistributorCompare) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = dist.nombre, style = MaterialTheme.typography.bodyMedium, color = GrayDark)
                if (dist.esPrecioMasBajo) {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = OrangeLight, shape = RoundedCornerShape(4.dp)) {
                        Text("Mejor precio", style = MaterialTheme.typography.labelSmall, color = OrangeAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Text("Stock: ${dist.stock} ${dist.unidadMedida ?: "unidades"}", style = MaterialTheme.typography.bodySmall, color = GrayMid)
        }
        Text("Q${"%.2f".format(dist.precio)}", style = MaterialTheme.typography.titleSmall,
            color = if (dist.esPrecioMasBajo) GreenPrimary else GrayDark,
            fontWeight = if (dist.esPrecioMasBajo) FontWeight.Bold else FontWeight.Normal)
    }
}
