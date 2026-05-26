package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.GreenPrimary
import kotlinx.coroutines.flow.first

private val GreenPrimary = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    orders: List<OrderSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    onTrackOrder: (Int) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val context = LocalContext.current
    var tipoUsuario by remember { mutableStateOf("agricultor") }

    LaunchedEffect(Unit) {
        tipoUsuario = SessionManager.getTipoUsuario(context).first() ?: "agricultor"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mis pedidos",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isLoading && orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aún no tienes pedidos registrados.",
                        color = Color(0xFF78909C)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Pedido #${order.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Estado: ${order.estado}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Total: Q${order.totalPedido}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Fecha: ${order.fechaPedido.take(10)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78909C)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionChip(
                                        onClick = { },
                                        enabled = false,
                                        label = { Text("Pago contra entrega") }
                                    )
                                    TextButton(onClick = { onTrackOrder(order.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Seguimiento")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}