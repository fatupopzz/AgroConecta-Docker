package com.uvg.agroconecta.data.images

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Cálculos de tamaño separados del decodificado para poder probarlos sin
 * Bitmap ni Robolectric.
 */
internal object ImageScaling {

    // 1600 px de lado mayor alcanza para el detalle a pantalla completa en un
    // teléfono y deja una foto típica en unos cientos de KB.
    const val MAX_SIDE_PX = 1600

    /**
     * Potencia de 2 más grande que deja la imagen todavía por encima de
     * [maxSide]. BitmapFactory solo submuestrea en potencias de 2; el ajuste
     * fino se hace después con [targetSize]. Así no se carga en memoria una
     * foto de 50 MP completa solo para achicarla.
     */
    fun sampleSizeFor(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSide) {
            sample *= 2
        }
        return sample
    }

    /** Medidas finales conservando la proporción; no agranda fotos pequeñas. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }
}
