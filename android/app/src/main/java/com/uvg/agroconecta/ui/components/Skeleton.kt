package com.uvg.agroconecta.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.uvg.agroconecta.ui.theme.AgroSpacing

/**
 * Bloque gris que late mientras llega el contenido real. Se usa en lugar de
 * spinners porque deja ver la forma de lo que viene y la espera se siente más
 * corta en conexiones lentas.
 *
 * El parpadeo cambia solo la opacidad y se lee dentro de drawBehind: así cada
 * cuadro solo vuelve a dibujar, sin recomponer, lo que importa en teléfonos
 * de gama baja con varias tarjetas en pantalla.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind { drawRect(color = color, alpha = alpha) }
    )
}

/** Silueta de [ProductCard]: foto, categoría, nombre y precio. */
@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    AgroCard(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
            shape = MaterialTheme.shapes.extraSmall
        )
        Column(
            modifier = Modifier.padding(AgroSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
        ) {
            SkeletonBox(Modifier.size(width = 72.dp, height = 18.dp))
            SkeletonBox(Modifier.fillMaxWidth().height(18.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(18.dp))
            Spacer(Modifier.height(AgroSpacing.xxs))
            SkeletonBox(Modifier.size(width = 96.dp, height = 26.dp))
        }
    }
}

/** Silueta de una fila de lista (pedido, alerta, producto del distribuidor). */
@Composable
fun ListItemSkeleton(modifier: Modifier = Modifier) {
    AgroCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AgroSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md)
        ) {
            SkeletonBox(Modifier.size(56.dp), shape = MaterialTheme.shapes.medium)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AgroSpacing.xs)
            ) {
                SkeletonBox(Modifier.fillMaxWidth(0.7f).height(20.dp))
                SkeletonBox(Modifier.fillMaxWidth(0.5f).height(16.dp))
                SkeletonBox(Modifier.fillMaxWidth(0.35f).height(16.dp))
            }
        }
    }
}

/**
 * Lista de siluetas que TalkBack anuncia una sola vez como "Cargando", en vez
 * de recorrer cada bloque vacío.
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 4,
    description: String = "Cargando",
    item: @Composable () -> Unit = { ListItemSkeleton() }
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        repeat(count) { item() }
    }
}

/** Cuadrícula de dos columnas de [ProductCardSkeleton], igual que el catálogo. */
@Composable
fun ProductGridSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 2
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "Cargando productos" },
        verticalArrangement = Arrangement.spacedBy(AgroSpacing.sm)
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm)) {
                ProductCardSkeleton(Modifier.weight(1f))
                ProductCardSkeleton(Modifier.weight(1f))
            }
        }
    }
}
