package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.agroColors
import java.util.Locale
import kotlin.math.roundToInt

internal enum class StarFill {
    FULL,
    HALF,
    EMPTY
}

internal fun starFillsFor(rating: Double?): List<StarFill> {
    val normalizedRating = rating
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0.0, 5.0)
        ?: 0.0
    val halfSteps = (normalizedRating * 2).roundToInt()

    return List(5) { index ->
        when {
            halfSteps >= (index + 1) * 2 -> StarFill.FULL
            halfSteps == index * 2 + 1 -> StarFill.HALF
            else -> StarFill.EMPTY
        }
    }
}

internal fun starRatingDescription(rating: Double?): String {
    val value = rating?.takeIf { it.isFinite() }?.coerceIn(0.0, 5.0) ?: 0.0
    return "Calificación ${String.format(Locale.US, "%.1f", value)} de 5"
}

/**
 * Cinco estrellas de solo lectura. Para TalkBack se leen como una sola frase
 * ("Calificación 4.5 de 5") en lugar de cinco íconos sin nombre.
 *
 * Las vacías usan `outline` y no un gris muy claro: al sol no se distinguían
 * del fondo y una calificación de 2 parecía de 5.
 */
@Composable
internal fun StarRating(
    rating: Double?,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp
) {
    val filledColor = MaterialTheme.agroColors.star
    val emptyColor = MaterialTheme.colorScheme.outline
    val description = starRatingDescription(rating)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        starFillsFor(rating).forEach { fill ->
            Icon(
                imageVector = when (fill) {
                    StarFill.FULL -> Icons.Filled.Star
                    StarFill.HALF -> Icons.AutoMirrored.Filled.StarHalf
                    StarFill.EMPTY -> Icons.Outlined.StarBorder
                },
                contentDescription = null,
                tint = if (fill == StarFill.EMPTY) emptyColor else filledColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}
