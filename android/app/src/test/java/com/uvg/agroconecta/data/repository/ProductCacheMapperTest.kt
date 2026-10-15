package com.uvg.agroconecta.data.repository

import com.uvg.agroconecta.data.local.ProductCacheEntity
import com.uvg.agroconecta.data.models.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductCacheMapperTest {

    @Test
    fun `maps API product to Room entity`() {
        val entity = apiProduct().toCacheEntity(timestamp = 9_000L)

        assertEquals(7, entity.id)
        assertEquals("Fertilizante orgánico", entity.nombre)
        assertEquals("Fertilizantes", entity.categoria)
        assertEquals(115.75, entity.precio ?: 0.0, 0.0)
        assertEquals("Mejora el suelo", entity.descripcion)
        assertEquals(9_000L, entity.timestamp)
        assertEquals("/uploads/productos/abono.jpg", entity.imagenUrl)
        assertEquals("/uploads/productos/thumbs/abono.webp", entity.imagenThumbUrl)
    }

    @Test
    fun `maps Room entity to product shown by UI`() {
        val product = ProductCacheEntity(
            id = 8,
            nombre = "Semilla certificada",
            categoria = "Semillas",
            precio = 63.25,
            descripcion = "Maíz blanco",
            timestamp = 10L,
            imagenUrl = "/uploads/productos/maiz.png",
            imagenThumbUrl = "/uploads/productos/thumbs/maiz.webp"
        ).toProduct()

        assertEquals(8, product.id)
        assertEquals("Semilla certificada", product.nombre)
        assertEquals("Semillas", product.categoria)
        assertEquals(63.25, product.precioDesde ?: 0.0, 0.0)
        assertEquals("Maíz blanco", product.descripcion)
        assertEquals(0.0, product.calificacion, 0.0)
        assertNull(product.marca)
        assertEquals("/uploads/productos/maiz.png", product.imagenUrl)
        assertEquals("/uploads/productos/thumbs/maiz.webp", product.imagenThumbUrl)
    }

    @Test
    fun `un producto sin foto se cachea sin imagen`() {
        val entity = apiProduct().copy(imagenUrl = null, imagenThumbUrl = null)
            .toCacheEntity(timestamp = 1L)

        assertNull(entity.imagenUrl)
        assertNull(entity.toProduct().imagenUrl)
        assertNull(entity.toProduct().imagenThumbUrl)
    }

    private fun apiProduct() = Product(
        id = 7,
        nombre = "Fertilizante orgánico",
        marca = "Agro",
        descripcion = "Mejora el suelo",
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 4.5,
        categoria = "Fertilizantes",
        precioDesde = 115.75,
        numDistribuidores = 2,
        activo = true,
        imagenUrl = "/uploads/productos/abono.jpg",
        imagenThumbUrl = "/uploads/productos/thumbs/abono.webp"
    )
}
