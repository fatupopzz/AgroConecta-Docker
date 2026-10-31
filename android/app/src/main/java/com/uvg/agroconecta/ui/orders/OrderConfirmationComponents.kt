package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.RadioOption
import com.uvg.agroconecta.ui.components.SkeletonBox
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.orders.checkout.CheckoutValidation
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers

@Composable
internal fun OrderConfirmationTopBar(onBack: () -> Unit, enabled: Boolean = true) {
    AgroTopAppBar(title = "Confirmar pedido", onBack = onBack, backEnabled = enabled)
}

/**
 * Total y botón de confirmar. El error se queda visible hasta que la persona
 * actúe (no es un snackbar que desaparece) porque perder un pedido sin saber
 * por qué es lo peor que puede pasar en esta pantalla.
 */
@Composable
internal fun OrderConfirmationBottomBar(
    total: Double,
    canConfirm: Boolean,
    onConfirmOrder: () -> Unit,
    isCreatingOrder: Boolean,
    errorMessage: String?,
    canRetry: Boolean,
    onRetryOrder: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            if (errorMessage != null) {
                InlineError(
                    message = errorMessage,
                    onRetry = if (canRetry && !isCreatingOrder) onRetryOrder else null
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total a pagar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatQuetzales(total),
                    style = MaterialTheme.typography.headlineMedium.tabularNumbers,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AgroPrimaryButton(
                text = "Confirmar pedido",
                loadingText = "Enviando pedido…",
                icon = Icons.Default.CheckCircle,
                onClick = onConfirmOrder,
                enabled = canConfirm,
                loading = isCreatingOrder,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun DeliveryTypeCard(
    deliveryType: String,
    onDeliveryTypeChange: (String) -> Unit,
    enabled: Boolean = true
) {
    CheckoutSection(title = "¿Cómo recibes tu pedido?") {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            RadioOption(
                label = "Entrega en mi finca",
                description = "El distribuidor lo lleva a tu dirección",
                icon = Icons.Default.Home,
                selected = deliveryType == "domicilio",
                enabled = enabled,
                onClick = { onDeliveryTypeChange("domicilio") }
            )
            RadioOption(
                label = "Lo recojo yo",
                description = "Pasas por la tienda del distribuidor",
                icon = Icons.Default.Store,
                selected = deliveryType == "recogida",
                enabled = enabled,
                onClick = { onDeliveryTypeChange("recogida") }
            )
        }
    }
}

@Composable
internal fun DeliveryAddressCard(
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit,
    enabled: Boolean = true
) {
    CheckoutSection(title = "Dirección de entrega", icon = Icons.Default.LocationOn) {
        AgroTextField(
            value = deliveryAddress,
            onValueChange = onDeliveryAddressChange,
            label = "Aldea, caserío o referencia",
            placeholder = "Ej: Aldea El Tablón, Jalapa",
            errorMessage = if (CheckoutValidation.isDeliveryAddressTooShort(deliveryAddress)) {
                "Agrega más detalles para que el distribuidor llegue"
            } else {
                null
            },
            enabled = enabled,
            singleLine = false,
            maxLines = 3
        )
    }
}

@Composable
internal fun PickupAddressCard(
    pickupAddress: String?,
    isLoadingPickupAddress: Boolean
) {
    CheckoutSection(title = "Dónde recogerlo", icon = Icons.Default.Store) {
        when {
            isLoadingPickupAddress -> SkeletonBox(
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )

            pickupAddress.isNullOrBlank() -> InlineError(
                message = "El distribuidor no tiene una dirección registrada. Elige entrega en tu finca."
            )

            else -> Text(
                text = pickupAddress,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun PaymentMethodCard() {
    AgroCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        border = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(AgroSizes.iconMedium)
            )
            Spacer(Modifier.width(AgroSpacing.sm))
            Column {
                Text(
                    text = "Pagas al recibir",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "En efectivo, cuando te entreguen el pedido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
internal fun OrderSummaryHeader(itemCount: Int, distributorName: String? = null) {
    Column(
        modifier = Modifier
            .padding(top = AgroSpacing.xs)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        Text(
            text = if (itemCount == 1) "Tu pedido (1 producto)" else "Tu pedido ($itemCount productos)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        distributorName?.let {
            Text(
                text = "Te lo vende $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun OrderSummaryItemCard(item: CartItemUI) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AgroSpacing.sm)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imagenUrl = item.imagenUrl,
                contentDescription = null,
                iconSize = AgroSizes.iconMedium,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(Modifier.width(AgroSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.distribuidor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(AgroSpacing.xxs))
                StatusTag(text = "Cantidad: ${item.cantidad}")
            }
            Text(
                text = formatQuetzales(item.subtotal),
                style = MaterialTheme.typography.titleSmall.tabularNumbers,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CheckoutSection(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AgroSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AgroSizes.iconSmall)
                    )
                    Spacer(Modifier.width(AgroSpacing.xs))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}
