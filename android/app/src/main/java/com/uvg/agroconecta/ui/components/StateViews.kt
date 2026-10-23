package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing

internal const val OFFLINE_RETRY_MESSAGE =
    "Estás sin señal. Apenas vuelva, lo intentamos de nuevo solos."

/**
 * Pantalla o sección sin contenido. Siempre explica por qué está vacía y,
 * cuando se puede, ofrece el siguiente paso; un "No hay datos" solo deja a la
 * persona sin saber qué hacer.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    StateLayout(
        icon = icon,
        iconContainer = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
        title = title,
        message = message,
        modifier = modifier
    ) {
        if (actionLabel != null && onAction != null) {
            AgroPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Error que impide mostrar la pantalla. El mensaje debe decir qué pasó en
 * palabras simples; el botón de reintentar va siempre porque casi todos los
 * errores en campo son de señal y se resuelven solos.
 *
 * Sin señal, el texto lo dice claro y avisa que se reintentará solo: en
 * cuanto vuelve la conexión se llama a [onRetry] sin que nadie toque nada.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "No pudimos cargar la información",
    icon: ImageVector = Icons.Default.CloudOff
) {
    val isOffline = LocalIsOffline.current
    RetryWhenBackOnline(onRetry)

    StateLayout(
        icon = if (isOffline) Icons.Default.WifiOff else icon,
        iconContainer = MaterialTheme.colorScheme.errorContainer,
        iconTint = MaterialTheme.colorScheme.onErrorContainer,
        title = title,
        message = if (isOffline) OFFLINE_RETRY_MESSAGE else friendlyErrorMessage(message),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        AgroPrimaryButton(
            text = "Reintentar",
            onClick = onRetry,
            icon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Error dentro de una pantalla que sí tiene contenido (por ejemplo, una
 * sección de Home que falló). Ocupa poco para no tapar lo que sí cargó.
 * Igual que [ErrorState], si trae [onRetry] se reintenta solo al volver la señal.
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Reintentar"
) {
    if (onRetry != null) RetryWhenBackOnline(onRetry)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.xs, top = AgroSpacing.xs, bottom = AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(AgroSizes.iconSmall)
            )
            Spacer(Modifier.size(AgroSpacing.sm))
            Text(
                text = friendlyErrorMessage(message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = AgroSpacing.xs)
            )
            if (onRetry != null) {
                AgroTextButton(text = retryLabel, onClick = onRetry, destructive = true)
            }
        }
    }
}

@Composable
private fun StateLayout(
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.lg, vertical = AgroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = iconContainer,
            contentColor = iconTint,
            modifier = Modifier.size(AgroSizes.illustration)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(AgroSizes.iconLarge + AgroSpacing.xxs))
            }
        }
        Spacer(Modifier.height(AgroSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(AgroSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AgroSpacing.lg))
        action()
    }
}
