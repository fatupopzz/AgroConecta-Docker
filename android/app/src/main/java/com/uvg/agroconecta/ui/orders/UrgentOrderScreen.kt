package com.uvg.agroconecta.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.cart.CartItemUI
import com.uvg.agroconecta.ui.theme.ErrorRed
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.OrangeLight
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
    if (keywords.isEmpty()) return cartItems.first()

    val scoredItems = cartItems.map { item ->
        item to keywords.count { keyword ->
            item.nombre.normalizedForSearch().contains(keyword)
        }
    }
    val best = scoredItems.maxByOrNull { (_, score) -> score }
    return if (best != null && best.second > 0) best.first else cartItems.first()
}

private fun String.normalizedForSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .lowercase(Locale.ROOT)
    .trim()

@OptIn(ExperimentalMaterial3Api::class)
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
    var pestMenuExpanded by rememberSaveable { mutableStateOf(false) }

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
        deliveryAddress.trim().length >= 5 &&
        !isSubmitting

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Entrega urgente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ErrorRed)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        selectedProduct?.let { product ->
                            onConfirmUrgentOrder(product, selectedPest)
                        }
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Confirmar pedido urgente")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = OrangeLight,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OrangeAccent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = ErrorRed
                        )
                        Column {
                            Text(
                                text = "Atención prioritaria",
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                            Text(
                                text = "El distribuidor recibirá este pedido marcado como urgente.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "1. Tipo de plaga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { pestMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedPest.ifBlank { "Selecciona la plaga detectada" },
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = pestMenuExpanded,
                        onDismissRequest = { pestMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        urgentPestTypes.forEach { pest ->
                            DropdownMenuItem(
                                text = { Text(pest) },
                                onClick = {
                                    selectedPest = pest
                                    pestMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "2. Producto recomendado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Elige el insumo del carrito que deseas recibir primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid
                )
            }

            if (cartItems.isEmpty()) {
                item {
                    Text(
                        text = "Agrega un producto al carrito antes de solicitar la entrega urgente.",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(cartItems, key = { it.idInventario }) { product ->
                    val isSelected = product.idInventario == selectedInventoryId
                    val isRecommended = selectedPest.isNotBlank() &&
                        product.idInventario == recommendedProduct?.idInventario

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedInventoryId = product.idInventario },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) OrangeLight else Color.White
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) OrangeAccent else Color(0xFFE0E0E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedInventoryId = product.idInventario }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(product.nombre, fontWeight = FontWeight.SemiBold)
                                    if (isRecommended) {
                                        Surface(
                                            color = OrangeAccent,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Recomendado",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 3.dp
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${product.distribuidor} · Cantidad ${product.cantidad}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrayMid
                                )
                                Text(
                                    text = "Q${"%.2f".format(product.precio * product.cantidad)}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "3. Dirección de entrega",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = onDeliveryAddressChange,
                    label = { Text("Dirección de la finca") },
                    supportingText = {
                        Text("Verifica la dirección para evitar demoras.")
                    },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
