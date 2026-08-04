package com.uvg.agroconecta.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateOrderRequestTest {

    private val gson = Gson()

    @Test
    fun urgentOrderSerializesApiFieldNames() {
        val request = CreateOrderRequest(
            idAgricultor = 2,
            idDistribuidor = 3,
            direccionEntrega = "Parcela norte",
            tipoEntrega = "domicilio",
            productos = listOf(OrderProduct(idInventario = 8, cantidad = 1)),
            esUrgente = true,
            tipoPlaga = "Pulgón"
        )

        val json = gson.toJsonTree(request).asJsonObject

        assertTrue(json.get("esUrgente").asBoolean)
        assertEquals("Pulgón", json.get("tipoPlaga").asString)
        assertEquals(3, json.get("id_distribuidor").asInt)
        assertFalse(json.has("es_urgente"))
    }

    @Test
    fun regularOrderKeepsBackwardsCompatibleDefaults() {
        val request = CreateOrderRequest(
            idAgricultor = 2,
            idDistribuidor = 3,
            direccionEntrega = "Parcela norte",
            tipoEntrega = "domicilio",
            productos = listOf(OrderProduct(idInventario = 8, cantidad = 1))
        )

        val json = gson.toJsonTree(request).asJsonObject

        assertFalse(json.get("esUrgente").asBoolean)
        assertFalse(json.has("tipoPlaga"))
    }
}
