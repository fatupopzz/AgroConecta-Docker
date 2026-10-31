package com.uvg.agroconecta.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import kotlinx.coroutines.launch
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
 *
 * Quitar un producto se puede deshacer. El producto se esconde enseguida y
 * solo se borra en el servidor cuando el aviso se va, cuando se sale del
 * carrito o cuando se sigue con el pedido. Así un toque por error no obliga
 * a buscarlo otra vez en el catálogo.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberAgroHaptics()
    val currentOnRemoveItem by rememberUpdatedState(onRemoveItem)

    // Esperando la decisión del aviso "Deshacer".
    var pendingRemoval by remember { mutableStateOf<CartItemUI?>(null) }
    // Ya enviados al servidor; se siguen escondiendo hasta que la lista recargada
    // deje de traerlos, para que no reaparezcan un instante.
    var removingIds by remember { mutableStateOf(emptySet<Int>()) }

    fun commitRemoval(item: CartItemUI) {
        removingIds = removingIds + item.id
        currentOnRemoveItem(item.id)
    }

    fun commitPendingRemoval() {
        pendingRemoval?.let(::commitRemoval)
        pendingRemoval = null
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    fun requestRemoval(item: CartItemUI) {
        haptics.tick()
        pendingRemoval?.let(::commitRemoval)
        pendingRemoval = item
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Quitaste ${item.nombre}",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long
            )
            // Si mientras tanto se quitó otro producto, este ya se confirmó allá.
            if (pendingRemoval?.id != item.id) return@launch
            if (result != SnackbarResult.ActionPerformed) commitRemoval(item)
            pendingRemoval = null
        }
    }

    LaunchedEffect(items) {
        val currentIds = items.mapTo(mutableSetOf()) { it.id }
        removingIds = removingIds intersect currentIds
    }
    // Si el servidor no pudo borrarlo, el producto vuelve a verse junto al error.
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) removingIds = emptySet()
    }
    // Salir del carrito con el aviso abierto cuenta como "no deshacer".
    DisposableEffect(Unit) {
        onDispose { pendingRemoval?.let { currentOnRemoveItem(it.id) } }
    }

    val hiddenIds = removingIds + listOfNotNull(pendingRemoval?.id)
    val visibleItems = remember(items, hiddenIds) { items.filterNot { it.id in hiddenIds } }
    val visibleTotal = total - items.filter { it.id in hiddenIds }.sumOf { it.subtotal }
    val groups = remember(visibleItems) { visibleItems.groupBy { it.idDistribuidor } }
    val mixedDistributors = remember(visibleItems) { CheckoutValidation.hasMultipleDistributors(visibleItems) }
    val unitCount = visibleItems.sumOf { it.cantidad }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AgroTopAppBar(
                title = "Mi carrito",
                subtitle = when {
                    visibleItems.isEmpty() -> null
                    unitCount == 1 -> "1 producto"
                    else -> "$unitCount productos"
                },
                onBack = {
                    commitPendingRemoval()
                    onNavigateBack()
                }
            )
        },
        bottomBar = {
            if (visibleItems.isNotEmpty()) {
                CartSummaryBar(
                    total = visibleTotal,
                    canCheckout = !mixedDistributors,
                    onCheckout = {
                        commitPendingRemoval()
                        onCheckout()
                    },
                    onUrgentOrder = {
                        commitPendingRemoval()
                        onUrgentOrder()
                    }
                )
            }
        }
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // Se mira la lista completa y no la visible: al quitar el último
                // producto la recarga no debe mostrar siluetas.
                isLoading && items.isEmpty() -> SkeletonList(
                    count = 3,
                    description = "Cargando tu carrito",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                visibleItems.isEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    errorMessage?.let { message ->
                        item(key = "error") {
                            InlineError(
                                message = message,
                                onRetry = onRefresh,
                                modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                            )
                        }
                    }
                    item(key = "empty") {
                        EmptyState(
                            icon = Icons.Outlined.ShoppingCart,
                            title = "Tu carrito está vacío",
                            message = "Agrega productos desde el catálogo para hacer tu pedido.",
                            actionLabel = "Ver productos",
                            onAction = onGoToCatalog,
                            modifier = Modifier.fillParentMaxSize()
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                ) {
                    errorMessage?.let { message ->
                        item(key = "error") { InlineError(message = message, onRetry = onRefresh) }
                    }
                    if (mixedDistributors) {
                        item(key = "mixed-warning") { MixedDistributorsNotice(groups.size) }
                    }
                    groups.forEach { (distributorId, groupItems) ->
                        item(key = "distributor-$distributorId") {
                            DistributorHeader(
                                name = groupItems.first().distribuidor,
                                subtotal = groupItems.sumOf { it.subtotal },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                        items(groupItems, key = { it.id }) { item ->
                            CartItemCard(
                                item = item,
                                onIncrease = { onIncreaseQuantity(item.id) },
                                onDecrease = { onDecreaseQuantity(item.id) },
                                onRemove = { requestRemoval(item) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
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
private fun DistributorHeader(name: String, subtotal: Double, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    AgroCard(modifier = modifier.fillMaxWidth()) {
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
