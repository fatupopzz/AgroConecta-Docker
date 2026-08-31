package com.uvg.agroconecta.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.DistributorCompare
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.PriceComparison
import com.uvg.agroconecta.ui.components.DistributorRatingCard
import com.uvg.agroconecta.ui.theme.GrayDark
import com.uvg.agroconecta.ui.theme.GrayLight
import com.uvg.agroconecta.ui.theme.GrayMid
import com.uvg.agroconecta.ui.theme.GreenPrimary
import com.uvg.agroconecta.ui.theme.GreenSurface
import com.uvg.agroconecta.ui.theme.OrangeAccent
import com.uvg.agroconecta.ui.theme.OrangeLight

@Composable
internal fun DistributorOffersSection(
    offers: List<DistributorOffer>,
    selectedOffer: DistributorOffer?,
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean,
    onOfferSelected: (DistributorOffer) -> Unit
) {
    ProductSectionCard(title = "Distribuidores disponibles") {
        if (offers.isEmpty()) {
            Text(
                text = "No hay distribuidores disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMid
            )
        } else {
            offers.forEach { offer ->
                val isSelected = selectedOffer?.idInventario == offer.idInventario
                DistributorOfferItem(
                    offer = offer,
                    isSelected = isSelected,
                    distributorRating = if (isSelected) distributorRating else null,
                    isLoadingRating = isSelected && isLoadingRating,
                    onClick = { onOfferSelected(offer) }
                )
                if (offer != offers.last()) {
                    HorizontalDivider(
                        color = GrayLight,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PriceComparisonSection(
    comparison: PriceComparison?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ProductSectionCard(
        title = "Comparar precios",
        trailing = {
            IconButton(
                onClick = { onExpandedChange(!isExpanded) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = GreenPrimary
                )
            }
        }
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column {
                comparison?.let { currentComparison ->
                    currentComparison.precioMasBajo?.let { lowestPrice ->
                        Surface(
                            color = GreenSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Mejor precio: Q${"%.2f".format(lowestPrice)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = GreenPrimary
                                )
                            }
                        }
                        Spacer(Modifier.size(12.dp))
                    }
                    currentComparison.distribuidores.forEach { distributor ->
                        DistributorComparisonItem(distributor = distributor)
                        if (distributor != currentComparison.distribuidores.last()) {
                            HorizontalDivider(
                                color = GrayLight,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } ?: Text(
                    text = "Cargando comparación...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMid
                )
            }
        }
        if (!isExpanded) {
            Text(
                text = comparison?.let {
                    "${it.distribuidores.size} distribuidor(es) disponibles"
                } ?: "Toca para ver precios de otros distribuidores",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMid
            )
        }
    }
}

@Composable
private fun DistributorOfferItem(
    offer: DistributorOffer,
    isSelected: Boolean,
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean,
    onClick: () -> Unit
) {
    val isVerified = offer.estadoVerificacion == "verificado"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, GreenPrimary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .background(if (isSelected) GreenSurface else Color.Transparent)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = offer.distribuidor,
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayDark
                )
                if (isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verificado",
                        tint = GreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.size(2.dp))
            Text(
                text = "Stock: ${offer.stock} ${offer.unidadMedida ?: "unidades"}",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMid
            )
            if (isSelected) {
                Spacer(Modifier.size(8.dp))
                DistributorRating(
                    distributorRating = distributorRating,
                    isLoadingRating = isLoadingRating
                )
            }
        }
        Text(
            text = "Q${"%.2f".format(offer.precio)}",
            style = MaterialTheme.typography.titleMedium,
            color = GreenPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DistributorRating(
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean
) {
    when {
        isLoadingRating -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = GreenPrimary,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cargando rating del distribuidor...",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid
                )
            }
        }

        distributorRating != null && distributorRating.totalResenas > 0 -> {
            DistributorRatingCard(
                rating = distributorRating.calificacionPromedio,
                totalReviews = distributorRating.totalResenas
            )
        }

        else -> {
            Text(
                text = "Aún no hay reseñas de este distribuidor",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMid
            )
        }
    }
}

@Composable
private fun DistributorComparisonItem(distributor: DistributorCompare) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = distributor.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayDark
                )
                if (distributor.esPrecioMasBajo) {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = OrangeLight, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "Mejor precio",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangeAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = "Stock: ${distributor.stock} ${distributor.unidadMedida ?: "unidades"}",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMid
            )
        }
        Text(
            text = "Q${"%.2f".format(distributor.precio)}",
            style = MaterialTheme.typography.titleSmall,
            color = if (distributor.esPrecioMasBajo) GreenPrimary else GrayDark,
            fontWeight = if (distributor.esPrecioMasBajo) FontWeight.Bold else FontWeight.Normal
        )
    }
}
