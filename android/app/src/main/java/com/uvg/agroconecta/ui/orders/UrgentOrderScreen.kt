package com.uvg.agroconecta.ui.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.components.AgroDropdownField
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.AgroTopAppBar
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.RadioOption
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.agroColors
import com.uvg.agroconecta.ui.theme.tabularNumbers
import java.text.Normalizer
import java.util.Locale

val urgentPestTypes = listOf(
    "Pulgón",
    "Mosca blanca",
    "Trips",
    "Gusano cogollero",
    "Hongos",
    "Ácaros",
    "Otra plaga"
)

private val pestProductKeywords = mapOf(
    "pulgon" to listOf("insecticida", "neem", "control biologico"),
    "mosca blanca" to listOf("insecticida", "neem", "jabon potasico"),
    "trips" to listOf("insecticida", "spinosad", "neem"),
    "gusano cogollero" to listOf("bacillus", "insecticida", "control biologico"),
    "hongos" to listOf("fungicida", "cobre", "azufre"),
    "acaros" to listOf("acaricida", "azufre", "neem")
)

internal fun recommendUrgentProduct(
    cartItems: List<CartItemUI>,
    pestType: String
): CartItemUI? {
    if (cartItems.isEmpty()) return null

    val keywords = pestProductKeywords[pestType.normalizedForSearch()].orEmpty()
    if (keywords.isEmpty()) return null

    val scoredItems = cartItems.map { item ->
        item to keywords.count { keyword ->
            item.nombre.normalizedForSearch().contains(keyword)
        }
    }
    val best = scoredItems.maxByOrNull { (_, score) -> score }
    return best?.takeIf { (_, score) -> score > 0 }?.first
}

private fun String.normalizedForSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .lowercase(Locale.ROOT)
    .trim()

@Composable
fun UrgentOrderScreen(
    cartItems: List<CartItemUI>,
    deliveryAddress: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDeliveryAddressChange: (String) -> Unit,
    onConfirmUrgentOrder: (product: CartItemUI, pestType: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedPest by rememberSaveable { mutableStateOf("") }
    var selectedInventoryId by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(enabled = isSubmitting) {
        // Evita abandonar la pantalla mientras el servidor procesa el pedido.
    }

    val recommendedProduct = recommendUrgentProduct(cartItems, selectedPest)
    val selectedProduct = cartItems.firstOrNull {
        it.idInventario == selectedInventoryId
    }

    LaunchedEffect(selectedPest, cartItems) {
        val currentSelectionIsValid = cartItems.any {
            it.idInventario == selectedInventoryId
        }
        if (!currentSelectionIsValid || selectedPest.isNotBlank()) {
            selectedInventoryId = recommendedProduct?.idInventario
        }
    }

    val canConfirm = selectedPest.isNotBlank() &&
        selectedProduct != null &&
        deliveryAddress.trim().length >= 5

    Scaffold(
        topBar = {
            AgroTopAppBar(title = "Entrega urgente", onBack = onBack, backEnabled = !isSubmitting)
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                AgroPrimaryButton(
                    text = "Pedir entrega urgente",
                    loadingText = "Enviando pedido…",
                    icon = Icons.Default.Warning,
                    onClick = {
                        selectedProduct?.let { product ->
                            onConfirmUrgentOrder(product, selectedPest)
                        }
                    },
                    enabled = canConfirm,
                    loading = isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.sm)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(AgroSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.md)
        ) {
            item(key = "notice") {
                Surface(
                    color = MaterialTheme.agroColors.warningContainer,
                    contentColor = MaterialTheme.agroColors.onWarningContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(AgroSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null)
                        Column {
                            Text(
                                text = "Atención prioritaria",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "El distribuidor verá este pedido primero, marcado como urgente.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item(key = "pest") {
                Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                    StepTitle("1. ¿Qué plaga encontraste?")
                    AgroDropdownField(
                        label = "Plaga",
                        options = urgentPestTypes,
                        selected = selectedPest.ifBlank { null },
                        onSelectedChange = { selectedPest = it },
                        optionLabel = { it },
                        enabled = !isSubmitting
                    )
                }
            }

            item(key = "product-title") {
                Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xxs)) {
                    StepTitle("2. ¿Qué producto necesitas primero?")
                    Text(
                        text = "Elige del carrito el que debe llegar antes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedPest.isNotBlank() && recommendedProduct == null) {
                        Text(
                            text = "No encontramos uno que encaje solo. Elige el que te sirva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.agroColors.warning
                        )
                    }
                }
            }

            if (cartItems.isEmpty()) {
                item(key = "empty-cart") {
                    InlineError(message = "Agrega un producto al carrito antes de pedir entrega urgente.")
                }
            } else {
                item(key = "products") {
                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
                    ) {
                        cartItems.forEach { product ->
                            val isRecommended = selectedPest.isNotBlank() &&
                                product.idInventario == recommendedProduct?.idInventario
                            RadioOption(
                                label = product.nombre,
                                description = "${product.distribuidor} · ${product.cantidad} en el carrito",
                                selected = product.idInventario == selectedInventoryId,
                                enabled = !isSubmitting,
                                onClick = { selectedInventoryId = product.idInventario },
                                trailing = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (isRecommended) {
                                            StatusTag(text = "Recomendado", tone = TagTone.Success)
                                        }
                                        Text(
                                            text = formatQuetzales(product.precio * product.cantidad),
                                            style = MaterialTheme.typography.titleSmall.tabularNumbers,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item(key = "address") {
                Column(verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)) {
                    StepTitle("3. ¿A dónde lo llevamos?")
                    AgroTextField(
                        value = deliveryAddress,
                        onValueChange = onDeliveryAddressChange,
                        label = "Dirección de la finca",
                        supportingText = "Revísala para que no haya demoras.",
                        singleLine = false,
                        minLines = 2,
                        enabled = !isSubmitting
                    )
                }
            }

            errorMessage?.let { message ->
                item(key = "error") { InlineError(message = message) }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() }
    )
}
