package com.uvg.agroconecta.ui.distributor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.data.models.DistributorRatingResponse
import com.uvg.agroconecta.data.models.DistributorReview
import com.uvg.agroconecta.ui.components.DistributorRatingCard

@Composable
fun DistributorProfileScreen(
    distributorName: String,
    rating: DistributorRatingResponse?,
    reviews: List<DistributorReview>,
    isLoading: Boolean
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {

            Text(
                text = distributorName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {

                isLoading -> {

                    Text(
                        text = "Cargando información...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                rating != null && rating.totalResenas > 0 -> {

                    DistributorRatingCard(
                        rating = rating.calificacionPromedio,
                        totalReviews = rating.totalResenas
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RatingBar("5 estrellas", rating.distribucion.cinco, rating.totalResenas)
                    RatingBar("4 estrellas", rating.distribucion.cuatro, rating.totalResenas)
                    RatingBar("3 estrellas", rating.distribucion.tres, rating.totalResenas)
                    RatingBar("2 estrellas", rating.distribucion.dos, rating.totalResenas)
                    RatingBar("1 estrella", rating.distribucion.una, rating.totalResenas)
                }

                else -> {

                    Text(
                        text = "Aún no hay reseñas de este distribuidor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        if (reviews.isNotEmpty()) {

            item {

                Text(
                    text = "Reseñas recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(reviews) { review ->

                ReviewCard(review)
            }
        }
    }
}

@Composable
private fun RatingBar(
    label: String,
    value: Int,
    total: Int
) {

    val progress = if (total > 0) value.toFloat() / total.toFloat() else 0f

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(label)
            Text("$value")
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        )
    }
}

@Composable
private fun ReviewCard(review: DistributorReview) {

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = review.agricultorNombre ?: "Usuario",
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFF3CD)
                ) {

                    Text(
                        text = "⭐ ${review.calificacion}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.productoNombre ?: "Producto",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.comentario ?: "Sin comentario",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}