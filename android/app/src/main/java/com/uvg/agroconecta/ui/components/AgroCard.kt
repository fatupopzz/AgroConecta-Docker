package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors

/**
 * Tarjeta base de la app. Lleva un borde suave además de la sombra mínima
 * porque en pantallas con poco brillo la sombra sola no separa la tarjeta del
 * fondo. Si recibe [onClick], toda la tarjeta es el área táctil.
 */
@Composable
fun AgroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

enum class TagTone { Neutral, Success, Info, Warning, Danger }

/**
 * Etiqueta corta de estado (categoría, verificado, urgente, agotado). El tono
 * acompaña al texto, nunca lo reemplaza: quien no distingue colores sigue
 * leyendo "Agotado".
 */
@Composable
fun StatusTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: TagTone = TagTone.Neutral,
    icon: ImageVector? = null
) {
    val (container, content) = when (tone) {
        TagTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        TagTone.Success -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        TagTone.Info -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TagTone.Warning -> MaterialTheme.agroColors.warningContainer to MaterialTheme.agroColors.onWarningContainer
        TagTone.Danger -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AgroSpacing.xs, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(AgroSpacing.xxs))
            }
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Encabezado de sección con acción opcional ("Ver todo"). El título se marca
 * como encabezado para que TalkBack permita saltar de sección en sección.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AgroSizes.minTouchTarget)
            .padding(start = AgroSpacing.screenHorizontal, end = AgroSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            AgroTextButton(text = actionLabel, onClick = onAction)
        }
    }
}
