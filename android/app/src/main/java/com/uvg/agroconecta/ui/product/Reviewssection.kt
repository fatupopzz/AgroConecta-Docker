package com.uvg.agroconecta.ui.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uvg.agroconecta.data.models.Review
import com.uvg.agroconecta.ui.theme.*

/**
 * Sección de reseñas para ProductDetailScreen.
 *
 * Uso desde ProductDetailScreen (dentro de LazyColumn):
 *
 *   item {
 *       ReviewsSection(
 *           reviews       = reviews,
 *           promedio      = reviewsPromedio,
 *           isLoading     = reviewsLoading,
 *           submitState   = reviewSubmitState,
 *           onSubmit      = { cal, com -> viewModel.submitReview(productId, cal, com, token) },
 *           onDismissMsg  = { viewModel.clearReviewMessages() }
 *       )
 *   }
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
    // ── Estado local del formulario ───────────────────────────────────────────
    var selectedStars by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }

    // Limpiar form al envío exitoso
    LaunchedEffect(submitState) {
        if (submitState is ReviewSubmitState.Success) {
            selectedStars = 0
            comentario = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header con promedio ───────────────────────────────────────────
            ReviewsHeader(promedio = promedio, total = reviews.size)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = GrayLight)
            Spacer(Modifier.height(16.dp))

            // ── Formulario nueva reseña ───────────────────────────────────────
            Text(
                text = "Dejá tu reseña",
                style = MaterialTheme.typography.titleSmall,
                color = GrayDark
            )
            Spacer(Modifier.height(10.dp))

            StarRatingInput(
                selected = selectedStars,
                onSelect = { selectedStars = it }
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = comentario,
                onValueChange = { if (it.length <= 300) comentario = it },
                placeholder = {
                    Text(
                        "¿Cómo fue tu experiencia con este producto?",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMid
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GrayLight,
                    cursorColor = GreenPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                supportingText = {
                    Text(
                        text = "${comentario.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMid
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            // Mensaje de feedback (éxito / error)
            AnimatedVisibility(
                visible = submitState is ReviewSubmitState.Error || submitState is ReviewSubmitState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val (msg, color) = when (submitState) {
                    is ReviewSubmitState.Error   -> submitState.message to MaterialTheme.colorScheme.error
                    is ReviewSubmitState.Success -> "¡Reseña enviada!" to GreenPrimary
                    else                         -> "" to Color.Transparent
                }
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Button(
                onClick = {
                    if (selectedStars > 0) {
                        onSubmit(selectedStars, comentario.trim())
                    }
                },
                enabled = selectedStars > 0 && submitState !is ReviewSubmitState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (submitState is ReviewSubmitState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar reseña", style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Lista de reseñas ──────────────────────────────────────────────
            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = GreenPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (reviews.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GrayLight)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Aún no hay reseñas. ¡Sé el primero!",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid
                )
            } else {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GrayLight)
                Spacer(Modifier.height(12.dp))

                reviews.forEach { review ->
                    ReviewItem(review = review)
                    if (review != reviews.last()) {
                        HorizontalDivider(
                            color = GrayLight,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun ReviewsHeader(promedio: Double?, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Reseñas",
            style = MaterialTheme.typography.titleSmall,
            color = GrayDark
        )

        if (promedio != null && total > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(promedio),
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "($total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMid
                )
            }
        }
    }
}

@Composable
private fun StarRatingInput(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= selected) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = "Estrella $i",
                tint = if (i <= selected) Color(0xFFFFC107) else GrayLight,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onSelect(i) }
            )
        }
        if (selected > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = when (selected) {
                    1 -> "Muy malo"
                    2 -> "Malo"
                    3 -> "Regular"
                    4 -> "Bueno"
                    5 -> "Excelente"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = GreenPrimary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Avatar inicial
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(GreenSurface),
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
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.nombreAgricultor ?: "Agricultor",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayDark,
                    fontWeight = FontWeight.SemiBold
                )
                // Fecha formateada (tomar solo YYYY-MM-DD del ISO)
                Text(
                    text = review.fechaCreacion?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = GrayMid
                )
            }

            Spacer(Modifier.height(3.dp))

            // Estrellas de solo lectura
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= review.calificacion) Icons.Default.Star
                        else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (i <= review.calificacion) Color(0xFFFFC107) else GrayLight,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            review.comentario?.takeIf { it.isNotBlank() }?.let { com ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = com,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayDark,
                    lineHeight = 18.sp
                )
            }
        }
    }
}