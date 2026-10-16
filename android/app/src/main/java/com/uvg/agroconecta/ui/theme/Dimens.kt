package com.uvg.agroconecta.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Espaciado en múltiplos de 4. Los nombres describen el uso y no el valor
 * para que ajustar la densidad de toda la app sea cambiar una línea.
 */
object AgroSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    // Margen lateral de todas las pantallas.
    val screenHorizontal = 16.dp
}

object AgroSizes {
    // Mínimo táctil de Android; con dedos gruesos o guantes no se negocia.
    val minTouchTarget = 48.dp
    // La acción principal de cada pantalla va un escalón más grande.
    val primaryButtonHeight = 56.dp
    val secondaryButtonHeight = 48.dp

    val iconSmall = 20.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val illustration = 72.dp
}

val AgroConectaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
