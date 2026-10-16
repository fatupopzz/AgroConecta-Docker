package com.uvg.agroconecta.ui.home

import com.uvg.agroconecta.data.models.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFiltersTest {

    private val barato = product(1, price = 30.0, brand = "AgroMaya")
    private val caro = product(2, price = 250.0, brand = "InsumoGT")
    private val agotado = product(3, price = null, brand = "AgroMaya")
    private val todos = listOf(barato, caro, agotado)

    @Test
    fun `without filters returns the same list`() {
        assertEquals(todos, applyLocalFilters(todos, null, null, " "))
    }

    @Test
    fun `price range keeps only products inside it and drops products without price`() {
        assertEquals(listOf(barato), applyLocalFilters(todos, 10, 100, ""))
        assertEquals(listOf(caro), applyLocalFilters(todos, 100, null, ""))
    }

    @Test
    fun `brand filter ignores case and partial words`() {
        assertEquals(listOf(barato, agotado), applyLocalFilters(todos, null, null, "agromaya"))
    }

    @Test
    fun `describes active filters for removable chips`() {
        assertEquals(listOf("Q10 – Q100", "Marca: Maya"), activeFilterLabels(10, 100, " Maya "))
        assertEquals(listOf("Hasta Q50"), activeFilterLabels(null, 50, ""))
        assertEquals(emptyList<String>(), activeFilterLabels(null, null, ""))
    }

    @Test
    fun `greeting follows the time of day`() {
        assertEquals("Buenos días", greetingFor(7))
        assertEquals("Buenas tardes", greetingFor(15))
        assertEquals("Buenas noches", greetingFor(21))
        assertEquals("Buenas noches", greetingFor(2))
    }

    private fun product(id: Int, price: Double?, brand: String) = Product(
        id = id,
        nombre = "Producto $id",
        marca = brand,
        descripcion = null,
        composicion = null,
        dosis = null,
        instrucciones = null,
        calificacion = 0.0,
        categoria = "Fertilizantes",
        precioDesde = price,
        numDistribuidores = 1
    )
}
