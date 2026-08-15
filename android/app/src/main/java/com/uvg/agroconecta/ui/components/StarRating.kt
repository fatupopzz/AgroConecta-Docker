package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

@Composable
internal fun StarRating(
    rating: Double?,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp
) {
    Row(
        modifier = modifier,
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
                tint = if (fill == StarFill.EMPTY) Color(0xFFDDDDDD) else Color(0xFFFFC107),
                modifier = Modifier.size(starSize)
            )
        }
    }
}
