package com.uvg.agroconecta.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.ProductDetail
import com.uvg.agroconecta.ui.theme.GrayDark
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPale
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenPrimaryDark
import com.uvg.agroconecta.ui.theme.GreenSurface

@Composable
internal fun ProductOverviewSection(
    product: ProductDetail,
    selectedOffer: DistributorOffer?,
    isFarmer: Boolean,
    isFollowingPrice: Boolean,
    isUpdatingFollow: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProductImageGallery()
        Spacer(Modifier.height(12.dp))
        product.categoria?.let { category ->
            Surface(color = GreenPale, shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium,
                    color = GreenPrimaryDark,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = product.nombre,
            style = MaterialTheme.typography.headlineSmall,
            color = GrayDark,
            textAlign = TextAlign.Center
        )
        product.marca?.let { brand ->
            Spacer(Modifier.height(4.dp))
            Text(text = brand, style = MaterialTheme.typography.bodyMedium, color = GrayMid)
        }
        selectedOffer?.let { offer ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Q${"%.2f".format(offer.precio)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "/ ${offer.unidadMedida ?: "unidad"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        if (isFarmer) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onToggleFollow,
                enabled = !isUpdatingFollow,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isFollowingPrice) GreenPrimaryDark else GreenPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUpdatingFollow) {
                    CircularProgressIndicator(
                        color = GreenPrimary,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isFollowingPrice) {
                            Icons.Default.NotificationsActive
                        } else {
                            Icons.Default.NotificationsNone
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFollowingPrice) "Dejar de seguir precio" else "Seguir precio",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
internal fun ProductImageGallery() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GreenPale),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
internal fun ProductTechnicalSheetSection(product: ProductDetail) {
    ProductSectionCard(title = "Ficha técnica") {
        val hasTechnicalSheet = !product.descripcion.isNullOrBlank() ||
            !product.composicion.isNullOrBlank() ||
            !product.dosis.isNullOrBlank() ||
            !product.instrucciones.isNullOrBlank()

        if (!hasTechnicalSheet) {
            Text(
                text = "Este producto aún no tiene ficha técnica completa.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMid
            )
        } else {
            product.descripcion?.let { TechnicalSheetRow("Descripción", it) }
            product.composicion?.let { TechnicalSheetRow("Composición", it) }
            product.dosis?.let { TechnicalSheetRow("Dosis recomendada", it) }
            product.instrucciones?.let { TechnicalSheetRow("Instrucciones de uso", it) }
        }
    }
}

@Composable
internal fun ProductSectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = GrayDark)
                trailing?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TechnicalSheetRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = GrayMid)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = GrayDark)
    }
    Spacer(Modifier.height(8.dp))
}
