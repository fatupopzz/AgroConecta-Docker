package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvg.agroconecta.ui.cart.CartItemUI

private val GreenPrimary = Color(0xFF2E7D32)
private val GreenSurface = Color(0xFFF1F8E9)
private val GrayMid = Color(0xFF78909C)
private val GrayLight = Color(0xFFECEFF1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    items: List<CartItemUI>,
    total: Double,
    selectedPaymentMethod: String,
    deliveryAddress: String,
    pickupAddress: String?,
    isLoadingPickupAddress: Boolean,
    tipoEntrega: String,
    onDeliveryAddressChange: (String) -> Unit,
    onTipoEntregaChange: (String) -> Unit,
    onConfirmOrder: () -> Unit,
    onBack: () -> Unit
) {
    val canConfirm = items.isNotEmpty() &&
        when (tipoEntrega) {
        "domicilio" -> deliveryAddress.length >= 5

        "recogida" -> {
            !isLoadingPickupAddress &&
            !pickupAddress.isNullOrBlank()
        }

        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Confirmar pedido",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GreenPrimary)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total a pagar",
                            style = MaterialTheme.typography.titleSmall,
                            color = GrayMid
                        )
                        Text(
                            "Q${"%.2f".format(total)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onConfirmOrder,
                        enabled = canConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Confirmar pedido",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Tipo de entrega ──────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Tipo de entrega",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tipoEntrega == "domicilio",
                                onClick = {
                                    onTipoEntregaChange("domicilio")
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GreenPrimary
                                )
                            )

                            Text(
                                "Entrega a domicilio",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = tipoEntrega == "recogida",
                                onClick = {
                                    onTipoEntregaChange("recogida")
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GreenPrimary
                                )
                            )

                            Text(
                                "Recoger en punto",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
// ── Dirección de entrega a domicilio ──────────────────────────
if (tipoEntrega == "domicilio") {
    item {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Dirección de entrega",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = onDeliveryAddressChange,
                    placeholder = {
                        Text(
                            "Ej: Aldea El Tablón, Jalapa",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMid
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    isError = deliveryAddress.isNotBlank() &&
                            deliveryAddress.length < 5,
                    supportingText = {
                        if (
                            deliveryAddress.isNotBlank() &&
                            deliveryAddress.length < 5
                        ) {
                            Text(
                                "Ingresá una dirección más específica",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = GrayLight,
                        cursorColor = GreenPrimary
                    ),
                    maxLines = 3
                )
            }
        }
    }
}

            // ── Dirección del punto de recogida ───────────────────────────
            if (tipoEntrega == "recogida") {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = GreenSurface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    "Punto de recogida",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            when {
                                isLoadingPickupAddress -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = GreenPrimary
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        Text(
                                            "Cargando dirección...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GrayMid
                                        )
                                    }
                                }

                                pickupAddress.isNullOrBlank() -> {
                                    Text(
                                        "Dirección no disponible",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                else -> {
                                    Text(
                                        text = pickupAddress,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // ── Método de pago ────────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Método de pago",
                                style = MaterialTheme.typography.labelMedium,
                                color = GrayMid
                            )
                            Text(
                                "Pago contra entrega",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GreenPrimary
                            )
                        }
                    }
                }
            }

            // ── Resumen de productos ──────────────────────────────────────
            item {
                Text(
                    "Resumen del pedido (${items.size} productos)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37474F)
                )
            }

            items(items) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ícono producto
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.distribuidor,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMid
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = GrayLight
                            ) {
                                Text(
                                    text = "Cantidad: ${item.cantidad}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF37474F),
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 3.dp
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Q${"%.2f".format(item.precio * item.cantidad)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}