package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.ui.favorites.FavoriteButton
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.util.Locale

/**
 * Tarjeta de producto para catálogo, Home y favoritos.
 *
 * Toda la tarjeta abre el detalle, incluso sin existencias: antes se
 * desactivaba y la persona tocaba sin respuesta. Desde el detalle puede seguir
 * el precio o ver otros distribuidores.
 *
 * El precio va grande y en verde porque es lo primero que se compara. El
 * texto completo de la tarjeta se anuncia de una vez en TalkBack; el corazón
 * queda aparte como su propio botón.
 */
@Composable
fun ProductCard(
    producto: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavoriteAction: Boolean = false,
    isFavorite: Boolean = false,
    isUpdatingFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    val price = producto.precioDesde?.takeIf { it > 0.0 }
    val priceText = price?.let(::formatQuetzales)
    val summary = buildString {
        append(producto.nombre)
        producto.categoria?.let { append(", $it") }
        append(", ")
        append(if (priceText != null) "desde $priceText" else "agotado")
    }

    // La descripción va en la tarjeta (que ya agrupa su contenido) para que
    // TalkBack lea el resumen y anuncie a la vez que se puede tocar.
    AgroCard(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = summary }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(categoryBackground(producto.categoria))
        ) {
            // Sin descripción propia: el resumen de la tarjeta ya nombra el producto.
            ProductImage(
                imagenUrl = producto.imagenUrl,
                imagenThumbUrl = producto.imagenThumbUrl,
                contentDescription = null,
                categoria = producto.categoria,
                modifier = Modifier.matchParentSize()
            )
            if (price == null) {
                StatusTag(
                    text = "Agotado",
                    tone = TagTone.Danger,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(AgroSpacing.xs)
                )
            }
            if (showFavoriteAction) {
                FavoriteButton(
                    productId = producto.id,
                    isFavorite = isFavorite,
                    isPending = isUpdatingFavorite,
                    onClick = onFavoriteClick,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
        Column(modifier = Modifier.padding(AgroSpacing.sm)) {
            producto.categoria?.let { categoria ->
                Text(
                    text = categoria,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Spacer(Modifier.height(AgroSpacing.xs))
            if (priceText != null) {
                Text(
                    text = "desde",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleLarge.tabularNumbers,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Sin existencias",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * "Q1,250.00": el formato de facturas y comercios en Guatemala. Se fija el
 * Locale para que un teléfono configurado en otro idioma no muestre "1.250,00".
 */
fun formatQuetzales(amount: Double): String =
    "Q" + String.format(Locale.US, "%,.2f", amount)
