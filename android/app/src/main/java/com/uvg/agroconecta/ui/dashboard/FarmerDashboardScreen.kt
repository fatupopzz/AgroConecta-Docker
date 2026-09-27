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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.models.FarmerDashboardResponse
import com.uvg.agroconecta.data.models.FarmerLastOrder
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayBorder
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.VerifiedBlue
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: FarmerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    Scaffold(
        containerColor = GrayLight,
        topBar = {
            TopAppBar(
                title = { Text("Mi resumen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenPrimary,
                    titleContentColor = Color.White
                )
            )
        }
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
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.dashboard == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenPrimary
                )
            }
            uiState.errorMessage != null && uiState.dashboard == null -> {
                DashboardError(
                    message = uiState.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.dashboard != null -> DashboardContent(uiState.dashboard)
        }
    }
}

@Composable
private fun DashboardContent(dashboard: FarmerDashboardResponse) {
    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "GT")).apply {
            currency = java.util.Currency.getInstance("GTQ")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardMetricCard(
                title = "Total histórico",
                value = currency.format(dashboard.totalGastadoHistorico),
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            DashboardMetricCard(
                title = "Este mes",
                value = currency.format(dashboard.totalGastadoMesActual),
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
                    amount = currency.format(month.total),
                    progress = if (maximum > 0) (month.total / maximum).toFloat() else 0f
                )
            }
        }

        DashboardSection("Productos más comprados", Icons.Default.Inventory2) {
            if (dashboard.productosMasComprados.isEmpty()) {
                EmptyDashboardMessage("Todavía no hay productos comprados")
            } else {
                dashboard.productosMasComprados.forEachIndexed { index, product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(GreenSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${product.cantidad} unidades", fontSize = 12.sp, color = GrayMid)
                        }
                        Text(
                            currency.format(product.totalGastado),
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index < dashboard.productosMasComprados.lastIndex) {
                        HorizontalDivider(color = GrayBorder.copy(alpha = 0.35f))
                    }
                }
            }
        }

        DashboardSection("Último pedido", Icons.AutoMirrored.Filled.ReceiptLong) {
            dashboard.ultimoPedido?.let { LastOrderCard(it, currency) }
                ?: EmptyDashboardMessage("Todavía no has realizado pedidos")
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 12.sp, color = GrayMid)
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MonthlyBar(month: String, amount: String, progress: Float) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(month, fontSize = 12.sp, color = GrayMid)
            Text(amount, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(GrayLight, RoundedCornerShape(5.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(GreenPrimary, RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
private fun LastOrderCard(order: FarmerLastOrder, currency: NumberFormat) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Pedido #${order.id}", fontWeight = FontWeight.Bold)
            Text(order.distribuidorNombre, fontSize = 13.sp, color = GrayMid)
            Text(formatOrderDate(order.fechaPedido), fontSize = 12.sp, color = GrayMid)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(currency.format(order.totalPedido), fontWeight = FontWeight.Bold)
            Text(
                statusLabel(order.estado),
                color = statusColor(order.estado),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyDashboardMessage(message: String) {
    Text(message, color = GrayMid, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
}

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Analytics, contentDescription = null, tint = GrayBorder, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, color = GrayMid, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
            Text("Reintentar")
        }
    }
}

private fun formatMonth(value: String): String = try {
    YearMonth.parse(value).format(DateTimeFormatter.ofPattern("MMM yy", Locale("es", "GT")))
        .replaceFirstChar { it.uppercase() }
} catch (_: DateTimeParseException) {
    value
}

private fun formatOrderDate(value: String): String = try {
    Instant.parse(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "GT")))
} catch (_: DateTimeParseException) {
    value
}

private fun statusLabel(status: String): String = when (status) {
    "confirmado" -> "Confirmado"
    "preparando" -> "En preparación"
    "en_ruta" -> "En ruta"
    "entregado" -> "Entregado"
    "cancelado" -> "Cancelado"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun statusColor(status: String): Color = when (status) {
    "confirmado" -> VerifiedBlue
    "preparando" -> OrangeAccent
    "en_ruta", "entregado" -> GreenPrimary
    "cancelado" -> ErrorRed
    else -> GrayMid
}
