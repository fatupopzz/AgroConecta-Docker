package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.ListItemSkeleton
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.favorites.FavoriteButton
import com.uvg.agroconecta.ui.theme.AgroSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PestAlertDetailSheet(
    alert: PestAlert,
    products: List<PestSuggestedProduct>,
    isLoadingProducts: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    showFavoriteAction: Boolean = false,
    favoriteIds: Set<Int> = emptySet(),
    pendingFavoriteIds: Set<Int> = emptySet(),
    onFavoriteClick: (Int) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("pest-alert-detail")
    ) {
        LazyColumn(
            modifier = Modifier
                .testTag("pest-alert-detail-content")
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = AgroSpacing.screenHorizontal,
                end = AgroSpacing.screenHorizontal,
                bottom = AgroSpacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            item(key = "header") {
                DetailHeader(alert = alert, onDismiss = onDismiss)
            }

            item(key = "overview") {
                AlertOverview(alert)
            }

            if (!alert.descripcion.isNullOrBlank()) {
                item(key = "description") {
                    Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
                        Text(
                            text = "Qué se vio",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = alert.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item(key = "products-title") {
                Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "Productos preventivos sugeridos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = AgroSpacing.xs)
                            .semantics { heading() }
                    )
                    Text(
                        text = "Lee la etiqueta del producto y sigue lo que indica el fabricante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                isLoadingProducts -> item(key = "products-loading") {
                    ListItemSkeleton(Modifier.testTag("suggested-products-loading"))
                }
                errorMessage != null -> item(key = "products-error") {
                    ProductsErrorState(message = errorMessage, onRetry = onRetry)
                }
                products.isEmpty() -> item(key = "products-empty") { EmptyProductsState() }
                else -> items(products, key = PestSuggestedProduct::id) { product ->
                    SuggestedProductCard(
                        product = product,
                        showFavoriteAction = showFavoriteAction,
                        isFavorite = product.id in favoriteIds,
                        isUpdatingFavorite = product.id in pendingFavoriteIds,
                        onFavoriteClick = { onFavoriteClick(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(alert: PestAlert, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.padding(AgroSpacing.sm)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AgroSpacing.sm)
        ) {
            Text(
                text = pestTypeDisplayName(alert.pestType),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            StatusTag(text = "Alerta activa", tone = TagTone.Warning)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("dismiss-pest-alert-detail")
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar detalle")
        }
    }
}

@Composable
private fun AlertOverview(alert: PestAlert) {
    AgroCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            DetailValue(icon = Icons.Default.Agriculture, label = "Cultivo afectado", value = alert.cultivo)
            DetailValue(icon = Icons.Default.LocationOn, label = "Distancia", value = formatDistance(alert.distanceKm))
            DetailValue(
                icon = Icons.Default.CalendarToday,
                label = "Fecha del reporte",
                value = formatAlertDate(alert.reportedAt)
            )
        }
    }
}

@Composable
private fun DetailValue(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(AgroSpacing.xxs)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProductsErrorState(message: String, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            AgroSecondaryButton(
                text = "Reintentar",
                icon = Icons.Default.Refresh,
                onClick = onRetry,
                modifier = Modifier.testTag("retry-suggested-products")
            )
        }
    }
}

@Composable
private fun EmptyProductsState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AgroSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Por ahora no hay productos sugeridos para esta plaga.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuggestedProductCard(
    product: PestSuggestedProduct,
    showFavoriteAction: Boolean,
    isFavorite: Boolean,
    isUpdatingFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("suggested-product-${product.id}")
    ) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = AgroSpacing.xxs)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    product.marca?.takeIf(String::isNotBlank)?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showFavoriteAction) {
                    FavoriteButton(
                        productId = product.id,
                        isFavorite = isFavorite,
                        isPending = isUpdatingFavorite,
                        onClick = onFavoriteClick,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                }
            }

            product.descripcion?.takeIf(String::isNotBlank)?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            ProductAttribute(label = "Composición", value = product.composicion)
            ProductAttribute(label = "Dosis recomendada", value = product.recommendedDose)
            ProductAttribute(label = "Categoría", value = product.categoria)
        }
    }
}

@Composable
private fun ProductAttribute(label: String, value: String?) {
    value?.takeIf(String::isNotBlank)?.let {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xxs),
            modifier = Modifier.semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
