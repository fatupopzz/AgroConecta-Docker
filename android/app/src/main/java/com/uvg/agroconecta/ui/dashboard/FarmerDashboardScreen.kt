package com.uvg.agroconecta.ui.dashboard

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.FarmerDashboardResponse
import com.uvg.agroconecta.data.models.FarmerLastOrder
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.ErrorState
import com.uvg.agroconecta.ui.components.AgroPullToRefreshBox
import com.uvg.agroconecta.ui.components.RefreshableFullScreen
import com.uvg.agroconecta.ui.components.ProductImage
import com.uvg.agroconecta.ui.components.SkeletonList
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.formatFriendlyDate
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.orders.orderStatusLabel
import com.uvg.agroconecta.ui.orders.orderStatusTone
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun FarmerDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FarmerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AgroTopAppBar(title = "Mi resumen", onBack = onNavigateBack) }
    ) { padding ->
        FarmerDashboardBody(
            uiState = uiState,
            onRetry = viewModel::loadDashboard,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun FarmerDashboardBody(
    uiState: FarmerDashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AgroPullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = onRetry,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading && uiState.dashboard == null -> SkeletonList(
                count = 3,
                description = "Cargando tu resumen",
                modifier = Modifier.padding(AgroSpacing.screenHorizontal)
            )

            uiState.errorMessage != null && uiState.dashboard == null -> RefreshableFullScreen {
                ErrorState(
                    title = "No pudimos cargar tu resumen",
                    message = uiState.errorMessage,
                    icon = Icons.Default.Analytics,
                    onRetry = onRetry
                )
            }

            uiState.dashboard != null -> DashboardContent(uiState.dashboard)
        }
    }
}

@Composable
private fun DashboardContent(dashboard: FarmerDashboardResponse) {
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
            DashboardMetricCard(
                title = "Total histórico",
                value = formatQuetzales(dashboard.totalGastadoHistorico),
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            DashboardMetricCard(
                title = "Este mes",
                value = formatQuetzales(dashboard.totalGastadoMesActual),
                icon = Icons.Default.Analytics,
                modifier = Modifier.weight(1f)
            )
        }

        DashboardMetricCard(
            title = "Pedidos realizados",
            value = dashboard.cantidadPedidos.toString(),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            modifier = Modifier.fillMaxWidth()
        )

        DashboardSection("Gasto de los últimos 6 meses", Icons.Default.Analytics) {
            val maximum = dashboard.gastosPorMes.maxOfOrNull { it.total } ?: 0.0
            dashboard.gastosPorMes.forEach { month ->
                MonthlyBar(
                    month = formatMonth(month.mes),
                    amount = formatQuetzales(month.total),
                    progress = if (maximum > 0) (month.total / maximum).toFloat() else 0f
                )
            }
        }

        DashboardSection("Lo que más compras", Icons.Default.Inventory2) {
            if (dashboard.productosMasComprados.isEmpty()) {
                EmptyDashboardMessage("Cuando hagas tus primeras compras, aquí verás lo que más pides.")
            } else {
                dashboard.productosMasComprados.forEachIndexed { index, product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AgroSpacing.sm)
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(AgroSpacing.xs))
                        ProductImage(
                            imagenUrl = product.imagenUrl,
                            contentDescription = null,
                            iconSize = 22.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                        Spacer(Modifier.width(AgroSpacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = product.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${product.cantidad} unidades",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatQuetzales(product.totalGastado),
                            style = MaterialTheme.typography.titleSmall.tabularNumbers,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < dashboard.productosMasComprados.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        DashboardSection("Último pedido", Icons.AutoMirrored.Filled.ReceiptLong) {
            dashboard.ultimoPedido?.let { LastOrderCard(it) }
                ?: EmptyDashboardMessage("Todavía no has hecho pedidos.")
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AgroCard(modifier = modifier) {
        Column(
            Modifier
                .padding(AgroSpacing.md)
                .semantics(mergeDescendants = true) {}
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AgroSizes.iconMedium)
            )
            Spacer(Modifier.height(AgroSpacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.tabularNumbers,
                color = MaterialTheme.colorScheme.onSurface
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
private fun DashboardSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    AgroCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AgroSpacing.md)) {
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
            Spacer(Modifier.height(AgroSpacing.sm))
            content()
        }
    }
}

@Composable
private fun MonthlyBar(month: String, amount: String, progress: Float) {
    Column(
        Modifier
            .padding(vertical = AgroSpacing.xs)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = month,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.labelMedium.tabularNumbers,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(AgroSpacing.xxs))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraSmall)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

@Composable
private fun LastOrderCard(order: FarmerLastOrder) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Pedido #${order.id}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = order.distribuidorNombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatFriendlyDate(order.fechaPedido).orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatQuetzales(order.totalPedido),
                style = MaterialTheme.typography.titleSmall.tabularNumbers,
                color = MaterialTheme.colorScheme.onSurface
            )
            StatusTag(text = orderStatusLabel(order.estado), tone = orderStatusTone(order.estado))
        }
    }
}

@Composable
private fun EmptyDashboardMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = AgroSpacing.md)
    )
}

private fun formatMonth(value: String): String = try {
    YearMonth.parse(value).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "GT")))
        .replaceFirstChar { it.uppercase() }
} catch (_: DateTimeParseException) {
    value
}
