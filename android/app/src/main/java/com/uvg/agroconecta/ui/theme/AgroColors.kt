package com.uvg.agroconecta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores que Material 3 no trae como rol propio pero que la app necesita con
 * el mismo cuidado de contraste: avisos (sin conexión, pedidos urgentes),
 * estrellas de calificación y la barra superior.
 *
 * La barra superior va aparte porque en modo claro es verde de marca, pero en
 * oscuro un verde claro a lo ancho de la pantalla encandila; ahí usa superficie.
 */
@Immutable
data class AgroExtendedColors(
    val star: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val topBarContainer: Color,
    val onTopBar: Color
)

internal val LightAgroColors = AgroExtendedColors(
    star = LightStar,
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    topBarContainer = LightPrimary,
    onTopBar = LightOnPrimary
)

internal val DarkAgroColors = AgroExtendedColors(
    star = DarkStar,
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    topBarContainer = DarkSurfaceContainer,
    onTopBar = DarkOnSurface
)

// El valor por defecto es el claro para que los tests y previews que usan un
// MaterialTheme simple sigan viendo colores válidos sin envolver en el tema.
internal val LocalAgroColors = staticCompositionLocalOf { LightAgroColors }

val MaterialTheme.agroColors: AgroExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAgroColors.current
