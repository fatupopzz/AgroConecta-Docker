package com.uvg.agroconecta.ui.pestalerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.PestAlert
import com.uvg.agroconecta.data.models.PestSuggestedProduct
import com.uvg.agroconecta.ui.favorites.FavoriteButton
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayDark
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface

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
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("pest-alert-detail")
    ) {
        LazyColumn(
            modifier = Modifier.testTag("pest-alert-detail-content"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                DetailHeader(alert = alert, onDismiss = onDismiss)
            }

            item {
                AlertOverview(alert)
            }

            if (!alert.descripcion.isNullOrBlank()) {
                item {
                    DetailSection(title = "Descripción") {
                        Text(
                            text = alert.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayDark
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = GrayLight)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Productos preventivos sugeridos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Consulta la etiqueta del producto y sigue las indicaciones del fabricante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid
                )
            }

            when {
                isLoadingProducts -> item { ProductsLoadingState() }
                errorMessage != null -> item {
                    ProductsErrorState(message = errorMessage, onRetry = onRetry)
                }
                products.isEmpty() -> item { EmptyProductsState() }
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
        Surface(color = GreenSurface, shape = MaterialTheme.shapes.medium) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.padding(10.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = pestTypeDisplayName(alert.pestType),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Alerta activa",
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimary
            )
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
    Card(
        colors = CardDefaults.cardColors(containerColor = GreenSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DetailValue(
                icon = Icons.Default.Agriculture,
                label = "Cultivo afectado",
                value = alert.cultivo
            )
            DetailValue(
                icon = Icons.Default.LocationOn,
                label = "Distancia",
                value = formatDistance(alert.distanceKm)
            )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = GrayMid)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun ProductsLoadingState() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
            .testTag("suggested-products-loading"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = GreenPrimary)
        Text(
            text = "Buscando recomendaciones…",
            modifier = Modifier.padding(start = 12.dp),
            color = GrayMid
        )
    }
}

@Composable
private fun ProductsErrorState(message: String, onRetry: () -> Unit) {
    Surface(
        color = Color(0xFFFFEDEA),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry, modifier = Modifier.testTag("retry-suggested-products")) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Reintentar", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptyProductsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = GrayMid,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "No hay productos sugeridos para esta alerta",
            style = MaterialTheme.typography.bodyMedium,
            color = GrayMid
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, GrayLight),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("suggested-product-${product.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    product.marca?.takeIf(String::isNotBlank)?.let { brand ->
                        Text(text = brand, style = MaterialTheme.typography.bodySmall, color = GrayMid)
                    }
                }
                if (showFavoriteAction) {
                    FavoriteButton(
                        productId = product.id,
                        isFavorite = isFavorite,
                        isPending = isUpdatingFavorite,
                        onClick = onFavoriteClick,
                        containerColor = GreenSurface
                    )
                }
            }

            product.descripcion?.takeIf(String::isNotBlank)?.let { description ->
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = GrayDark)
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
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = GrayDark)
        }
    }
}
