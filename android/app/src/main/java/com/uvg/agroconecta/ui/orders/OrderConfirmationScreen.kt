package com.uvg.agroconecta.ui.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.orders.checkout.CheckoutValidation
import com.uvg.agroconecta.ui.theme.AgroSpacing

@Composable
fun OrderConfirmationScreen(
    items: List<CartItemUI>,
    total: Double,
    deliveryAddress: String,
    pickupAddress: String?,
    isLoadingPickupAddress: Boolean,
    tipoEntrega: String,
    isCreatingOrder: Boolean,
    errorMessage: String?,
    canSubmit: Boolean,
    canRetry: Boolean,
    onRetryOrder: () -> Unit,
    onDeliveryAddressChange: (String) -> Unit,
    onTipoEntregaChange: (String) -> Unit,
    onConfirmOrder: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = isCreatingOrder) { }
    val canConfirm = canSubmit && !isCreatingOrder && CheckoutValidation.canConfirm(
        hasItems = items.isNotEmpty(),
        deliveryAddress = deliveryAddress,
        pickupAddress = pickupAddress,
        isLoadingPickupAddress = isLoadingPickupAddress,
        deliveryType = tipoEntrega
    )

    Scaffold(
        topBar = { OrderConfirmationTopBar(onBack, enabled = !isCreatingOrder) },
        bottomBar = {
            OrderConfirmationBottomBar(
                total = total,
                canConfirm = canConfirm,
                onConfirmOrder = onConfirmOrder,
                isCreatingOrder = isCreatingOrder,
                errorMessage = errorMessage,
                canRetry = canRetry && !isCreatingOrder,
                onRetryOrder = onRetryOrder
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
        ) {
            item {
                DeliveryTypeCard(
                    deliveryType = tipoEntrega,
                    enabled = !isCreatingOrder,
                    onDeliveryTypeChange = onTipoEntregaChange
                )
            }

            if (tipoEntrega == "domicilio") {
                item {
                    DeliveryAddressCard(
                        deliveryAddress = deliveryAddress,
                        enabled = !isCreatingOrder,
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
            item { OrderSummaryHeader(items.size, items.firstOrNull()?.distribuidor) }
            items(items, key = { it.id }) { item -> OrderSummaryItemCard(item) }
        }
    }
}
