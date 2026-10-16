package com.uvg.agroconecta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors

internal const val OFFLINE_INDICATOR_TEXT = "Sin conexión"
internal const val OFFLINE_INDICATOR_DETAIL = "Puede que veas información de antes. Se actualiza sola cuando vuelva la señal."

/**
 * Aviso de sin conexión que vive arriba de toda la app. En el campo la señal
 * va y viene, así que aparece y desaparece solo, sin bloquear nada: el
 * catálogo guardado se sigue pudiendo ver.
 *
 * Es una región "live" para que TalkBack avise el cambio sin que la persona
 * tenga que buscarlo.
 */
@Composable
fun OfflineBanner(
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.agroColors.warningContainer,
            contentColor = MaterialTheme.agroColors.onWarningContainer,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline-indicator")
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AgroSpacing.md, vertical = AgroSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    modifier = Modifier.size(AgroSizes.iconSmall)
                )
                Column {
                    Text(
                        text = OFFLINE_INDICATOR_TEXT,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = OFFLINE_INDICATOR_DETAIL,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
