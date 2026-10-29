package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.AgroSecondaryButton
import com.uvg.agroconecta.ui.components.AgroTextButton
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.components.CategoryChip
import com.uvg.agroconecta.ui.components.EmptyState
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.formatFriendlyDate
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers

/**
 * Historial de pedidos. Toda la tarjeta abre el seguimiento, que es lo que
 * más se busca aquí; los filtros separan lo que todavía está en camino de lo
 * que ya llegó, para no tener que revisar pedido por pedido.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderHistoryScreen(
    orders: List<OrderSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    tipoUsuario: String,
    exportState: OrderExportState = OrderExportState.Idle,
    exportNotice: String? = null,
    onExportPdf: () -> Unit = {},
    onTrackOrder: (Int) -> Unit,
    onOpenAdvice: (Int) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPerfilClick: () -> Unit,
    onRecurringOrders: () -> Unit = {},
    onMakeRecurring: (Int) -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val isDistributor = tipoUsuario == "distribuidor"
    var filter by rememberSaveable { mutableStateOf(OrderFilter.ALL) }
    val visibleOrders = remember(orders, filter) { orders.filter { filter.matches(it.estado) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = if (isDistributor) "Pedidos recibidos" else "Mis pedidos") },
        bottomBar = {
            AppBottomBar(
                selectedTab = BottomNavTab.PEDIDOS,
                tipoUsuario = tipoUsuario,
                onHomeClick = onHomeClick,
                onAgregarClick = onAgregarClick,
                onPedidosClick = { },
                onPerfilClick = onPerfilClick
            )
        }
    ) { padding ->
        AgroPullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                if (tipoUsuario == "agricultor") {
                    item(key = "farmer-actions") {
                        FarmerOrderActions(
                            exportState = exportState,
                            exportNotice = exportNotice,
                            onExportPdf = onExportPdf,
                            onRecurringOrders = onRecurringOrders
                        )
                    }
                }

                if (orders.isNotEmpty()) {
                    item(key = "filters") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                            items(OrderFilter.entries) { option ->
                                val count = orders.count { option.matches(it.estado) }
                                CategoryChip(
                                    label = "${option.label} ($count)",
                                    selected = filter == option,
                                    onClick = { filter = option }
                                )
                            }
                        }
                    }
                }

                when {
                    isLoading && orders.isEmpty() -> item(key = "loading") {
                        SkeletonList(description = "Cargando pedidos")
                    }

                    // Si falló la carga no se dice "no tienes pedidos": sería falso
                    // y asustaría a quien sí tiene pedidos en camino.
                    errorMessage != null && orders.isEmpty() -> item(key = "error") {
                        ErrorState(
                            title = "No pudimos cargar tus pedidos",
                            message = errorMessage,
                            onRetry = onRetry
                        )
                    }

                    orders.isEmpty() -> item(key = "empty") {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = if (isDistributor) "Aún no recibes pedidos" else "Aún no tienes pedidos",
                            message = if (isDistributor) {
                                "Cuando un agricultor te compre, su pedido aparecerá aquí."
                            } else {
                                "Cuando hagas tu primera compra, aquí podrás seguir su entrega."
                            }
                        )
                    }

                    else -> {
                        errorMessage?.let { message ->
                            item(key = "inline-error") { InlineError(message = message, onRetry = onRetry) }
                        }
                        if (isLoading) {
                            item(key = "refreshing") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        }
                        if (visibleOrders.isEmpty()) {
                            item(key = "filter-empty") {
                                Text(
                                    text = when (filter) {
                                        OrderFilter.ACTIVE -> "No tienes pedidos en camino."
                                        OrderFilter.DELIVERED -> "Todavía no te han entregado pedidos."
                                        OrderFilter.ALL -> ""
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = AgroSpacing.md)
                                )
                            }
                        }
                        items(visibleOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                isDistributor = isDistributor,
                                onTrackOrder = { onTrackOrder(order.id) },
                                onOpenAdvice = { onOpenAdvice(order.id) },
                                onMakeRecurring = { onMakeRecurring(order.id) },
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
private fun FarmerOrderActions(
    exportState: OrderExportState,
    exportNotice: String?,
    onExportPdf: () -> Unit,
    onRecurringOrders: () -> Unit
) {
    // Los dos botones a la par partían "Pedidos recurrentes" en dos líneas.
    // La tarjeta además explica para qué sirve, que no era obvio por el nombre.
    Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
        AgroCard(
            onClick = onRecurringOrders,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            border = null
        ) {
            Row(
                modifier = Modifier.padding(AgroSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pedidos recurrentes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Compra lo mismo cada cierto tiempo sin volver a armarlo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        val downloading = exportState is OrderExportState.Downloading
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AgroTextButton(
                text = if (downloading) "Descargando PDF…" else "Exportar PDF",
                icon = Icons.Default.PictureAsPdf,
                onClick = onExportPdf,
                enabled = !downloading
            )
        }
        val exportError = (exportState as? OrderExportState.Error)?.message
        when {
            exportError != null -> InlineError(message = exportError, onRetry = onExportPdf)
            exportNotice != null -> Text(
                text = exportNotice,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(
    order: OrderSummary,
    isDistributor: Boolean,
    onTrackOrder: () -> Unit,
    onOpenAdvice: () -> Unit,
    onMakeRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCanceled = order.estado == "cancelado"

    AgroCard(
        onClick = onTrackOrder,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Pedido ${order.id}, ${orderStatusLabel(order.estado)}, " +
                    "total ${formatQuetzales(order.totalPedido)}. Toca para ver el seguimiento"
            },
        containerColor = if (order.esUrgente) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = if (order.esUrgente) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier.padding(
                start = AgroSpacing.md,
                end = AgroSpacing.md,
                top = AgroSpacing.md,
                bottom = AgroSpacing.xs
            ),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                Text(
                    text = "Pedido #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (order.esUrgente) {
                    StatusTag(text = "Urgente", tone = TagTone.Danger, icon = Icons.Default.Warning)
                }
                StatusTag(text = orderStatusLabel(order.estado), tone = orderStatusTone(order.estado))
            }
            val counterparty = if (isDistributor) order.agricultorNombre else order.distribuidorNombre
            counterparty?.let {
                Text(
                    text = if (isDistributor) "Agricultor: $it" else "Distribuidor: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (order.esUrgente && !order.tipoPlaga.isNullOrBlank()) {
                Text(
                    text = "Plaga detectada: ${order.tipoPlaga}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatQuetzales(order.totalPedido),
                    style = MaterialTheme.typography.titleLarge.tabularNumbers,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (order.cantidadProductos == 1) "1 producto" else "${order.cantidadProductos} productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            formatFriendlyDate(order.fechaPedido)?.let { fecha ->
                Text(
                    text = "Pedido el $fecha · Pagas al recibir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isCanceled) {
                // La barra resume en qué paso va sin tener que abrir el seguimiento.
                LinearProgressIndicator(
                    progress = { orderProgress(order.estado) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AgroSpacing.xs)
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
            // FlowRow: con letra grande los botones no caben en una línea y
            // antes se cortaban; así bajan a la siguiente.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xxs, Alignment.End)
            ) {
                if (!isDistributor && order.estado == "entregado") {
                    AgroTextButton(text = "Hacer recurrente", icon = Icons.Default.Repeat, onClick = onMakeRecurring)
                }
                AgroTextButton(text = "Asesoría", icon = Icons.AutoMirrored.Filled.Chat, onClick = onOpenAdvice)
                AgroTextButton(text = "Seguimiento", icon = Icons.Default.LocalShipping, onClick = onTrackOrder)
            }
        }
    }
}
