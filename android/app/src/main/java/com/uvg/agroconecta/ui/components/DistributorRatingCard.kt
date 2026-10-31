package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSpacing
import java.util.Locale

/**
 * Resumen de la calificación de un distribuidor. Antes las estrellas latían en
 * un bucle infinito: distraía, gastaba batería y forzaba a redibujar todo el
 * tiempo en teléfonos lentos, así que ahora son estáticas.
 */
@Composable
fun DistributorRatingCard(
    rating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier
) {
    val ratingText = String.format(Locale.US, "%.1f", rating)
    val reviewsText = if (totalReviews == 1) "1 reseña" else "$totalReviews reseñas"

    Surface(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Calificación $ratingText de 5, $reviewsText"
        },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AgroSpacing.sm, vertical = AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarRating(rating = rating, starSize = 20.dp)
            Spacer(Modifier.width(AgroSpacing.xs))
            Text(
                text = ratingText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(AgroSpacing.xxs))
            Text(
                text = "($reviewsText)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
