package com.uvg.agroconecta.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.ProductCard
import com.uvg.agroconecta.ui.components.ProductGridSkeleton
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.friendlyErrorMessage
import com.uvg.agroconecta.ui.theme.AgroSpacing
import kotlinx.coroutines.launch

/**
 * Productos guardados con el corazón.
 *
 * Quitar uno se puede deshacer desde el aviso. Basta con volver a marcarlo,
 * porque el ViewModel guarda la lista completa y el producto reaparece en su
 * lugar.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val scope = rememberCoroutineScope()

    // Con productos en pantalla el error va en un aviso breve; sin productos
    // se muestra a pantalla completa con su botón de reintentar.
    LaunchedEffect(uiState.errorMessage, uiState.favoriteProducts.isNotEmpty()) {
        val message = uiState.errorMessage
        if (message != null && uiState.favoriteProducts.isNotEmpty()) {
            snackbarHostState.showSnackbar(friendlyErrorMessage(message))
            onErrorShown()
        }
    }

    fun removeWithUndo(product: Product) {
        onToggleFavorite(product.id)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Quitaste ${product.nombre} de favoritos",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onToggleFavorite(product.id)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("favorites-screen"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AgroTopAppBar(title = "Mis favoritos", onBack = onNavigateBack) }
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.favoriteProducts.isEmpty() -> {
                    ProductGridSkeleton(
                        rows = 2,
                        modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                    )
                }

                uiState.errorMessage != null && uiState.favoriteProducts.isEmpty() -> {
                    RefreshableFullScreen {
                        ErrorState(
                            title = "No pudimos cargar tus favoritos",
                            message = uiState.errorMessage,
                            onRetry = onRetry,
                            modifier = Modifier.testTag("favorites-error-state")
                        )
                    }
                }

                uiState.favoriteProducts.isEmpty() -> {
                    RefreshableFullScreen {
                        EmptyState(
                            icon = Icons.Default.FavoriteBorder,
                            title = "Todavía no tienes favoritos",
                            message = "Toca el corazón de un producto para guardarlo aquí.",
                            modifier = Modifier.testTag("favorites-empty-state")
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        // Igual que el catálogo: dos columnas en teléfonos y más en
                        // pantallas anchas, sin filas a medio llenar que salten al quitar uno.
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 156.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                        ) {
                            items(uiState.favoriteProducts, key = { it.id }) { product ->
                                val isFavorite = product.id in uiState.favoriteIds
                                ProductCard(
                                    producto = product,
                                    onClick = { onProductClick(product.id) },
                                    showFavoriteAction = true,
                                    isFavorite = isFavorite,
                                    isUpdatingFavorite = product.id in uiState.pendingProductIds,
                                    onFavoriteClick = {
                                        if (isFavorite) removeWithUndo(product) else onToggleFavorite(product.id)
                                    },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
