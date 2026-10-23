package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.tabularNumbers

/**
 * Botones de menos y más con la cantidad en medio. Los botones miden 48 dp
 * (antes 32) y llevan ícono en vez de los caracteres "-" y "+", que TalkBack
 * leía como "guion" o no leía.
 *
 * El número es una región "live" para que se anuncie cada cambio sin tener
 * que volver a enfocarlo.
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    itemName: String? = null,
    minQuantity: Int = 1,
    maxQuantity: Int = Int.MAX_VALUE,
    enabled: Boolean = true
) {
    val suffix = itemName?.let { " de $it" }.orEmpty()
    // Un toque leve por cada cambio deja contar las unidades sin mirar el número.
    val haptics = rememberAgroHaptics()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(
            onClick = {
                haptics.tick()
                onDecrease()
            },
            enabled = enabled && quantity > minQuantity,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.semantics { contentDescription = "Quitar uno$suffix" }
        ) {
            Icon(Icons.Default.Remove, contentDescription = null)
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium.tabularNumbers,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = AgroSizes.minTouchTarget)
                .semantics {
                    contentDescription = "Cantidad: $quantity"
                    liveRegion = LiveRegionMode.Polite
                }
        )
        FilledTonalIconButton(
            onClick = {
                haptics.tick()
                onIncrease()
            },
            enabled = enabled && quantity < maxQuantity,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.semantics { contentDescription = "Agregar uno$suffix" }
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}
