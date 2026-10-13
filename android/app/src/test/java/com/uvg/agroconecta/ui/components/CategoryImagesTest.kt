package com.uvg.agroconecta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryImagesTest {

    @Test
    fun `cada categoria del catalogo apunta a su foto en assets`() {
        assertEquals(
            "file:///android_asset/categorias/fertilizantes.jpg",
            CategoryImages.assetUriFor("Fertilizantes")
        )
        assertEquals(
            "file:///android_asset/categorias/herramientas.jpg",
            CategoryImages.assetUriFor("Herramientas")
        )
        assertEquals(
            "file:///android_asset/categorias/fungicidas.jpg",
            CategoryImages.assetUriFor("Fungicidas")
        )
    }

    @Test
    fun `ignora mayusculas tildes y espacios`() {
        assertEquals(
            "file:///android_asset/categorias/semillas.jpg",
            CategoryImages.assetUriFor("  SEMÍLLAS ")
        )
    }

    @Test
    fun `una categoria desconocida o vacia usa el icono de siempre`() {
        assertNull(CategoryImages.assetUriFor(null))
        assertNull(CategoryImages.assetUriFor(""))
        assertNull(CategoryImages.assetUriFor("Maquinaria"))
    }
}
