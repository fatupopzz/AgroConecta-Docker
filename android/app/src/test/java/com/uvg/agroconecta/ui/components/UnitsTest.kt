package com.uvg.agroconecta.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {

    @Test
    fun `una unidad queda en singular`() {
        assertEquals("1 saco", formatQuantityWithUnit(1, "saco"))
    }

    @Test
    fun `varias unidades van en plural`() {
        assertEquals("40 sacos", formatQuantityWithUnit(40, "saco"))
        assertEquals("3 galones", formatQuantityWithUnit(3, "galón"))
        assertEquals("8 unidades", formatQuantityWithUnit(8, "unidad"))
        assertEquals("2 bolsas", formatQuantityWithUnit(2, "bolsas"))
    }

    @Test
    fun `las abreviaturas no cambian`() {
        assertEquals("25 kg", formatQuantityWithUnit(25, "kg"))
        assertEquals("4 L", formatQuantityWithUnit(4, "L"))
        assertEquals("10 qq", formatQuantityWithUnit(10, "qq"))
    }

    @Test
    fun `sin unidad se habla de unidades`() {
        assertEquals("1 unidad", formatQuantityWithUnit(1, null))
        assertEquals("5 unidades", formatQuantityWithUnit(5, " "))
    }
}
