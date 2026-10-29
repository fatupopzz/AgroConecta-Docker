package com.uvg.agroconecta.ui.distributor

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.SectionHeader
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StarRating
import com.uvg.agroconecta.ui.components.StarRatingInput
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.categoryBackground
import com.uvg.agroconecta.ui.components.formatFriendlyDate
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.favorites.FavoriteButton
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.util.Locale

private const val INITIAL_PRODUCTS_SHOWN = 5
private const val MAX_REVIEW_LENGTH = 300

@Composable
fun DistributorProfileScreen(
    distributorId: Int,
    onNavigateBack: () -> Unit,
    onProductoClick: (Int) -> Unit,
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    favoriteErrorMessage: String? = null,
    onFavoriteClick: (Int) -> Unit = {},
    onFavoriteErrorShown: () -> Unit = {},
    viewModel: DistributorViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    var mostrarTodosProductos by remember { mutableStateOf(false) }

    LaunchedEffect(distributorId) {
        viewModel.loadAll(distributorId)
    }

    val haptics = rememberAgroHaptics()
    LaunchedEffect(uiState.reviewSubmitSuccess) {
        if (uiState.reviewSubmitSuccess) {
            haptics.confirm()
            snackbarHostState.showSnackbar("¡Gracias! Tu reseña se publicó.")
            viewModel.clearReviewMessages()
        }
    }

    LaunchedEffect(uiState.reviewSubmitError) {
        uiState.reviewSubmitError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearReviewMessages()
        }
    }

    LaunchedEffect(favoriteErrorMessage) {
        favoriteErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onFavoriteErrorShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AgroTopAppBar(
                title = uiState.distributorName.ifBlank { "Distribuidor" },
                onBack = onNavigateBack
            )
        }
    ) { padding ->

        if (uiState.isLoading) {
            SkeletonList(
                count = 4,
                description = "Cargando distribuidor",
                modifier = Modifier
                    .padding(padding)
                    .padding(AgroSpacing.screenHorizontal)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AgroSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            item(key = "header") {
                DistributorHeader(
                    name = uiState.distributorName,
                    isVerified = uiState.isVerified,
                    averageRating = uiState.rating?.calificacionPromedio,
                    totalReviews = uiState.rating?.totalResenas ?: uiState.reviews.size
                )
            }

            item(key = "products-title") { SectionHeader(title = "Productos disponibles") }

            if (uiState.productos.isEmpty()) {
                item(key = "products-empty") {
                    Text(
                        text = "Este distribuidor todavía no publica productos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal)
                    )
                }
            } else {
                val productosAMostrar = if (mostrarTodosProductos) {
                    uiState.productos
                } else {
                    uiState.productos.take(INITIAL_PRODUCTS_SHOWN)
                }

                items(productosAMostrar, key = { it.id }) { producto ->
                    ProductoDistribuidorCard(
                        producto = producto,
                        onClick = { onProductoClick(producto.id) },
                        showFavoriteAction = showFavoriteAction,
                        isFavorite = producto.id in favoriteIds,
                        isUpdatingFavorite = producto.id in pendingFavoriteIds,
                        onFavoriteClick = { onFavoriteClick(producto.id) }
                    )
                }

                if (uiState.productos.size > INITIAL_PRODUCTS_SHOWN) {
                    item(key = "products-toggle") {
                        val hidden = uiState.productos.size - INITIAL_PRODUCTS_SHOWN
                        AgroTextButton(
                            text = if (mostrarTodosProductos) "Ver menos" else "Ver $hidden productos más",
                            icon = if (mostrarTodosProductos) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            onClick = { mostrarTodosProductos = !mostrarTodosProductos },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AgroSpacing.screenHorizontal)
                        )
                    }
                }
            }

            item(key = "review-form") {
                SectionHeader(title = "Deja tu reseña")
                ReviewFormCard(
                    isSubmitting = uiState.isSubmittingReview,
                    onSubmit = { calificacion, comentario ->
                        viewModel.submitReview(distributorId, calificacion, comentario)
                    }
                )
            }

            item(key = "reviews-title") {
                SectionHeader(title = "Reseñas (${uiState.rating?.totalResenas ?: uiState.reviews.size})")
            }

            if (uiState.reviews.isEmpty()) {
                item(key = "reviews-empty") {
                    Text(
                        text = "Todavía nadie ha opinado. ¡Sé la primera persona!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AgroSpacing.screenHorizontal)
                    )
                }
            } else {
                items(uiState.reviews, key = { it.idResena }) { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }
}

@Composable
private fun DistributorHeader(
    name: String,
    isVerified: Boolean,
    averageRating: Double?,
    totalReviews: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(AgroSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "D",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isVerified) {
            StatusTag(text = "Distribuidor verificado", tone = TagTone.Info, icon = Icons.Default.Verified)
        }
        if (averageRating != null && totalReviews > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                StarRating(rating = averageRating, starSize = 20.dp)
                Spacer(Modifier.width(AgroSpacing.xs))
                Text(
                    text = String.format(Locale.US, "%.1f", averageRating),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(AgroSpacing.xxs))
                Text(
                    text = if (totalReviews == 1) "(1 reseña)" else "($totalReviews reseñas)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProductoDistribuidorCard(
    producto: Product,
    onClick: () -> Unit,
    showFavoriteAction: Boolean,
    isFavorite: Boolean,
    isUpdatingFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    AgroCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imagenUrl = producto.imagenUrl,
                contentDescription = null,
                categoria = producto.categoria,
                iconSize = 28.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(categoryBackground(producto.categoria))
            )
            Spacer(Modifier.width(AgroSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                producto.categoria?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                producto.precioDesde?.let { precio ->
                    Text(
                        text = formatQuetzales(precio),
                        style = MaterialTheme.typography.titleMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (showFavoriteAction) {
                FavoriteButton(
                    productId = producto.id,
                    isFavorite = isFavorite,
                    isPending = isUpdatingFavorite,
                    onClick = onFavoriteClick,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            }
        }
    }
}

@Composable
private fun ReviewFormCard(
    isSubmitting: Boolean,
    onSubmit: (calificacion: Int, comentario: String) -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
    ) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            StarRatingInput(selected = selectedStars, onSelect = { selectedStars = it })
            Spacer(Modifier.height(AgroSpacing.xs))
            AgroTextField(
                value = comentario,
                onValueChange = { if (it.length <= MAX_REVIEW_LENGTH) comentario = it },
                label = "Comentario (opcional)",
                placeholder = "¿Cómo te atendió este distribuidor?",
                supportingText = "${comentario.length}/$MAX_REVIEW_LENGTH",
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )
            Spacer(Modifier.height(AgroSpacing.xs))
            AgroPrimaryButton(
                text = if (selectedStars == 0) "Elige las estrellas para enviar" else "Enviar reseña",
                loadingText = "Enviando…",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = {
                    if (selectedStars > 0) {
                        onSubmit(selectedStars, comentario.trim())
                        selectedStars = 0
                        comentario = ""
                    }
                },
                enabled = selectedStars > 0,
                loading = isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ReviewCard(review: DistributorReview) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal)
    ) {
        Row(
            modifier = Modifier
                .padding(AgroSpacing.md)
                .semantics(mergeDescendants = true) {}
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = review.agricultorNombre?.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(AgroSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.agricultorNombre ?: "Agricultor",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                formatFriendlyDate(review.fechaResena)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(AgroSpacing.xxs))
                StarRating(rating = review.calificacion.toDouble())
                review.comentario?.takeIf { it.isNotBlank() }?.let { com ->
                    Spacer(Modifier.height(AgroSpacing.xxs))
                    Text(
                        text = com,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                review.productoNombre?.let { prod ->
                    Spacer(Modifier.height(AgroSpacing.xxs))
                    Text(
                        text = "Compró: $prod",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
