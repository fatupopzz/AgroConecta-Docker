package com.uvg.agroconecta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import java.text.Normalizer

/**
 * Foto de un producto con sus respaldos: si no tiene foto propia se usa la de
 * su categoría ([CategoryImages]); mientras Coil carga se ve un ícono tenue, y si
 * no hay ninguna de las dos (o falla) queda la hoja de siempre. Las tarjetas
 * pasan además [imagenThumbUrl]; ver [ProductImageSource].
 *
 * Los respaldos se dibujan encima del AsyncImage en vez de usar sus parámetros
 * placeholder/error porque esos heredan el ContentScale.Crop de la foto y el
 * ícono terminaría estirado a todo el recuadro.
 *
 * El fondo y la forma los pone quien la usa con [modifier], así cada pantalla
 * conserva sus colores por categoría o sus esquinas.
 */
@Composable
fun ProductImage(
    imagenUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    categoria: String? = null,
    iconSize: Dp = 40.dp,
    imagenThumbUrl: String? = null
) {
    val url = remember(imagenThumbUrl, imagenUrl, categoria) {
        ProductImageSource.pick(imagenThumbUrl, imagenUrl, categoria)
    }
    var painterState by remember(url) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val tint = MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onState = { painterState = it },
                modifier = Modifier.matchParentSize()
            )
        }

        when {
            url == null || painterState is AsyncImagePainter.State.Error -> Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
            painterState !is AsyncImagePainter.State.Success -> Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = tint.copy(alpha = 0.35f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Fondo suave detrás de la foto según la categoría. Ayuda a reconocer el tipo
 * de insumo de un vistazo, incluso antes de que cargue la imagen. Sale del
 * esquema de color para que funcione igual en modo oscuro.
 */
@Composable
fun categoryBackground(categoria: String?): Color {
    val key = categoria?.let {
        Normalizer.normalize(it.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
    }
    return when (key) {
        "fertilizantes", "semillas" -> MaterialTheme.colorScheme.primaryContainer
        "pesticidas", "herbicidas", "fungicidas" -> MaterialTheme.colorScheme.secondaryContainer
        "herramientas" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}
