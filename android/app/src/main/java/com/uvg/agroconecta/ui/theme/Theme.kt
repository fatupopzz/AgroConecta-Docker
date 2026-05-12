package com.uvg.agroconecta.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AgroConectaLightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenPrimaryDark,

    secondary = OrangeAccent,
    onSecondary = White,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = OrangeAccent,

    tertiary = VerifiedBlue,
    onTertiary = White,

    background = White,
    onBackground = GrayDark,
    surface = White,
    onSurface = GrayDark,
    surfaceVariant = GrayLight,
    onSurfaceVariant = GrayMid,
    outline = GrayBorder,

    error = ErrorRed,
    onError = White
)

// Por ahora reusamos los light colors. Si en el futuro se necesita modo oscuro real,
// se define un dark scheme aquí.
private val AgroConectaDarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenPrimaryDark,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = GreenPale,

    secondary = OrangeAccent,
    onSecondary = White,

    background = GrayDark,
    onBackground = White,
    surface = GrayDark,
    onSurface = White,

    error = ErrorRed,
    onError = White
)

@Composable
fun AgroConectaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color desactivado para mantener brand identity verde en todos los devices
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AgroConectaDarkColors else AgroConectaLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar verde oscuro como en el XML original
            window.statusBarColor = GreenPrimaryDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgroConectaTypography,
        content = content
    )
}