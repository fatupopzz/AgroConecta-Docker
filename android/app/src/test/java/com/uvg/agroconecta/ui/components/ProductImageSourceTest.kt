package com.uvg.agroconecta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductImageSourceTest {

    private val baseUrl = "https://agro.example.com/api/"

    @Test
    fun `con miniatura las tarjetas piden la miniatura y no la foto grande`() {
        assertEquals(
            "https://agro.example.com/uploads/productos/thumbs/abc.webp",
            ProductImageSource.pick(
                imagenThumbUrl = "/uploads/productos/thumbs/abc.webp",
                imagenUrl = "/uploads/productos/abc.jpg",
                categoria = "Fertilizantes",
                apiBaseUrl = baseUrl
            )
        )
    }

    @Test
    fun `una foto sin miniatura usa la foto grande antes que la de la categoria`() {
        val expected = "https://agro.example.com/uploads/productos/abc.jpg"

        assertEquals(
            expected,
            ProductImageSource.pick(null, "/uploads/productos/abc.jpg", "Fertilizantes", baseUrl)
        )
        assertEquals(
            expected,
            ProductImageSource.pick("  ", "/uploads/productos/abc.jpg", "Fertilizantes", baseUrl)
        )
    }

    @Test
    fun `sin miniatura ni foto propia usa la foto de su categoria`() {
        assertEquals(
            "file:///android_asset/categorias/semillas.jpg",
            ProductImageSource.pick(null, null, "Semillas", baseUrl)
        )
    }

    @Test
    fun `sin ninguna imagen ni categoria conocida queda el icono de siempre`() {
        assertNull(ProductImageSource.pick(null, null, null, baseUrl))
        assertNull(ProductImageSource.pick(null, "", "Maquinaria", baseUrl))
    }
}
