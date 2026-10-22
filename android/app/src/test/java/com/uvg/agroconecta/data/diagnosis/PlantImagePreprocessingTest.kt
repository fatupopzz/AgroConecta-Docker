package com.uvg.agroconecta.data.diagnosis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlantImagePreprocessingTest {

    @Test
    fun `recorta el cuadrado central de una foto horizontal y una vertical`() {
        assertEquals(
            PlantImagePreprocessing.Square(left = 200, top = 0, side = 1200),
            PlantImagePreprocessing.centerSquare(1600, 1200)
        )
        assertEquals(
            PlantImagePreprocessing.Square(left = 0, top = 200, side = 1200),
            PlantImagePreprocessing.centerSquare(1200, 1600)
        )
        assertEquals(
            PlantImagePreprocessing.Square(left = 0, top = 0, side = 224),
            PlantImagePreprocessing.centerSquare(224, 224)
        )
    }

    @Test
    fun `reduce al leer sin bajar el lado corto de 224`() {
        assertEquals(4, PlantImagePreprocessing.sampleSizeFor(1600, 1200))
        assertEquals(1, PlantImagePreprocessing.sampleSizeFor(400, 300))
        assertEquals(2, PlantImagePreprocessing.sampleSizeFor(448, 448))
        assertEquals(1, PlantImagePreprocessing.sampleSizeFor(100, 80))
    }

    @Test
    fun `pasa los píxeles a bytes RGB sin normalizar`() {
        val pixels = IntArray(224 * 224) { 0xFF112233.toInt() }
        pixels[1] = 0x80FF00C8.toInt()

        val buffer = PlantImagePreprocessing.toModelInput(pixels)

        assertEquals(224 * 224 * 3, buffer.capacity())
        assertEquals(0, buffer.position())
        val firstSix = ByteArray(6).also { buffer.get(it) }.map { it.toInt() and 0xFF }
        // El canal alfa se descarta: el modelo solo recibe rojo, verde y azul.
        assertEquals(listOf(0x11, 0x22, 0x33, 0xFF, 0x00, 0xC8), firstSix)
    }

    @Test
    fun `rechaza una cantidad de píxeles distinta a la del modelo`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlantImagePreprocessing.toModelInput(IntArray(10))
        }
    }
}
