package com.uvg.agroconecta.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import kotlinx.coroutines.delay

internal const val OFFLINE_INDICATOR_TEXT = "Sin conexión"
internal const val OFFLINE_INDICATOR_DETAIL = "Puede que veas información de antes. Se actualiza sola cuando vuelva la señal."
internal const val BACK_ONLINE_TEXT = "Ya tienes señal otra vez"

/**
 * Aviso de sin conexión que vive arriba de toda la app. En el campo la señal
 * va y viene, así que aparece y desaparece solo, sin bloquear nada: el
 * catálogo guardado se sigue pudiendo ver.
 *
 * Cuando vuelve la señal no desaparece de golpe: pasa unos segundos a verde
 * diciendo que ya hay conexión. Si se esfumara sin más, la persona no sabría
 * si se arregló o si solo lo dejó de ver.
 *
 * Es una región "live" para que TalkBack avise el cambio sin que la persona
 * tenga que buscarlo.
 */
@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    var showBackOnline by remember { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(isOffline) }

    LaunchedEffect(isOffline) {
        if (isOffline) {
            showBackOnline = false
        } else if (wasOffline) {
            showBackOnline = true
            delay(BACK_ONLINE_VISIBLE_MS)
            showBackOnline = false
        }
        wasOffline = isOffline
    }

    AnimatedVisibility(
        visible = isOffline || showBackOnline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = isOffline,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "offlineBanner"
        ) { offline ->
            if (offline) {
                BannerRow(
                    icon = Icons.Default.WifiOff,
                    title = OFFLINE_INDICATOR_TEXT,
                    detail = OFFLINE_INDICATOR_DETAIL,
                    isWarning = true,
                    modifier = Modifier.testTag("offline-indicator")
                )
            } else {
                BannerRow(
                    icon = Icons.Default.Wifi,
                    title = BACK_ONLINE_TEXT,
                    detail = null,
                    isWarning = false,
                    modifier = Modifier.testTag("back-online-indicator")
                )
            }
        }
    }
}

@Composable
private fun BannerRow(
    icon: ImageVector,
    title: String,
    detail: String?,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isWarning) MaterialTheme.agroColors.warningContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isWarning) MaterialTheme.agroColors.onWarningContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AgroSpacing.md, vertical = AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AgroSizes.iconSmall)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                if (detail != null) {
                    Text(text = detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// Suficiente para leerlo de reojo sin que estorbe más de la cuenta.
private const val BACK_ONLINE_VISIBLE_MS = 3_000L
