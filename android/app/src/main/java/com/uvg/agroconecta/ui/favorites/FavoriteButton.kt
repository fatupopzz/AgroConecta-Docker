package com.uvg.agroconecta.ui.favorites

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.theme.AgroSizes

/**
 * Corazón de favorito. Antes iba dentro de un círculo fijo de 40 dp que
 * recortaba el área táctil; ahora el círculo visible sigue siendo de 40 dp
 * pero el toque cubre los 48 dp mínimos.
 *
 * [containerColor] por defecto es un fondo casi opaco para que el corazón se
 * vea encima de cualquier foto; en barras de color se pasa transparente.
 */
@Composable
fun FavoriteButton(
    productId: Int,
    isFavorite: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    inactiveTint: Color = Color.Unspecified
) {
    val background = containerColor.takeOrElse {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f)
    }
    val idleTint = inactiveTint.takeOrElse { MaterialTheme.colorScheme.primary }
    val activeTint = MaterialTheme.colorScheme.error
    val tint = if (isFavorite) activeTint else idleTint
    val haptics = rememberAgroHaptics()

    // El corazón late una vez al marcarlo, para que se note que quedó guardado
    // aunque la respuesta del servidor tarde.
    val scale = remember { Animatable(1f) }
    var tapped by remember { mutableStateOf(false) }
    LaunchedEffect(isFavorite) {
        if (tapped && isFavorite) {
            scale.animateTo(1.3f, tween(durationMillis = 120))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    IconButton(
        onClick = {
            tapped = true
            haptics.tick()
            onClick()
        },
        enabled = !isPending,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = background,
            contentColor = tint,
            disabledContainerColor = background,
            disabledContentColor = tint
        ),
        modifier = modifier.testTag("favorite-button-$productId")
    ) {
        if (isPending) {
            CircularProgressIndicator(
                modifier = Modifier.size(AgroSizes.iconSmall),
                color = tint,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
            )
        }
    }
}
