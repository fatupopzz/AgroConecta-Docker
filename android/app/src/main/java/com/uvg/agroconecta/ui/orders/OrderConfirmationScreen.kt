package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI

@Composable
fun OrderConfirmationScreen(
    items: List<CartItemUI>,
    total: Double,
    selectedPaymentMethod: String,
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit,
    onConfirmOrder: () -> Unit,
    onBack: () -> Unit
) {
    val isCashPayment = selectedPaymentMethod == "efectivo"
    val canConfirm = deliveryAddress.isNotBlank() && items.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Confirmar pedido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = deliveryAddress,
            onValueChange = onDeliveryAddressChange,
            label = { Text("Dirección de entrega") },
            modifier = Modifier.fillMaxWidth(),
            isError = deliveryAddress.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isCashPayment) {
            Text(
                text = "Pague al recibir su pedido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Resumen del pedido",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(item.nombre, fontWeight = FontWeight.Bold)
                        Text("Distribuidor: ${item.distribuidor}")
                        Text("Cantidad: ${item.cantidad}")
                        Text("Precio unitario: Q${item.precio}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Total: Q$total",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Regresar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onConfirmOrder,
            modifier = Modifier.fillMaxWidth(),
            enabled = canConfirm
        ) {
            Text("Confirmar pedido")
        }
    }
}