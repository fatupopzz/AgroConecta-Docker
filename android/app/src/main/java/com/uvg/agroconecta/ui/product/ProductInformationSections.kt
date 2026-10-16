package com.uvg.agroconecta.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.ProductDetail
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.QuantityStepper
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.categoryBackground
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.favorites.FavoriteButton
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers

@Composable
internal fun ProductDetailTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onNavigateToCart: () -> Unit,
    productId: Int,
    showFavoriteAction: Boolean = false,
    isFavorite: Boolean = false,
    isUpdatingFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    AgroTopAppBar(
        title = title,
        onBack = onNavigateBack,
        actions = {
            if (showFavoriteAction) {
                FavoriteButton(
                    productId = productId,
                    isFavorite = isFavorite,
                    isPending = isUpdatingFavorite,
                    onClick = onFavoriteClick,
                    containerColor = Color.Transparent,
                    inactiveTint = MaterialTheme.agroColors.onTopBar
                )
            }
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Recomendar a alguien")
            }
            IconButton(onClick = onNavigateToCart) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Ver carrito")
            }
        }
    )
}

/**
 * Barra fija con la cantidad y el botón de compra, al alcance del pulgar. El
 * botón muestra el total ya multiplicado para que no haya que hacer cuentas.
 * La cantidad no puede pasar de lo que el distribuidor tiene disponible.
 */
@Composable
internal fun AddToCartBar(
    selectedOffer: DistributorOffer?,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    isAddingToCart: Boolean,
    onAddToCart: () -> Unit
) {
    val stock = selectedOffer?.stock ?: 0
    val hasStock = stock > 0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            if (selectedOffer != null && hasStock) {
                QuantityStepper(
                    quantity = quantity,
                    onDecrease = { onQuantityChange((quantity - 1).coerceAtLeast(1)) },
                    onIncrease = { onQuantityChange((quantity + 1).coerceAtMost(stock)) },
                    maxQuantity = stock,
                    enabled = !isAddingToCart
                )
            }
            AgroPrimaryButton(
                text = when {
                    selectedOffer == null -> "Elige un distribuidor"
                    !hasStock -> "Sin existencias"
                    else -> "Agregar · ${formatQuetzales(selectedOffer.precio * quantity)}"
                },
                loadingText = "Agregando…",
                icon = Icons.Default.AddShoppingCart,
                onClick = onAddToCart,
                enabled = selectedOffer != null && hasStock,
                loading = isAddingToCart,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun ProductOverviewSection(
    product: ProductDetail,
    selectedOffer: DistributorOffer?,
    isFarmer: Boolean,
    isFollowingPrice: Boolean,
    isUpdatingFollow: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProductImageGallery(product)
        Column(
            modifier = Modifier.padding(
                horizontal = AgroSpacing.screenHorizontal,
                vertical = AgroSpacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            product.categoria?.let { category -> StatusTag(text = category) }
            Text(
                text = product.nombre,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            product.marca?.let { brand ->
                Text(
                    text = brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val offer = selectedOffer
            if (offer != null) {
                Spacer(Modifier.height(AgroSpacing.xs))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatQuetzales(offer.precio),
                        style = MaterialTheme.typography.displaySmall.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(AgroSpacing.xs))
                    Text(
                        text = "por ${offer.unidadMedida ?: "unidad"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = AgroSpacing.xxs)
                    )
                }
                AvailabilityTag(offer)
                Text(
                    text = "Vende: ${offer.distribuidor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (product.ofertas.isEmpty()) {
                Spacer(Modifier.height(AgroSpacing.xs))
                StatusTag(text = "Agotado por ahora", tone = TagTone.Danger)
            }
            if (isFarmer) {
                Spacer(Modifier.height(AgroSpacing.xs))
                AgroSecondaryButton(
                    text = if (isFollowingPrice) "Dejar de avisarme del precio" else "Avisarme si baja el precio",
                    icon = if (isFollowingPrice) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    onClick = onToggleFollow,
                    loading = isUpdatingFollow,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Disponibilidad en palabras y con tono: "Quedan pocos" avisa antes de que
 * la persona arme un pedido grande que el distribuidor no puede cubrir.
 */
@Composable
private fun AvailabilityTag(offer: DistributorOffer) {
    val unit = offer.unidadMedida ?: "unidades"
    val (text, tone) = when {
        offer.stock <= 0 -> "Sin existencias" to TagTone.Danger
        offer.stock <= LOW_STOCK_THRESHOLD -> "Quedan pocos: ${offer.stock} $unit" to TagTone.Warning
        else -> "Disponible: ${offer.stock} $unit" to TagTone.Success
    }
    StatusTag(text = text, tone = tone, modifier = Modifier.padding(vertical = AgroSpacing.xxs))
}

private const val LOW_STOCK_THRESHOLD = 5

/** Foto a todo el ancho: en el detalle es lo que más ayuda a reconocer el producto. */
@Composable
internal fun ProductImageGallery(product: ProductDetail) {
    ProductImage(
        imagenUrl = product.imagenUrl,
        contentDescription = "Foto de ${product.nombre}",
        categoria = product.categoria,
        iconSize = 64.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(categoryBackground(product.categoria))
    )
}

@Composable
internal fun ProductTechnicalSheetSection(product: ProductDetail) {
    ProductSectionCard(title = "Cómo usarlo") {
        val hasTechnicalSheet = !product.descripcion.isNullOrBlank() ||
            !product.composicion.isNullOrBlank() ||
            !product.dosis.isNullOrBlank() ||
            !product.instrucciones.isNullOrBlank()

        if (!hasTechnicalSheet) {
            Text(
                text = "El distribuidor todavía no agregó la información de uso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            product.descripcion?.let { TechnicalSheetRow("Descripción", it) }
            product.composicion?.let { TechnicalSheetRow("Composición", it) }
            product.dosis?.let { TechnicalSheetRow("Dosis recomendada", it) }
            product.instrucciones?.let { TechnicalSheetRow("Instrucciones de uso", it) }
        }
    }
}

@Composable
internal fun ProductSectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.xs)
    ) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                trailing?.invoke()
            }
            Spacer(Modifier.height(AgroSpacing.sm))
            content()
        }
    }
}

@Composable
private fun TechnicalSheetRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = AgroSpacing.sm)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
