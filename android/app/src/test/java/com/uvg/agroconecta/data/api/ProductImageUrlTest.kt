package com.uvg.agroconecta.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductImageUrlTest {

    private val vmBaseUrl = "http://20.63.8.63:8080/api/"

    @Test
    fun `arma la URL con el host de la API y sin el prefijo api`() {
        assertEquals(
            "http://20.63.8.63:8080/uploads/productos/abc.png",
            ProductImageUrl.resolve("/uploads/productos/abc.png", vmBaseUrl)
        )
    }

    @Test
    fun `respeta el esquema de la URL base`() {
        // En release la URL base se fuerza a https; la imagen debe seguirla.
        assertEquals(
            "https://agro.example.com/uploads/productos/abc.webp",
            ProductImageUrl.resolve("/uploads/productos/abc.webp", "https://agro.example.com/api/")
        )
    }

    @Test
    fun `una ruta sin barra inicial tampoco queda colgada de api`() {
        assertEquals(
            "http://20.63.8.63:8080/uploads/productos/abc.jpg",
            ProductImageUrl.resolve("uploads/productos/abc.jpg", vmBaseUrl)
        )
    }

    @Test
    fun `sin imagen devuelve null para mostrar el icono por defecto`() {
        assertNull(ProductImageUrl.resolve(null, vmBaseUrl))
        assertNull(ProductImageUrl.resolve("", vmBaseUrl))
        assertNull(ProductImageUrl.resolve("   ", vmBaseUrl))
    }

    @Test
    fun `una URL absoluta se usa tal cual`() {
        assertEquals(
            "https://cdn.example.com/abc.png",
            ProductImageUrl.resolve("https://cdn.example.com/abc.png", vmBaseUrl)
        )
    }

    @Test
    fun `con una URL base invalida no inventa una direccion`() {
        assertNull(ProductImageUrl.resolve("/uploads/productos/abc.png", "no-es-una-url"))
    }
}
