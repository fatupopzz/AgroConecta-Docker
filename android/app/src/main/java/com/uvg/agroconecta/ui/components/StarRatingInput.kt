package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors

internal fun starRatingLabel(stars: Int): String = when (stars) {
    1 -> "Muy malo"
    2 -> "Malo"
    3 -> "Regular"
    4 -> "Bueno"
    5 -> "Excelente"
    else -> ""
}

/**
 * Selector de 1 a 5 estrellas para dejar una reseña. Cada estrella ocupa los
 * 48 dp táctiles (antes eran 32 y era fácil marcar la de al lado) y TalkBack
 * la anuncia con su significado: "4 estrellas, Bueno".
 */
@Composable
fun StarRatingInput(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.selectableGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (stars in 1..5) {
            val isFilled = stars <= selected
            val description = if (stars == 1) "1 estrella, ${starRatingLabel(1)}" else "$stars estrellas, ${starRatingLabel(stars)}"
            Box(
                modifier = Modifier
                    .size(AgroSizes.minTouchTarget)
                    .selectable(
                        selected = stars == selected,
                        onClick = { onSelect(stars) },
                        role = Role.RadioButton
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (isFilled) MaterialTheme.agroColors.star else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        if (selected > 0) {
            Spacer(Modifier.width(AgroSpacing.xs))
            Text(
                text = starRatingLabel(selected),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
