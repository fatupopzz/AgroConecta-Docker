package com.uvg.agroconecta.ui.distributor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.DistributorStatsResponse
import com.uvg.agroconecta.data.models.OrdersByStatus
import com.uvg.agroconecta.data.models.TopSellingProduct
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.orders.orderStatusTone
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers
import kotlinx.coroutines.flow.first

@Composable
fun DistributorStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DistributorStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var distributorId by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        distributorId = SessionManager.getPerfilId(context).first() ?: -1
        viewModel.loadStats(distributorId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = "Mis ventas", onBack = onNavigateBack) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val stats = uiState.stats
            when {
                uiState.isLoading && stats == null -> SkeletonList(
                    count = 3,
                    description = "Cargando tus ventas",
                    modifier = Modifier.padding(AgroSpacing.screenHorizontal)
                )

                uiState.errorMessage != null && stats == null -> ErrorState(
                    title = "No pudimos cargar tus ventas",
                    message = uiState.errorMessage.orEmpty(),
                    icon = Icons.Default.Analytics,
                    onRetry = { viewModel.loadStats(distributorId) },
                    modifier = Modifier.align(Alignment.Center)
                )

                stats != null -> StatsContent(stats = stats)
            }
        }
    }
}

@Composable
private fun StatsContent(stats: DistributorStatsResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AgroSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            MetricCard(
                title = "Pedidos",
                value = stats.totalPedidos.toString(),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Ingresos",
                value = formatQuetzales(stats.ingresosTotales),
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
        }

        StatsSection(title = "Lo que más vendes", icon = Icons.Default.Inventory2) {
            if (stats.productosMasVendidos.isEmpty()) {
                EmptySectionMessage("Aquí verás tus productos más vendidos cuando entregues tus primeros pedidos.")
            } else {
                stats.productosMasVendidos.forEachIndexed { index, product ->
                    TopProductRow(position = index + 1, product = product)
                    if (index < stats.productosMasVendidos.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        StatsSection(title = "Pedidos por estado", icon = Icons.Default.Analytics) {
            stats.pedidosPorEstado.forEachIndexed { index, item ->
                OrderStatusRow(item)
                if (index < stats.pedidosPorEstado.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AgroCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(AgroSpacing.md)
                .semantics(mergeDescendants = true) {}
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(AgroSizes.iconSmall)
                )
            }
            Spacer(Modifier.height(AgroSpacing.sm))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.tabularNumbers,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AgroSizes.iconSmall)
                )
                Spacer(Modifier.width(AgroSpacing.xs))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            Spacer(Modifier.height(AgroSpacing.xs))
            content()
        }
    }
}

@Composable
private fun TopProductRow(
    position: Int,
    product: TopSellingProduct
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AgroSpacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(AgroSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${product.cantidad} vendidos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatQuetzales(product.ingresos),
            style = MaterialTheme.typography.titleSmall.tabularNumbers,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun OrderStatusRow(item: OrdersByStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AgroSpacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusTag(text = statusLabel(item.estado), tone = orderStatusTone(item.estado))
        Spacer(Modifier.weight(1f))
        Text(
            text = item.cantidad.toString(),
            style = MaterialTheme.typography.titleMedium.tabularNumbers,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AgroSpacing.md),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun statusLabel(status: String): String = when (status) {
    "confirmado" -> "Confirmados"
    "preparando" -> "En preparación"
    "en_ruta" -> "En camino"
    "entregado" -> "Entregados"
    "cancelado" -> "Cancelados"
    else -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
}
