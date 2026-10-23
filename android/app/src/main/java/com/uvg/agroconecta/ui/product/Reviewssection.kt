package com.uvg.agroconecta.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.Review
import com.uvg.agroconecta.ui.components.rememberAgroHaptics
import com.uvg.agroconecta.ui.components.AgroCard
import com.uvg.agroconecta.ui.components.AgroPrimaryButton
import com.uvg.agroconecta.ui.components.AgroTextField
import com.uvg.agroconecta.ui.components.InlineError
import com.uvg.agroconecta.ui.components.ListItemSkeleton
import com.uvg.agroconecta.ui.components.StarRating
import com.uvg.agroconecta.ui.components.StarRatingInput
import com.uvg.agroconecta.ui.components.formatFriendlyDate
import com.uvg.agroconecta.ui.theme.AgroSpacing
import java.util.Locale

private const val MAX_REVIEW_LENGTH = 300

/**
 * Sección de reseñas del detalle de producto: promedio, formulario para dejar
 * una nueva y la lista de las existentes.
 */
@Composable
fun ReviewsSection(
    reviews: List<Review>,
    promedio: Double?,
    isLoading: Boolean,
    submitState: ReviewSubmitState,
    onSubmit: (calificacion: Int, comentario: String) -> Unit,
    onDismissMsg: () -> Unit
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    val haptics = rememberAgroHaptics()

    // Al enviar con éxito se limpia el formulario para no reenviar lo mismo.
    LaunchedEffect(submitState) {
        if (submitState is ReviewSubmitState.Success) {
            haptics.confirm()
            selectedStars = 0
            comentario = ""
        }
    }

    AgroCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screenHorizontal, vertical = AgroSpacing.xs)
    ) {
        Column(modifier = Modifier.padding(AgroSpacing.md)) {
            ReviewsHeader(promedio = promedio, total = reviews.size)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = AgroSpacing.md)
            )

            Text(
                text = "Deja tu reseña",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            StarRatingInput(selected = selectedStars, onSelect = { selectedStars = it })

            Spacer(Modifier.height(AgroSpacing.xs))

            AgroTextField(
                value = comentario,
                onValueChange = { if (it.length <= MAX_REVIEW_LENGTH) comentario = it },
                label = "Comentario (opcional)",
                placeholder = "¿Cómo te funcionó este producto?",
                supportingText = "${comentario.length}/$MAX_REVIEW_LENGTH",
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )

            AnimatedVisibility(
                visible = submitState is ReviewSubmitState.Error || submitState is ReviewSubmitState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                when (submitState) {
                    is ReviewSubmitState.Error -> InlineError(
                        message = submitState.message,
                        modifier = Modifier.padding(vertical = AgroSpacing.xs)
                    )
                    is ReviewSubmitState.Success -> Text(
                        text = "¡Gracias! Tu reseña se publicó.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(vertical = AgroSpacing.xs)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    else -> Unit
                }
            }

            Spacer(Modifier.height(AgroSpacing.xs))

            AgroPrimaryButton(
                text = if (selectedStars == 0) "Elige las estrellas para enviar" else "Enviar reseña",
                loadingText = "Enviando…",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = {
                    if (selectedStars > 0) {
                        onDismissMsg()
                        onSubmit(selectedStars, comentario.trim())
                    }
                },
                enabled = selectedStars > 0,
                loading = submitState is ReviewSubmitState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            when {
                isLoading -> {
                    Spacer(Modifier.height(AgroSpacing.md))
                    ListItemSkeleton()
                }
                reviews.isEmpty() -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = AgroSpacing.md)
                    )
                    Text(
                        text = "Todavía nadie ha opinado. ¡Sé la primera persona!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = AgroSpacing.md)
                    )
                    reviews.forEach { review ->
                        ReviewItem(review = review)
                        if (review != reviews.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = AgroSpacing.sm)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewsHeader(promedio: Double?, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Reseñas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        if (promedio != null && total > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(rating = promedio, starSize = 18.dp)
                Spacer(Modifier.width(AgroSpacing.xs))
                Text(
                    text = String.format(Locale.US, "%.1f", promedio),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(AgroSpacing.xxs))
                Text(
                    text = "($total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val inicial = review.nombreAgricultor
                ?.firstOrNull()
                ?.uppercaseChar()
                ?.toString()
            if (inicial != null) {
                Text(
                    text = inicial,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(AgroSpacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = review.nombreAgricultor ?: "Agricultor",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            formatFriendlyDate(review.fechaCreacion)?.let { fecha ->
                Text(
                    text = fecha,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(AgroSpacing.xxs))
            StarRating(rating = review.calificacion.toDouble(), starSize = 16.dp)

            review.comentario?.takeIf { it.isNotBlank() }?.let { com ->
                Spacer(Modifier.height(AgroSpacing.xxs))
                Text(
                    text = com,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
