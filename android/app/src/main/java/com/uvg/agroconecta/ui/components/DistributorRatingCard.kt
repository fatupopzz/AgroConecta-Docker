package com.uvg.agroconecta.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DistributorRatingCard(
    rating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier
) {

    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val starScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starScale"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {

        Row(
            modifier = Modifier
                .background(Color(0xFFF7FAF7))
                .width(220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            repeat(5) { index ->

                val filled = index < rating.toInt()

                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.scale(starScale)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = String.format("%.1f", rating),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1B5E20)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "($totalReviews)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}