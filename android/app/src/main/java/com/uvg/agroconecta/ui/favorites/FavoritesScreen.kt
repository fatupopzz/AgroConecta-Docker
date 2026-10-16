package com.uvg.agroconecta.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.ProductCard
import com.uvg.agroconecta.ui.components.ProductGridSkeleton
import com.uvg.agroconecta.ui.theme.AgroSpacing

@Composable
fun FavoritesScreen(
    uiState: FavoriteUiState,
    onNavigateBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Con productos en pantalla el error va en un aviso breve; sin productos
    // se muestra a pantalla completa con su botón de reintentar.
    LaunchedEffect(uiState.errorMessage, uiState.favoriteProducts.isNotEmpty()) {
        val message = uiState.errorMessage
        if (message != null && uiState.favoriteProducts.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.testTag("favorites-screen"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Mis favoritos", onBack = onNavigateBack) }
    ) { padding ->
        when {
            uiState.isLoading && uiState.favoriteProducts.isEmpty() -> {
                ProductGridSkeleton(
                    rows = 2,
                    modifier = Modifier
                        .padding(padding)
                        .padding(AgroSpacing.screenHorizontal)
                )
            }

            uiState.errorMessage != null && uiState.favoriteProducts.isEmpty() -> {
                ErrorState(
                    title = "No pudimos cargar tus favoritos",
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("favorites-error-state")
                )
            }

            uiState.favoriteProducts.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.FavoriteBorder,
                    title = "Todavía no tienes favoritos",
                    message = "Toca el corazón de un producto para guardarlo aquí.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("favorites-empty-state")
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                    ) {
                        items(
                            items = uiState.favoriteProducts.chunked(2),
                            key = { row -> row.joinToString(separator = "-") { it.id.toString() } }
                        ) { products ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                                verticalAlignment = Alignment.Top
                            ) {
                                products.forEach { product ->
                                    ProductCard(
                                        producto = product,
                                        onClick = { onProductClick(product.id) },
                                        showFavoriteAction = true,
                                        isFavorite = product.id in uiState.favoriteIds,
                                        isUpdatingFavorite = product.id in uiState.pendingProductIds,
                                        onFavoriteClick = { onToggleFavorite(product.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (products.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
