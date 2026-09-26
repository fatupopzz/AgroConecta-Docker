package com.uvg.agroconecta.ui.favorites

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val FavoriteRed = Color(0xFFE53935)
private val FavoriteGreen = Color(0xFF2D6A1F)

@Composable
fun FavoriteButton(
    productId: Int,
    isFavorite: Boolean,
    isPending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.94f),
    inactiveTint: Color = FavoriteGreen
) {
    Surface(
        modifier = modifier
            .size(40.dp),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = if (containerColor.alpha > 0f) 2.dp else 0.dp
    ) {
        IconButton(
            onClick = onClick,
            enabled = !isPending,
            modifier = Modifier.testTag("favorite-button-$productId")
        ) {
            if (isPending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = if (isFavorite) FavoriteRed else inactiveTint,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (isFavorite) {
                        "Quitar de favoritos"
                    } else {
                        "Agregar a favoritos"
                    },
                    tint = if (isFavorite) FavoriteRed else inactiveTint
                )
            }
        }
    }
}
