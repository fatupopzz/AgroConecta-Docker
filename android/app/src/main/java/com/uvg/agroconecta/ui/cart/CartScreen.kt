package com.uvg.agroconecta.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.QuantityStepper
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.orders.checkout.CheckoutValidation
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers

// CartItemUI ya no viaja por savedStateHandle; se lee directamente del
// CartViewModel compartido, por eso no necesita ser Serializable.
data class CartItemUI(
    val id: Int,
    val idInventario: Int,
    val idDistribuidor: Int,
    val nombre: String,
    val distribuidor: String,
    val cantidad: Int,
    val precio: Double,
    val subtotal: Double,
    val imagenUrl: String? = null,
    val imagenThumbUrl: String? = null
)

/**
 * Carrito agrupado por distribuidor. Agrupar deja ver de un vistazo a quién
 * se le compra cada cosa, y explica por qué no se puede pedir todo junto
 * cuando hay más de un distribuidor (cada pedido sale de uno solo).
 */
@Composable
fun CartScreen(
    items: List<CartItemUI>,
    total: Double,
    errorMessage: String?,
    onIncreaseQuantity: (Int) -> Unit,
    onDecreaseQuantity: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onCheckout: () -> Unit,
    onUrgentOrder: () -> Unit,
    onGoToCatalog: () -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false
) {
    val groups = remember(items) { items.groupBy { it.idDistribuidor } }
    val mixedDistributors = remember(items) { CheckoutValidation.hasMultipleDistributors(items) }
    val unitCount = items.sumOf { it.cantidad }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AgroTopAppBar(
                title = "Mi carrito",
                subtitle = when {
                    items.isEmpty() -> null
                    unitCount == 1 -> "1 producto"
                    else -> "$unitCount productos"
                },
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                CartSummaryBar(
                    total = total,
                    canCheckout = !mixedDistributors,
                    onCheckout = onCheckout,
                    onUrgentOrder = onUrgentOrder
                )
            }
        }
    ) { padding ->
        when {
            isLoading && items.isEmpty() -> SkeletonList(
                count = 3,
                description = "Cargando tu carrito",
                modifier = Modifier
                    .padding(padding)
                    .padding(AgroSpacing.screenHorizontal)
            )

            items.isEmpty() -> Column(Modifier.padding(padding)) {
                errorMessage?.let {
                    InlineError(
                        message = it,
                        modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                    )
                }
                EmptyState(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Tu carrito está vacío",
                    message = "Agrega productos desde el catálogo para hacer tu pedido.",
                    actionLabel = "Ver productos",
                    onAction = onGoToCatalog,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                errorMessage?.let { message ->
                    item(key = "error") { InlineError(message = message) }
                }
                if (mixedDistributors) {
                    item(key = "mixed-warning") { MixedDistributorsNotice(groups.size) }
                }
                groups.forEach { (distributorId, groupItems) ->
                    item(key = "distributor-$distributorId") {
                        DistributorHeader(
                            name = groupItems.first().distribuidor,
                            subtotal = groupItems.sumOf { it.subtotal }
                        )
                    }
                    items(groupItems, key = { it.id }) { item ->
                        CartItemCard(
                            item = item,
                            onIncrease = { onIncreaseQuantity(item.id) },
                            onDecrease = { onDecreaseQuantity(item.id) },
                            onRemove = { onRemoveItem(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MixedDistributorsNotice(distributorCount: Int) {
    Surface(
        color = MaterialTheme.agroColors.warningContainer,
        contentColor = MaterialTheme.agroColors.onWarningContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Column {
                Text(
                    text = "Tienes productos de $distributorCount distribuidores",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Cada pedido es de un solo distribuidor. Deja en el carrito solo los de uno y pide los otros después.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DistributorHeader(name: String, subtotal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AgroSpacing.xs)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AgroSizes.iconSmall)
        )
        Spacer(Modifier.width(AgroSpacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatQuetzales(subtotal),
            style = MaterialTheme.typography.labelLarge.tabularNumbers,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CartItemCard(
    item: CartItemUI,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = AgroSpacing.md, end = AgroSpacing.md, top = AgroSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                ProductImage(
                    imagenUrl = item.imagenUrl,
                    imagenThumbUrl = item.imagenThumbUrl,
                    contentDescription = null,
                    iconSize = 28.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.width(AgroSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatQuetzales(item.precio)} c/u",
                        style = MaterialTheme.typography.bodySmall.tabularNumbers,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatQuetzales(item.subtotal),
                        style = MaterialTheme.typography.titleMedium.tabularNumbers,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics {
                            contentDescription = "Subtotal ${formatQuetzales(item.subtotal)}"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(AgroSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityStepper(
                    quantity = item.cantidad,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                    itemName = item.nombre
                )
                Spacer(Modifier.weight(1f))
                AgroTextButton(
                    text = "Quitar",
                    icon = Icons.Default.Delete,
                    destructive = true,
                    onClick = onRemove,
                    modifier = Modifier.semantics { contentDescription = "Quitar ${item.nombre} del carrito" }
                )
            }
        }
    }
}

/**
 * Total y acciones al pie de la pantalla, al alcance del pulgar. El pedido
 * normal es la acción principal; el urgente queda como alternativa para no
 * competir con ella.
 */
@Composable
private fun CartSummaryBar(
    total: Double,
    canCheckout: Boolean,
    onCheckout: () -> Unit,
    onUrgentOrder: () -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pagas al recibir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatQuetzales(total),
                    style = MaterialTheme.typography.headlineMedium.tabularNumbers,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            AgroPrimaryButton(
                text = if (canCheckout) "Continuar con el pedido" else "Deja productos de un solo distribuidor",
                onClick = onCheckout,
                enabled = canCheckout,
                modifier = Modifier.fillMaxWidth()
            )
            AgroSecondaryButton(
                text = "Necesito entrega urgente",
                icon = Icons.Default.Warning,
                destructive = true,
                onClick = onUrgentOrder,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
