package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing

/**
 * La acción principal de una pantalla. Solo debería haber una visible a la
 * vez: si todo es verde y lleno, nada destaca.
 *
 * Mientras [loading] está activo el botón se bloquea para evitar pedidos
 * duplicados con conexiones lentas, y TalkBack anuncia que está en proceso.
 */
@Composable
fun AgroPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = AgroSizes.primaryButtonHeight)
            .semantics { if (loading) stateDescription = "En proceso" },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = AgroSpacing.lg, vertical = AgroSpacing.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        ButtonContent(
            text = if (loading) loadingText ?: text else text,
            icon = icon,
            loading = loading
        )
    }
}

/**
 * Acción alternativa junto a la principal. Con [destructive] toma el color de
 * error para acciones que conviene pensar dos veces (pedido urgente, borrar).
 */
@Composable
fun AgroSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = AgroSizes.secondaryButtonHeight)
            .semantics { if (loading) stateDescription = "En proceso" },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = AgroSpacing.md, vertical = AgroSpacing.xs),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
    ) {
        ButtonContent(
            text = if (loading) loadingText ?: text else text,
            icon = icon,
            loading = loading
        )
    }
}

/**
 * Acción de bajo peso (ver más, cancelar). Mantiene los 48 dp táctiles aunque
 * visualmente sea solo texto.
 */
@Composable
fun AgroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    destructive: Boolean = false
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = AgroSizes.minTouchTarget),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    ) {
        ButtonContent(text = text, icon = icon, loading = false)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    loading: Boolean
) {
    when {
        loading -> {
            CircularProgressIndicator(
                modifier = Modifier.size(AgroSizes.iconSmall),
                color = LocalContentColor.current,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(AgroSpacing.xs))
        }
        icon != null -> {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AgroSizes.iconSmall)
            )
            Spacer(Modifier.width(AgroSpacing.xs))
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}
