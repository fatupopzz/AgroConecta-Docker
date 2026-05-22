package com.uvg.agroconecta.ui.payment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PaymentMethod(
    val id: String,
    val title: String,
    val description: String,
    val isAvailable: Boolean
)

@Composable
fun PaymentMethodScreen(
    selectedMethod: String?,
    onMethodSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val methods = listOf(
        PaymentMethod(
            id = "efectivo",
            title = "Efectivo",
            description = "Pago contra entrega",
            isAvailable = true
        ),
        PaymentMethod(
            id = "tigo_money",
            title = "Tigo Money",
            description = "Pago desde billetera móvil",
            isAvailable = false
        ),
        PaymentMethod(
            id = "banrural_movil",
            title = "Banrural Móvil",
            description = "Pago desde banca móvil",
            isAvailable = false
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Selecciona cómo deseas pagar tu pedido.")

        Spacer(modifier = Modifier.height(16.dp))

        methods.forEach { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable(enabled = method.isAvailable) {
                        onMethodSelected(method.id)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMethod == method.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = method.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(method.description)

                    if (!method.isAvailable) {
                        Spacer(modifier = Modifier.height(8.dp))

                        AssistChip(
                            onClick = { },
                            label = {
                                Text("Próximamente disponible")
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Regresar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedMethod == "efectivo"
        ) {
            Text("Continuar")
        }
    }
}