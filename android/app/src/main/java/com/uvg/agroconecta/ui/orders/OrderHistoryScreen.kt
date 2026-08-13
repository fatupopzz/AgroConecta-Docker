package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.OrderSummary
import com.uvg.agroconecta.ui.components.AppBottomBar
import com.uvg.agroconecta.ui.components.BottomNavTab
import com.uvg.agroconecta.ui.theme.GreenPrimary

private val UrgentBackground = Color(0xFFFFEBEE)
private val UrgentRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    orders: List<OrderSummary>,
    isLoading: Boolean,
    errorMessage: String?,
    tipoUsuario: String,
    onTrackOrder: (Int) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAgregarClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val isDistributor = tipoUsuario == "distribuidor"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isDistributor) "Pedidos recibidos" else "Mis pedidos",
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
                        if (isDistributor) {
                            "Aún no has recibido pedidos."
                        } else {
                            "Aún no tienes pedidos registrados."
                        },
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
                            colors = CardDefaults.cardColors(
                                containerColor = if (order.esUrgente) {
                                    UrgentBackground
                                } else {
                                    Color.White
                                }
                            ),
                            border = if (order.esUrgente) {
                                BorderStroke(2.dp, UrgentRed)
                            } else {
                                null
                            },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pedido #${order.id}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (order.esUrgente) {
                                        Surface(
                                            color = UrgentRed,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = "URGENTE",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val counterparty = if (isDistributor) {
                                    order.agricultorNombre
                                } else {
                                    order.distribuidorNombre
                                }
                                counterparty?.let {
                                    Text(
                                        text = if (isDistributor) {
                                            "Agricultor: $it"
                                        } else {
                                            "Distribuidor: $it"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (order.esUrgente && !order.tipoPlaga.isNullOrBlank()) {
                                    Text(
                                        text = "Plaga detectada: ${order.tipoPlaga}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = UrgentRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
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
