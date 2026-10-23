package com.uvg.agroconecta.data.diagnosis

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Cálculos para dejar una foto como la espera el modelo: un cuadrado RGB de
 * [INPUT_SIZE] píxeles, sin normalizar, porque la normalización de MobileNetV2
 * ya va dentro del modelo (ml/train_plantvillage.py). Están separados del
 * clasificador para poder probarlos sin Bitmap.
 */
internal object PlantImagePreprocessing {

    const val INPUT_SIZE = 224
    const val CHANNELS = 3

    data class Square(val left: Int, val top: Int, val side: Int)

    /**
     * Recorte central cuadrado. El modelo se entrenó con fotos cuadradas de una
     * sola hoja; estirar una foto 4:3 a cuadrado deformaría las manchas.
     */
    fun centerSquare(width: Int, height: Int): Square {
        require(width > 0 && height > 0) { "Tamaño de imagen inválido: ${width}x$height" }
        val side = minOf(width, height)
        return Square(left = (width - side) / 2, top = (height - side) / 2, side = side)
    }

    /**
     * Mayor potencia de 2 que deja el lado corto en al menos [INPUT_SIZE]. Leer la
     * foto ya reducida evita cargar en memoria millones de píxeles que el
     * modelo no va a usar.
     */
    fun sampleSizeFor(width: Int, height: Int): Int {
        var sampleSize = 1
        while (minOf(width, height) / (sampleSize * 2) >= INPUT_SIZE) sampleSize *= 2
        return sampleSize
    }

    /** Pasa píxeles ARGB (Bitmap.getPixels) a bytes RGB, que es la entrada uint8 del modelo. */
    fun toModelInput(argbPixels: IntArray): ByteBuffer {
        require(argbPixels.size == INPUT_SIZE * INPUT_SIZE) {
            "Se esperaban ${INPUT_SIZE * INPUT_SIZE} píxeles y llegaron ${argbPixels.size}"
        }
        val buffer = ByteBuffer.allocateDirect(argbPixels.size * CHANNELS).order(ByteOrder.nativeOrder())
        for (pixel in argbPixels) {
            buffer.put((pixel shr 16 and 0xFF).toByte())
            buffer.put((pixel shr 8 and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }
        buffer.rewind()
        return buffer
    }
}
