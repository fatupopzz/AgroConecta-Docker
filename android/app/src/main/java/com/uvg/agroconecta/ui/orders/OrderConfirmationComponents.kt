package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI

internal val CheckoutGreenPrimary = Color(0xFF2E7D32)
internal val CheckoutGreenSurface = Color(0xFFF1F8E9)
internal val CheckoutGrayMid = Color(0xFF78909C)
internal val CheckoutGrayLight = Color(0xFFECEFF1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderConfirmationTopBar(onBack: () -> Unit) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CheckoutGreenPrimary
        )
    )
}

@Composable
internal fun OrderConfirmationBottomBar(
    total: Double,
    canConfirm: Boolean,
    onConfirmOrder: () -> Unit
) {
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
                    color = CheckoutGrayMid
                )
                Text(
                    "Q${"%.2f".format(total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CheckoutGreenPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onConfirmOrder,
                enabled = canConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CheckoutGreenPrimary
                ),
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

@Composable
internal fun DeliveryTypeCard(
    deliveryType: String,
    onDeliveryTypeChange: (String) -> Unit
) {
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
            DeliveryTypeOption(
                selected = deliveryType == "domicilio",
                label = "Entrega a domicilio",
                onClick = { onDeliveryTypeChange("domicilio") }
            )
            DeliveryTypeOption(
                selected = deliveryType == "recogida",
                label = "Recoger en punto",
                onClick = { onDeliveryTypeChange("recogida") }
            )
        }
    }
}

@Composable
private fun DeliveryTypeOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = CheckoutGreenPrimary
            )
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
internal fun DeliveryAddressCard(
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CheckoutGreenPrimary,
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
                        color = CheckoutGrayMid
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                isError = deliveryAddress.isNotBlank() && deliveryAddress.length < 5,
                supportingText = {
                    if (deliveryAddress.isNotBlank() && deliveryAddress.length < 5) {
                        Text(
                            "Ingresá una dirección más específica",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CheckoutGreenPrimary,
                    unfocusedBorderColor = CheckoutGrayLight,
                    cursorColor = CheckoutGreenPrimary
                ),
                maxLines = 3
            )
        }
    }
}

@Composable
internal fun PickupAddressCard(
    pickupAddress: String?,
    isLoadingPickupAddress: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CheckoutGreenSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = CheckoutGreenPrimary,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CheckoutGreenPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cargando dirección...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CheckoutGrayMid
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

@Composable
internal fun PaymentMethodCard() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CheckoutGreenSurface),
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
                tint = CheckoutGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Método de pago",
                    style = MaterialTheme.typography.labelMedium,
                    color = CheckoutGrayMid
                )
                Text(
                    "Pago contra entrega",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CheckoutGreenPrimary
                )
            }
        }
    }
}

@Composable
internal fun OrderSummaryHeader(itemCount: Int) {
    Text(
        "Resumen del pedido ($itemCount productos)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF37474F)
    )
}

@Composable
internal fun OrderSummaryItemCard(item: CartItemUI) {
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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CheckoutGreenSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Eco,
                        contentDescription = null,
                        tint = CheckoutGreenPrimary,
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
                    color = CheckoutGrayMid
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CheckoutGrayLight
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
                text = "Q${"%.2f".format(item.subtotal)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CheckoutGreenPrimary
            )
        }
    }
}
