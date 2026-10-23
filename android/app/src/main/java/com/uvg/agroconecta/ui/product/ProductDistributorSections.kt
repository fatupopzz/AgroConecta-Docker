package com.uvg.agroconecta.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.DistributorCompare
import com.uvg.agroconecta.data.models.DistributorOffer
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.PriceComparison
import com.uvg.agroconecta.ui.components.DistributorRatingCard
import com.uvg.agroconecta.ui.components.SkeletonBox
import com.uvg.agroconecta.ui.components.StatusTag
import com.uvg.agroconecta.ui.components.TagTone
import com.uvg.agroconecta.ui.components.formatQuetzales
import com.uvg.agroconecta.ui.theme.AgroSizes
import com.uvg.agroconecta.ui.theme.AgroSpacing
import com.uvg.agroconecta.ui.theme.tabularNumbers

@Composable
internal fun DistributorOffersSection(
    offers: List<DistributorOffer>,
    selectedOffer: DistributorOffer?,
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean,
    onOfferSelected: (DistributorOffer) -> Unit
) {
    ProductSectionCard(title = "¿A quién le compras?") {
        if (offers.isEmpty()) {
            Text(
                text = "Ahora ningún distribuidor tiene este producto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                offers.forEach { offer ->
                    val isSelected = selectedOffer?.idInventario == offer.idInventario
                    DistributorOfferItem(
                        offer = offer,
                        isSelected = isSelected,
                        distributorRating = if (isSelected) distributorRating else null,
                        isLoadingRating = isSelected && isLoadingRating,
                        onClick = { onOfferSelected(offer) }
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
            IconButton(onClick = { onExpandedChange(!isExpanded) }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Ocultar comparación" else "Ver comparación",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column {
                comparison?.let { currentComparison ->
                    currentComparison.precioMasBajo?.let { lowestPrice ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(AgroSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(AgroSizes.iconSmall)
                                )
                                Spacer(Modifier.width(AgroSpacing.xs))
                                Text(
                                    text = "Mejor precio: ${formatQuetzales(lowestPrice)}",
                                    style = MaterialTheme.typography.labelLarge.tabularNumbers
                                )
                            }
                        }
                        Spacer(Modifier.size(AgroSpacing.sm))
                    }
                    currentComparison.distribuidores.forEach { distributor ->
                        DistributorComparisonItem(distributor = distributor)
                        if (distributor != currentComparison.distribuidores.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = AgroSpacing.xs)
                            )
                        }
                    }
                } ?: SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
            }
        }
        if (!isExpanded) {
            Text(
                text = comparison?.let {
                    val count = it.distribuidores.size
                    if (count == 1) "1 distribuidor lo vende" else "$count distribuidores lo venden"
                } ?: "Toca para ver precios de otros distribuidores",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Oferta de un distribuidor. Toda la fila es el área táctil y se anuncia como
 * opción de radio, en vez de solo el círculo de 20 dp.
 */
@Composable
private fun DistributorOfferItem(
    offer: DistributorOffer,
    isSelected: Boolean,
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean,
    onClick: () -> Unit
) {
    val isVerified = offer.estadoVerificacion == "verificado"
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
    ) {
        Row(
            modifier = Modifier.padding(end = AgroSpacing.sm, top = AgroSpacing.xs, bottom = AgroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // onClick nulo: la fila completa ya maneja el toque y la semántica.
            RadioButton(selected = isSelected, onClick = null, modifier = Modifier.padding(AgroSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.distribuidor,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isVerified) {
                    StatusTag(
                        text = "Verificado",
                        tone = TagTone.Info,
                        icon = Icons.Default.Verified,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Text(
                    text = "Disponibles: ${offer.stock} ${offer.unidadMedida ?: "unidades"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Spacer(Modifier.size(AgroSpacing.xs))
                    DistributorRating(
                        distributorRating = distributorRating,
                        isLoadingRating = isLoadingRating
                    )
                }
            }
            Text(
                text = formatQuetzales(offer.precio),
                style = MaterialTheme.typography.titleMedium.tabularNumbers,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DistributorRating(
    distributorRating: DistributorRatingResponse?,
    isLoadingRating: Boolean
) {
    when {
        isLoadingRating -> SkeletonBox(Modifier.size(width = 180.dp, height = 32.dp))

        distributorRating != null && distributorRating.totalResenas > 0 -> {
            DistributorRatingCard(
                rating = distributorRating.calificacionPromedio,
                totalReviews = distributorRating.totalResenas
            )
        }

        else -> {
            Text(
                text = "Este distribuidor aún no tiene reseñas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DistributorComparisonItem(distributor: DistributorCompare) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = distributor.nombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (distributor.esPrecioMasBajo) {
                StatusTag(text = "Mejor precio", tone = TagTone.Success, modifier = Modifier.padding(vertical = 2.dp))
            }
            Text(
                text = "Disponibles: ${distributor.stock} ${distributor.unidadMedida ?: "unidades"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatQuetzales(distributor.precio),
            style = (
                if (distributor.esPrecioMasBajo) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                }
                ).tabularNumbers,
            color = if (distributor.esPrecioMasBajo) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
