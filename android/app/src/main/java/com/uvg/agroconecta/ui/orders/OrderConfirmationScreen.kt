package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutValidation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    items: List<CartItemUI>,
    total: Double,
    deliveryAddress: String,
    pickupAddress: String?,
    isLoadingPickupAddress: Boolean,
    tipoEntrega: String,
    onDeliveryAddressChange: (String) -> Unit,
    onTipoEntregaChange: (String) -> Unit,
    onConfirmOrder: () -> Unit,
    onBack: () -> Unit
) {
    val canConfirm = CheckoutValidation.canConfirm(
        hasItems = items.isNotEmpty(),
        deliveryAddress = deliveryAddress,
        pickupAddress = pickupAddress,
        isLoadingPickupAddress = isLoadingPickupAddress,
        deliveryType = tipoEntrega
    )

    Scaffold(
        topBar = { OrderConfirmationTopBar(onBack) },
        bottomBar = {
            OrderConfirmationBottomBar(
                total = total,
                canConfirm = canConfirm,
                onConfirmOrder = onConfirmOrder
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DeliveryTypeCard(
                    deliveryType = tipoEntrega,
                    onDeliveryTypeChange = onTipoEntregaChange
                )
            }

            if (tipoEntrega == "domicilio") {
                item {
                    DeliveryAddressCard(
                        deliveryAddress = deliveryAddress,
                        onDeliveryAddressChange = onDeliveryAddressChange
                    )
                }
            }

            if (tipoEntrega == "recogida") {
                item {
                    PickupAddressCard(
                        pickupAddress = pickupAddress,
                        isLoadingPickupAddress = isLoadingPickupAddress
                    )
                }
            }

            item { PaymentMethodCard() }
            item { OrderSummaryHeader(items.size) }
            items(items) { item -> OrderSummaryItemCard(item) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
