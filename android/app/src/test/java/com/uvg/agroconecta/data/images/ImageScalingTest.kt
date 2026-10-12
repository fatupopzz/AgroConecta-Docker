package com.uvg.agroconecta.data.images

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageScalingTest {

    @Test
    fun `una foto de celular de 12 MP se reduce a 1600 px de lado mayor`() {
        assertEquals(1600 to 1200, ImageScaling.targetSize(4000, 3000))
    }

    @Test
    fun `respeta la proporcion de una foto vertical`() {
        assertEquals(900 to 1600, ImageScaling.targetSize(2268, 4032))
    }

    @Test
    fun `no agranda fotos que ya son pequenas`() {
        assertEquals(800 to 600, ImageScaling.targetSize(800, 600))
        assertEquals(1600 to 1600, ImageScaling.targetSize(1600, 1600))
    }

    @Test
    fun `el submuestreo deja la imagen todavia por encima del tamano final`() {
        // 4000 / 2 = 2000 >= 1600, pero 4000 / 4 = 1000 ya quedaría corta y
        // perdería nitidez al estirarla.
        assertEquals(2, ImageScaling.sampleSizeFor(4000, 3000))
        // Una de 50 MP (8160 px) baja a 2040 con 4.
        assertEquals(4, ImageScaling.sampleSizeFor(8160, 6120))
    }

    @Test
    fun `sin submuestreo cuando la foto ya es chica`() {
        assertEquals(1, ImageScaling.sampleSizeFor(1600, 1200))
        assertEquals(1, ImageScaling.sampleSizeFor(3199, 2000))
    }
}
