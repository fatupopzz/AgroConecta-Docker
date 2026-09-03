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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.DistributorStatsResponse
import com.uvg.agroconecta.data.models.OrdersByStatus
import com.uvg.agroconecta.data.models.TopSellingProduct
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayBorder
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenLight
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.VerifiedBlue
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DistributorStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var distributorId by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        distributorId = SessionManager.getPerfilId(context).first() ?: -1
        viewModel.loadStats(distributorId)
    }

    Scaffold(
        containerColor = GrayLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Estadísticas de ventas",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.stats == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GreenPrimary
                    )
                }

                uiState.errorMessage != null && uiState.stats == null -> {
                    StatsError(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = {
                            viewModel.loadStats(distributorId)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.stats != null -> {
                    StatsContent(stats = uiState.stats!!)
                }
            }
        }
    }
}

@Composable
private fun StatsContent(stats: DistributorStatsResponse) {
    val numberFormatter = remember {
        NumberFormat.getNumberInstance(Locale("es", "GT")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
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
            MetricCard(
                title = "Total de pedidos",
                value = stats.totalPedidos.toString(),
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Ingresos totales",
                value = "Q ${numberFormatter.format(stats.ingresosTotales)}",
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
        }

        StatsSection(
            title = "Productos más vendidos",
            icon = Icons.Default.Inventory2
        ) {
            if (stats.productosMasVendidos.isEmpty()) {
                EmptySectionMessage("Aún no hay productos en pedidos entregados")
            } else {
                stats.productosMasVendidos.forEachIndexed { index, product ->
                    TopProductRow(
                        position = index + 1,
                        product = product,
                        formattedIncome = "Q ${numberFormatter.format(product.ingresos)}"
                    )
                    if (index < stats.productosMasVendidos.lastIndex) {
                        HorizontalDivider(color = GrayBorder.copy(alpha = 0.35f))
                    }
                }
            }
        }

        StatsSection(
            title = "Pedidos por estado",
            icon = Icons.Default.Analytics
        ) {
            stats.pedidosPorEstado.forEachIndexed { index, item ->
                OrderStatusRow(item)
                if (index < stats.pedidosPorEstado.lastIndex) {
                    HorizontalDivider(color = GrayBorder.copy(alpha = 0.35f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MetricCard(
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
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GreenSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(text = title, fontSize = 12.sp, color = GrayMid)
        }
    }
}

@Composable
private fun StatsSection(
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TopProductRow(
    position: Int,
    product: TopSellingProduct,
    formattedIncome: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(GreenSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(product.nombre, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${product.cantidad} unidades vendidas",
                fontSize = 12.sp,
                color = GrayMid
            )
        }
        Text(
            text = formattedIncome,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary
        )
    }
}

@Composable
private fun OrderStatusRow(item: OrdersByStatus) {
    val color = statusColor(item.estado)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = statusLabel(item.estado),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = item.cantidad.toString(),
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        color = GrayMid,
        fontSize = 13.sp
    )
}

@Composable
private fun StatsError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Analytics,
            contentDescription = null,
            tint = GrayBorder,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            color = GrayMid,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Reintentar")
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "confirmado" -> "Confirmados"
    "preparando" -> "En preparación"
    "en_ruta" -> "En ruta"
    "entregado" -> "Entregados"
    "cancelado" -> "Cancelados"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun statusColor(status: String): Color = when (status) {
    "confirmado" -> VerifiedBlue
    "preparando" -> OrangeAccent
    "en_ruta" -> GreenLight
    "entregado" -> GreenPrimary
    "cancelado" -> ErrorRed
    else -> GrayMid
}
